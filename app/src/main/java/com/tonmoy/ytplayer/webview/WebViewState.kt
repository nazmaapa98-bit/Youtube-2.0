package com.tonmoy.ytplayer.webview

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import com.tonmoy.ytplayer.util.Constants
import java.io.BufferedReader
import java.io.InputStreamReader

data class VideoInfo(
    val videoId: String,
    val url: String,
    val title: String,
    val duration: Double
)

class WebViewState {
    var webView: WebView? = null
    
    val currentUrl = MutableStateFlow(Constants.YOUTUBE_MOBILE_URL)
    val currentVideoInfo = MutableStateFlow<VideoInfo?>(null)
    val isLoggedIn = MutableStateFlow(false)
    val canGoBack = MutableStateFlow(false)

    fun goBack() {
        webView?.goBack()
    }

    fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    fun pauseWebView() {
        webView?.onPause()
        webView?.pauseTimers()
    }

    fun resumeWebView() {
        webView?.onResume()
        webView?.resumeTimers()
    }

    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null) {
        webView?.evaluateJavascript(script, callback)
    }

    fun injectJsFromAsset(assetPath: String) {
        val context = webView?.context ?: return
        try {
            val inputStream = context.assets.open(assetPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            reader.close()
            evaluateJavascript(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseVideo() {
        evaluateJavascript("if(typeof ytPauseVideo === 'function') { ytPauseVideo(); }")
    }

    fun playVideo() {
        evaluateJavascript("if(typeof ytPlayVideo === 'function') { ytPlayVideo(); }")
    }
}
