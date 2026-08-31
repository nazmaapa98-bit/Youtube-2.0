package com.tonmoy.ytplayer.ui

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.tonmoy.ytplayer.playback.AudioExtractor
import com.tonmoy.ytplayer.playback.PlaybackService
import com.tonmoy.ytplayer.webview.WebViewState
import com.tonmoy.ytplayer.webview.VideoInfo
import com.tonmoy.ytplayer.update.DownloadState
import com.tonmoy.ytplayer.update.UpdateChecker
import com.tonmoy.ytplayer.update.UpdateInfo
import com.tonmoy.ytplayer.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * UI state for the main screen.
 */
data class MainUiState(
    /** Whether audio-only background mode is currently active. */
    val isAudioMode: Boolean = false,
    /** Whether audio stream extraction is in progress. */
    val isLoading: Boolean = false,
    /** Whether audio is currently playing in the background service. */
    val isPlaying: Boolean = false,
    /** The currently detected video info from the WebView. */
    val currentVideoInfo: VideoInfo? = null,
    /** Error message to display, if any. */
    val errorMessage: String? = null,
    /** Title of the currently playing audio track. */
    val playingTitle: String = "",
    /** Channel name of the currently playing audio track. */
    val playingChannel: String = "",
    /** Current playback position in milliseconds. */
    val playbackPositionMs: Long = 0L,
    /** Total duration in milliseconds. */
    val durationMs: Long = 0L,
    /** Update info if an update is available */
    val availableUpdate: UpdateInfo? = null
)

/**
 * One-shot UI events (toasts, snackbars).
 */
sealed class MainUiEvent {
    data class ShowError(val message: String) : MainUiEvent()
    data class ShowMessage(val message: String) : MainUiEvent()
}

/**
 * ViewModel orchestrating the WebView ↔ Background Audio lifecycle and in-app updates.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val webViewState: WebViewState,
    private val audioExtractor: AudioExtractor,
    private val updateChecker: UpdateChecker,
    private val updateManager: UpdateManager
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val downloadState: StateFlow<DownloadState> = updateManager.downloadState

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events = _events.asSharedFlow()

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        connectToMediaService()
        checkForUpdates()
    }

    // ── In-App Updates ──────────────────────────────────────────────────

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val update = updateChecker.checkForUpdate(com.tonmoy.ytplayer.BuildConfig.VERSION_CODE)
                if (update != null) {
                    _uiState.update { it.copy(availableUpdate = update) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
            }
        }
    }

    fun startUpdateDownload() {
        val update = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            updateManager.downloadUpdate(update.downloadUrl)
        }
    }

    fun installUpdate(file: File) {
        updateManager.installApk(file)
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(availableUpdate = null) }
        updateManager.resetState()
    }

    // ── Video Detection from JS Bridge ──────────────────────────────────

    /**
     * Called when the JavaScript bridge detects a video in the WebView.
     */
    fun onVideoDetected(videoInfo: VideoInfo) {
        Log.d(TAG, "Video detected: ${videoInfo.videoId} - ${videoInfo.title}")
        _uiState.update { it.copy(currentVideoInfo = videoInfo) }
        webViewState.currentVideoInfo.value = videoInfo
    }

    // ── Audio Mode Toggle ───────────────────────────────────────────────

    /**
     * Toggles audio-only background mode.
     *
     * When activating:
     * 1. Pauses the WebView video
     * 2. Extracts audio-only stream URL via NewPipe Extractor
     * 3. Starts playback in the foreground service
     * 4. Pauses WebView rendering to save battery
     *
     * When deactivating:
     * 1. Stops background audio
     * 2. Resumes WebView rendering
     * 3. Optionally resumes WebView video playback
     */
    fun toggleAudioMode() {
        val current = _uiState.value
        if (current.isAudioMode) {
            stopAudioMode()
        } else {
            startAudioMode()
        }
    }

    private fun startAudioMode() {
        val videoInfo = _uiState.value.currentVideoInfo
        if (videoInfo == null) {
            viewModelScope.launch {
                _events.emit(MainUiEvent.ShowError("No video detected. Please open a video first."))
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Step 1: Pause the WebView video
        webViewState.pauseVideo()

        // Step 2: Extract audio stream and start playback
        viewModelScope.launch {
            val result = audioExtractor.extractAudioStreamByVideoId(videoInfo.videoId)

            result.onSuccess { streamInfo ->
                Log.d(TAG, "Audio stream extracted: ${streamInfo.format} @ ${streamInfo.bitrate}kbps")

                // Step 3: Build MediaItem and start playback
                val mediaItem = PlaybackService.buildMediaItem(
                    title = streamInfo.title,
                    channelName = streamInfo.channelName,
                    streamUrl = streamInfo.streamUrl,
                    thumbnailUrl = streamInfo.thumbnailUrl,
                    durationMs = streamInfo.duration * 1000L
                )

                mediaController?.let { controller ->
                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                }

                // Step 4: Pause WebView rendering to save battery
                webViewState.pauseWebView()

                _uiState.update {
                    it.copy(
                        isAudioMode = true,
                        isLoading = false,
                        isPlaying = true,
                        playingTitle = streamInfo.title,
                        playingChannel = streamInfo.channelName,
                        durationMs = streamInfo.duration * 1000L
                    )
                }

                _events.emit(
                    MainUiEvent.ShowMessage(
                        "🎵 Audio mode: Playing \"${streamInfo.title}\" (${streamInfo.format} @ ${streamInfo.bitrate}kbps)"
                    )
                )

            }.onFailure { error ->
                Log.e(TAG, "Failed to extract audio stream", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to extract audio: ${error.message}"
                    )
                }
                _events.emit(
                    MainUiEvent.ShowError("Audio extraction failed: ${error.localizedMessage}")
                )
                // Resume WebView since we failed
                webViewState.playVideo()
            }
        }
    }

    /**
     * Stops audio-only mode and returns to WebView.
     */
    fun stopAudioMode() {
        // Stop background audio
        mediaController?.let { controller ->
            controller.stop()
            controller.clearMediaItems()
        }

        // Resume WebView
        webViewState.resumeWebView()

        _uiState.update {
            it.copy(
                isAudioMode = false,
                isPlaying = false,
                isLoading = false,
                playingTitle = "",
                playingChannel = "",
                playbackPositionMs = 0L,
                durationMs = 0L
            )
        }
    }

    // ── Playback Controls ───────────────────────────────────────────────

    /** Toggle play/pause in the background service. */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    /** Seek forward by 10 seconds. */
    fun seekForward() {
        mediaController?.let { controller ->
            val newPos = (controller.currentPosition + 10_000L)
                .coerceAtMost(controller.duration)
            controller.seekTo(newPos)
        }
    }

    /** Seek backward by 10 seconds. */
    fun seekBackward() {
        mediaController?.let { controller ->
            val newPos = (controller.currentPosition - 10_000L).coerceAtLeast(0L)
            controller.seekTo(newPos)
        }
    }

    // ── MediaController Connection ──────────────────────────────────────

    private fun connectToMediaService() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java)
        )
        mediaControllerFuture = MediaController.Builder(appContext, sessionToken)
            .buildAsync()
            .also { future ->
                future.addListener(
                    {
                        try {
                            mediaController = future.get()
                            setupPlayerListener()
                            Log.d(TAG, "MediaController connected")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to connect MediaController", e)
                        }
                    },
                    MoreExecutors.directExecutor()
                )
            }
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                    Player.STATE_READY -> {
                        mediaController?.let { controller ->
                            _uiState.update {
                                it.copy(durationMs = controller.duration.coerceAtLeast(0L))
                            }
                        }
                    }
                    else -> { /* no-op */ }
                }
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                _uiState.update {
                    it.copy(
                        playingTitle = metadata.title?.toString() ?: it.playingTitle,
                        playingChannel = metadata.artist?.toString() ?: it.playingChannel
                    )
                }
            }
        })
    }

    // ── Cleanup ─────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
