package com.tonmoy.ytplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tonmoy.ytplayer.ui.theme.AudioModeGreen
import com.tonmoy.ytplayer.ui.theme.YTRed
import com.tonmoy.ytplayer.webview.AdBlockEngine
import com.tonmoy.ytplayer.webview.YouTubeWebView

/**
 * Root composable for the main screen.
 *
 * Automatically adapts without unmounting the WebView:
 * - PiP Mode: Full screen video without floating controls
 * - Normal Mode: Full WebView + Magic Button + Refresh Button + Mini Player
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    adBlockEngine: AdBlockEngine
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-shot events (snackbar messages)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainUiEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is MainUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            if (!uiState.isPipMode) {
                SnackbarHost(snackbarHostState)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(if (!uiState.isPipMode) Modifier.padding(paddingValues) else Modifier)
        ) {
            // ── Layer 1: Persistent YouTube WebView (NEVER RECREATED) ──
            YouTubeWebView(
                modifier = Modifier.fillMaxSize(),
                webViewState = viewModel.webViewState,
                adBlockEngine = adBlockEngine,
                onVideoDetected = { videoInfo ->
                    viewModel.onVideoDetected(videoInfo)
                }
            )

            // ── Layer 2: Mini Audio Player Bar (Hidden in PiP) ───────
            AnimatedVisibility(
                visible = uiState.isAudioMode && !uiState.isPipMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                MiniAudioPlayer(
                    title = uiState.playingTitle,
                    channel = uiState.playingChannel,
                    isPlaying = uiState.isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onSeekBack = { viewModel.seekBackward() },
                    onSeekForward = { viewModel.seekForward() },
                    onClose = { viewModel.stopAudioMode() }
                )
            }

            // ── Layer 3: Controls (Refresh + Magic Button, Hidden in PiP)
            AnimatedVisibility(
                visible = !uiState.isPipMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (uiState.isAudioMode) 88.dp else 24.dp
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Refresh Button
                    SmallFloatingActionButton(
                        onClick = { viewModel.reloadWebView() },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh YouTube page",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Magic Audio Button (FAB)
                    MagicFab(
                        isAudioMode = uiState.isAudioMode,
                        isLoading = uiState.isLoading,
                        hasVideo = uiState.currentVideoInfo != null,
                        onClick = { viewModel.toggleAudioMode() }
                    )
                }
            }

            // ── Layer 4: Loading Overlay (Hidden in PiP) ────────────
            if (uiState.isLoading && !uiState.isPipMode) {
                LoadingOverlay()
            }

            // ── Layer 5: In-App Update Dialog (Hidden in PiP) ───────
            if (!uiState.isPipMode) {
                uiState.availableUpdate?.let { updateInfo ->
                    val downloadState by viewModel.downloadState.collectAsState()
                    com.tonmoy.ytplayer.update.UpdateDialog(
                        updateInfo = updateInfo,
                        downloadState = downloadState,
                        onDownload = { viewModel.startUpdateDownload() },
                        onInstall = { file -> viewModel.installUpdate(file) },
                        onDismiss = { viewModel.dismissUpdateDialog() }
                    )
                }
            }
        }
    }
}

// ── Magic FAB ───────────────────────────────────────────────────────────

@Composable
private fun MagicFab(
    isAudioMode: Boolean,
    isLoading: Boolean,
    hasVideo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isAudioMode -> AudioModeGreen
            hasVideo -> YTRed
            else -> Color.Gray
        },
        animationSpec = tween(300),
        label = "fab_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAudioMode) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier
            .scale(pulseScale)
            .shadow(8.dp, CircleShape)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isAudioMode) Icons.Filled.VolumeUp else Icons.Filled.MusicNote,
                contentDescription = if (isAudioMode) "Stop Audio Mode" else "Start Audio Mode",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── Mini Audio Player ───────────────────────────────────────────────────

@Composable
private fun MiniAudioPlayer(
    title: String,
    channel: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AudioModeGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title.ifEmpty { "Loading..." },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (channel.isNotEmpty()) {
                Text(
                    text = channel,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onSeekBack, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Replay10,
                contentDescription = "Rewind 10s",
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(28.dp)
            )
        }

        IconButton(onClick = onSeekForward, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Forward10,
                contentDescription = "Forward 10s",
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close Audio Mode",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Loading Overlay ─────────────────────────────────────────────────────

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AudioModeGreen,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Extracting audio stream...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saving your data 📡",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}
