package com.tonmoy.ytplayer.webview

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import com.tonmoy.ytplayer.util.Constants
import java.net.URL
import android.util.Log

@Singleton
class AdBlockEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val adDomains = HashSet<String>()

    init {
        loadAdDomains()
    }

    private fun loadAdDomains() {
        try {
            val inputStream = context.assets.open(Constants.ADBLOCK_HOSTS_ASSET)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim()
                if (!trimmed.isNullOrEmpty() && !trimmed.startsWith("#")) {
                    adDomains.add(trimmed)
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("AdBlockEngine", "Error loading ad block hosts", e)
        }
    }

    fun isAdHost(host: String): Boolean {
        var currentHost = host
        while (currentHost.contains(".")) {
            if (adDomains.contains(currentHost)) {
                return true
            }
            val nextDotIndex = currentHost.indexOf('.')
            if (nextDotIndex != -1 && nextDotIndex + 1 < currentHost.length) {
                currentHost = currentHost.substring(nextDotIndex + 1)
            } else {
                break
            }
        }
        return false
    }

    fun isAdUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("/pagead/") ||
                lowerUrl.contains("/api/stats/ads") ||
                lowerUrl.contains("doubleclick.net") ||
                lowerUrl.contains("/ptracking") ||
                lowerUrl.contains("/ads/") ||
                lowerUrl.contains("get_video_info?") ||
                lowerUrl.contains("googleads")
    }

    fun shouldBlock(url: String): Boolean {
        if (isAdUrl(url)) return true
        return try {
            val parsedUrl = URL(url)
            isAdHost(parsedUrl.host)
        } catch (e: Exception) {
            false
        }
    }
}
