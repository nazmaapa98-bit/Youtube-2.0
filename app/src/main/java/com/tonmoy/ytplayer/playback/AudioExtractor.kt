package com.tonmoy.ytplayer.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
 * Wraps NewPipe Extractor to fetch audio-only stream URLs.
 */
@Singleton
class AudioExtractor @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            NewPipe.init(OkHttpDownloader())
            initialized = true
        }
    }

    /**
     * Extracts the best audio stream for the given YouTube video URL.
     */
    suspend fun extractAudioStream(videoUrl: String): Result<AudioStreamInfo> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val extractor = ServiceList.YouTube.getStreamExtractor(videoUrl)
            extractor.fetchPage()
            
            val audioStreams = extractor.audioStreams
            if (audioStreams.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No audio streams found"))
            }

            // Prefer m4a/AAC formats, then sort by bitrate descending
            val bestStream = audioStreams
                .filter { it.content != null && it.content.isNotEmpty() }
                .sortedWith(compareByDescending<org.schabi.newpipe.extractor.stream.AudioStream> { 
                    it.format == org.schabi.newpipe.extractor.MediaFormat.M4A
                }.thenByDescending { it.bitrate })
                .firstOrNull()
                ?: return@withContext Result.failure(Exception("No valid audio streams available"))

            val title = try { extractor.name ?: "" } catch (_: Exception) { "" }
            val channelName = try { extractor.uploaderName ?: "" } catch (_: Exception) { "" }
            val thumbnailUrl = try {
                extractor.thumbnails?.firstOrNull()?.url ?: ""
            } catch (_: Exception) { "" }
            val duration = try { extractor.length } catch (_: Exception) { 0L }
            val formatName = try { bestStream.format?.name ?: "M4A" } catch (_: Exception) { "M4A" }

            val info = AudioStreamInfo(
                streamUrl = bestStream.content,
                title = title,
                channelName = channelName,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                bitrate = bestStream.bitrate,
                format = formatName
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts the best audio stream for the given YouTube video ID.
     */
    suspend fun extractAudioStreamByVideoId(videoId: String): Result<AudioStreamInfo> {
        return extractAudioStream("https://www.youtube.com/watch?v=$videoId")
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
