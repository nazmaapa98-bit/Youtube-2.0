package com.tonmoy.ytplayer

import android.Manifest
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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

    /**
     * Auto-enter PiP when user swipes home while a video is detected.
     * Only available on Android 8.0+ (API 26+).
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModel?.uiState?.value
        // Enter PiP if there's a video playing (in WebView) and not in audio mode
        if (state?.currentVideoInfo != null && !state.isAudioMode) {
            enterPipMode()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                // PiP might not be supported on all devices
            }
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        // Android 12+ (API 31): Enable auto-enter and seamless resize
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
        // In PiP mode, the Compose UI will automatically adjust.
        // The WebView continues to render the video in the small window.
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        val state = viewModel?.uiState?.value
        // Only resume WebView if NOT in audio mode
        // (in audio mode, WebView is intentionally paused to save battery)
        if (state?.isAudioMode != true) {
            viewModel?.webViewState?.resumeWebView()
        }
    }

    override fun onPause() {
        super.onPause()
        val state = viewModel?.uiState?.value
        // Don't pause WebView if entering PiP (video should keep playing)
        if (!isInPictureInPictureMode && state?.isAudioMode != true) {
            // Let WebView continue running; Android manages it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) { }
        viewModel = null
    }
}
