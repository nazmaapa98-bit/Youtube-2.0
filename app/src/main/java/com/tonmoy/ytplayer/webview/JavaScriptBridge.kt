package com.tonmoy.ytplayer.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class JavaScriptBridge(
    private val onVideoDetected: (VideoInfo) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onVideoDetected(videoId: String, url: String, title: String, duration: Double) {
        handler.post {
            onVideoDetected(VideoInfo(videoId, url, title, duration))
        }
    }

    @JavascriptInterface
    fun onPlaybackStateChanged(isPlaying: Boolean) {
        handler.post {
            // Optional: handle playback state change
        }
    }

    @JavascriptInterface
    fun onCurrentTime(time: Double) {
        handler.post {
            // Optional: handle current time update
        }
    }
}
