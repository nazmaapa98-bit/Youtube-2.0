package com.tonmoy.ytplayer.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tonmoy.ytplayer.ui.theme.AudioModeGreen
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (downloadState !is DownloadState.Downloading) onDismiss() },
        icon = {
            Icon(imageVector = Icons.Filled.SystemUpdate, contentDescription = "Update Icon")
        },
        title = {
            Text(text = "Update Available!")
        },
        text = {
            Column {
                Text(text = "Version ${updateInfo.versionName} is ready")
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = updateInfo.releaseNotes,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                when (downloadState) {
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Downloading... ${downloadState.progress}%", style = MaterialTheme.typography.bodySmall)
                    }
                    is DownloadState.Downloaded -> {
                        Text(
                            text = "Download complete! Tap Install.",
                            color = AudioModeGreen,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is DownloadState.Error -> {
                        Text(
                            text = downloadState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.Idle -> {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = AudioModeGreen)
                    ) {
                        Text("Download")
                    }
                }
                is DownloadState.Downloading -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = AudioModeGreen.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Downloading...")
                    }
                }
                is DownloadState.Downloaded -> {
                    Button(
                        onClick = { onInstall(downloadState.file) },
                        colors = ButtonDefaults.buttonColors(containerColor = AudioModeGreen)
                    ) {
                        Text("Install")
                    }
                }
                is DownloadState.Error -> {
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = AudioModeGreen)
                    ) {
                        Text("Retry")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss, 
                enabled = downloadState !is DownloadState.Downloading
            ) {
                Text("Later")
            }
        }
    )
}
