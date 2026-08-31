package com.tonmoy.ytplayer

import android.Manifest
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tonmoy.ytplayer.ui.MainScreen
import com.tonmoy.ytplayer.ui.MainViewModel
import com.tonmoy.ytplayer.ui.theme.YTPlayerTheme
import com.tonmoy.ytplayer.util.Constants
import com.tonmoy.ytplayer.webview.AdBlockEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the YT Player app.
 *
 * Responsibilities:
 * - Hosts Jetpack Compose UI content
 * - Native Back Button handling for YouTube SPA navigation (doesn't exit unexpectedly)
 * - Manages Picture-in-Picture (PiP) mode transitions
 * - Handles WebView lifecycle synchronization
 * - Requests runtime permissions (POST_NOTIFICATIONS on Android 13+)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adBlockEngine: AdBlockEngine

    private var viewModel: MainViewModel? = null

    // Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently for personal-use app */ }

    // PiP broadcast receiver for media controls
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.PipActions.ACTION_PLAY -> viewModel?.togglePlayPause()
                Constants.PipActions.ACTION_PAUSE -> viewModel?.togglePlayPause()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        requestNotificationPermission()

        // Register Back button handler for YouTube web navigation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val webView = viewModel?.webViewState?.webView
                if (webView != null && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Minimize app instead of killing when on home page
                    moveTaskToBack(true)
                }
            }
        })

        // Register PiP broadcast receiver
        val filter = IntentFilter().apply {
            addAction(Constants.PipActions.ACTION_PLAY)
            addAction(Constants.PipActions.ACTION_PAUSE)
        }
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            YTPlayerTheme {
                val vm: MainViewModel = hiltViewModel()
                viewModel = vm

                MainScreen(
                    viewModel = vm,
                    adBlockEngine = adBlockEngine
                )
            }
        }
    }

    // ── Notification Permission ─────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ── Picture-in-Picture ──────────────────────────────────────────────

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModel?.uiState?.value
        // Enter PiP if video is active and not already in audio background mode
        if (state?.currentVideoInfo != null && !state.isAudioMode) {
            enterPipMode()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams()
                enterPictureInPictureMode(params)
            } catch (_: Exception) { }
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel?.setIsInPipMode(isInPictureInPictureMode)
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        val state = viewModel?.uiState?.value
        if (state?.isAudioMode != true) {
            viewModel?.webViewState?.resumeWebView()
        }
    }

    override fun onPause() {
        super.onPause()
        // Do not kill WebView media playback on pause
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) { }
        viewModel = null
    }
}
