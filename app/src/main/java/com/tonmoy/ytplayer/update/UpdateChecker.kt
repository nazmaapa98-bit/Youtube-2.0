package com.tonmoy.ytplayer.update

import com.tonmoy.ytplayer.BuildConfig
import com.tonmoy.ytplayer.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

@Singleton
class UpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/${Constants.GITHUB_OWNER}/${Constants.GITHUB_REPO}/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)

                val tagName = json.getString("tag_name")
                val versionName = if (tagName.startsWith("v", ignoreCase = true)) {
                    tagName.substring(1)
                } else {
                    tagName
                }

                val releaseNotes = json.optString("body", "")
                val publishedAt = json.optString("published_at", "")
                
                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (downloadUrl.isEmpty()) return@withContext null

                if (isNewerVersion(versionName, BuildConfig.VERSION_NAME)) {
                    return@withContext UpdateInfo(
                        versionName = versionName,
                        versionCode = -1,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes,
                        publishedAt = publishedAt
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split('.').mapNotNull { it.toIntOrNull() }
        val currentParts = current.split('.').mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
