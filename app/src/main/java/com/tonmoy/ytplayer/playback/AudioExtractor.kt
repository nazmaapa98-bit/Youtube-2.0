package com.tonmoy.ytplayer.playback

import android.util.Log
import android.webkit.CookieManager
import com.tonmoy.ytplayer.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Information about an extracted audio stream.
 */
data class AudioStreamInfo(
    val streamUrl: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val duration: Long,
    val bitrate: Int,
    val format: String
)

/**
 * Hybrid Audio Extractor combining YouTube Innertube API and NewPipe Extractor.
 * Primary: Native Innertube Android & iOS player API (100% immune to ytInitialData web HTML breakage).
 * Secondary: NewPipe Extractor fallback.
 */
@Singleton
class AudioExtractor @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "AudioExtractor"
    private var newPipeInitialized = false

    private fun ensureNewPipeInitialized() {
        if (!newPipeInitialized) {
            try {
                NewPipe.init(OkHttpDownloader())
                newPipeInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "NewPipe init failed", e)
            }
        }
    }

    /**
     * Extracts the best audio stream for the given YouTube video ID.
     */
    suspend fun extractAudioStreamByVideoId(videoId: String): Result<AudioStreamInfo> = withContext(Dispatchers.IO) {
        // Step 1: Try Innertube Android Client (Fastest, direct JSON, no HTML parsing)
        extractViaInnertube(videoId, clientName = "ANDROID", clientVersion = "19.09.37").onSuccess {
            return@withContext Result.success(it)
        }

        // Step 2: Try Innertube iOS Client
        extractViaInnertube(videoId, clientName = "IOS", clientVersion = "19.09.3").onSuccess {
            return@withContext Result.success(it)
        }

        // Step 3: Try Innertube TV / Web Embedded Client
        extractViaInnertube(videoId, clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER", clientVersion = "2.0").onSuccess {
            return@withContext Result.success(it)
        }

        // Step 4: Fallback to NewPipe Extractor
        extractViaNewPipe("https://www.youtube.com/watch?v=$videoId").onSuccess {
            return@withContext Result.success(it)
        }

        Result.failure(Exception("Unable to extract audio stream for video: $videoId"))
    }

    /**
     * Extracts audio directly from YouTube's Innertube API without web scraping.
     */
    private fun extractViaInnertube(videoId: String, clientName: String, clientVersion: String): Result<AudioStreamInfo> {
        return try {
            val jsonPayload = JSONObject().apply {
                val context = JSONObject().apply {
                    val client = JSONObject().apply {
                        put("clientName", clientName)
                        put("clientVersion", clientVersion)
                        put("hl", "en")
                        put("gl", "US")
                    }
                    put("client", client)
                }
                put("context", context)
                put("videoId", videoId)
            }

            val requestBuilder = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", Constants.CHROME_MOBILE_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/")

            try {
                val cookies = CookieManager.getInstance().getCookie("https://www.youtube.com")
                if (!cookies.isNullOrEmpty()) {
                    requestBuilder.header("Cookie", cookies)
                }
            } catch (_: Exception) { }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Innertube HTTP ${response.code}"))
            }

            val responseBody = response.body?.string() ?: return Result.failure(Exception("Empty body"))
            val json = JSONObject(responseBody)

            val videoDetails = json.optJSONObject("videoDetails")
            val title = videoDetails?.optString("title", "") ?: ""
            val author = videoDetails?.optString("author", "") ?: ""
            val lengthSeconds = videoDetails?.optLong("lengthSeconds", 0L) ?: 0L

            val streamingData = json.optJSONObject("streamingData")
                ?: return Result.failure(Exception("No streamingData in response"))

            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            val formats = streamingData.optJSONArray("formats")

            var bestUrl: String? = null
            var bestBitrate = 0
            var bestFormat = "M4A"

            // Look for audio streams in adaptiveFormats
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val fmt = adaptiveFormats.getJSONObject(i)
                    val mimeType = fmt.optString("mimeType", "")
                    val url = fmt.optString("url", "")

                    if (mimeType.startsWith("audio/") && url.isNotEmpty()) {
                        val bitrate = fmt.optInt("bitrate", 0)
                        if (bitrate > bestBitrate) {
                            bestBitrate = bitrate
                            bestUrl = url
                            bestFormat = if (mimeType.contains("mp4") || mimeType.contains("m4a")) "M4A" else "OPUS"
                        }
                    }
                }
            }

            // If none in adaptiveFormats, check formats array
            if (bestUrl == null && formats != null) {
                for (i in 0 until formats.length()) {
                    val fmt = formats.getJSONObject(i)
                    val url = fmt.optString("url", "")
                    if (url.isNotEmpty()) {
                        bestUrl = url
                        bestBitrate = fmt.optInt("bitrate", 0)
                        bestFormat = "MP4"
                        break
                    }
                }
            }

            val finalUrl = bestUrl ?: return Result.failure(Exception("No direct audio URL found in formats"))

            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            Result.success(
                AudioStreamInfo(
                    streamUrl = finalUrl,
                    title = title,
                    channelName = author,
                    thumbnailUrl = thumbnailUrl,
                    duration = lengthSeconds,
                    bitrate = bestBitrate / 1000,
                    format = bestFormat
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Innertube extraction error for client $clientName", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts audio stream using NewPipe Extractor as fallback.
     */
    private fun extractViaNewPipe(videoUrl: String): Result<AudioStreamInfo> {
        return try {
            ensureNewPipeInitialized()
            val extractor = ServiceList.YouTube.getStreamExtractor(videoUrl)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                return Result.failure(Exception("No audio streams found"))
            }

            val bestStream = audioStreams
                .filter { !it.content.isNullOrEmpty() }
                .sortedWith(compareByDescending<org.schabi.newpipe.extractor.stream.AudioStream> {
                    it.format == org.schabi.newpipe.extractor.MediaFormat.M4A
                }.thenByDescending { it.bitrate })
                .firstOrNull()
                ?: return Result.failure(Exception("No valid audio streams available"))

            Result.success(
                AudioStreamInfo(
                    streamUrl = bestStream.content,
                    title = extractor.name ?: "",
                    channelName = extractor.uploaderName ?: "",
                    thumbnailUrl = extractor.thumbnails?.firstOrNull()?.url ?: "",
                    duration = extractor.length,
                    bitrate = bestStream.bitrate,
                    format = bestStream.format?.name ?: "M4A"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private inner class OkHttpDownloader : Downloader() {
        override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
            val httpMethod = request.httpMethod()
            val url = request.url()
            val headers = request.headers()
            val dataToSend = request.dataToSend()

            val requestBuilder = Request.Builder()
                .url(url)
                .method(
                    httpMethod,
                    dataToSend?.toRequestBody(null)
                )

            requestBuilder.header("User-Agent", Constants.CHROME_MOBILE_USER_AGENT)
            requestBuilder.header("Accept-Language", "en-US,en;q=0.9")

            try {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    requestBuilder.header("Cookie", cookies)
                }
            } catch (_: Exception) { }

            headers.forEach { (key, values) ->
                values.forEach { value -> requestBuilder.addHeader(key, value) }
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            val responseHeaders = response.headers.toMultimap()

            return Response(
                response.code,
                response.message,
                responseHeaders,
                responseBody,
                response.request.url.toString()
            )
        }
    }
}
