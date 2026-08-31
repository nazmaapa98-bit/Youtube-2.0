package com.tonmoy.ytplayer.webview

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tonmoy.ytplayer.util.Constants
import java.io.ByteArrayInputStream

class AdBlockWebViewClient(
    private val adBlockEngine: AdBlockEngine,
    private val webViewState: WebViewState,
    private val onPageLoaded: () -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        if (adBlockEngine.shouldBlock(url)) {
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }
        
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        
        webViewState.injectJsFromAsset(Constants.JsAssets.YOUTUBE_BRIDGE)
        webViewState.injectJsFromAsset(Constants.JsAssets.ADBLOCK_COSMETIC)
        webViewState.injectJsFromAsset(Constants.JsAssets.PLAYBACK_CONTROL)
        
        url?.let {
            webViewState.currentUrl.value = it
        }
        
        val cookies = CookieManager.getInstance().getCookie(url)
        if (cookies != null) {
            val isLoggedIn = cookies.contains("SID=") || cookies.contains("LOGIN_INFO=")
            webViewState.isLoggedIn.value = isLoggedIn
        }
        
        onPageLoaded()
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        // Return false to allow all YouTube navigation within WebView
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        webViewState.canGoBack.value = view?.canGoBack() ?: false
    }
}
