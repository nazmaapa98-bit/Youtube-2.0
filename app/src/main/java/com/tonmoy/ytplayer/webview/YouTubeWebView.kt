package com.tonmoy.ytplayer.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tonmoy.ytplayer.util.Constants

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView(
    modifier: Modifier = Modifier,
    webViewState: WebViewState,
    adBlockEngine: AdBlockEngine,
    onVideoDetected: (VideoInfo) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = Constants.CHROME_MOBILE_USER_AGENT
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                
                val currentWebView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(currentWebView, true)
                }
                
                addJavascriptInterface(
                    JavaScriptBridge(onVideoDetected),
                    Constants.JS_BRIDGE_NAME
                )
                
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?
                    ): Boolean {
                        val newWebView = WebView(context)
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        transport?.webView = newWebView
                        resultMsg?.sendToTarget()
                        return true
                    }
                    
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                    }
                }
                
                webViewClient = AdBlockWebViewClient(
                    adBlockEngine = adBlockEngine,
                    webViewState = webViewState,
                    onPageLoaded = {}
                )
                
                webViewState.webView = this
                loadUrl(Constants.YOUTUBE_MOBILE_URL)
            }
        },
        update = {
            // WebView manages its own state
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            webViewState.webView?.apply {
                removeJavascriptInterface(Constants.JS_BRIDGE_NAME)
                stopLoading()
                destroy()
            }
            webViewState.webView = null
        }
    }
}
