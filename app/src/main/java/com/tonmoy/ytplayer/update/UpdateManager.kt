package com.tonmoy.ytplayer.update

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Downloaded(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    suspend fun downloadUpdate(downloadUrl: String) = withContext(Dispatchers.IO) {
        downloadState.value = DownloadState.Downloading(0)
        try {
            val request = Request.Builder().url(downloadUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    downloadState.value = DownloadState.Error("Failed to download update")
                    return@withContext
                }

                val body = response.body ?: run {
                    downloadState.value = DownloadState.Error("Empty response body")
                    return@withContext
                }

                val contentLength = body.contentLength()
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "YTPlayer-update.apk")
                
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesCopied: Long = 0
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            if (contentLength > 0) {
                                val progress = ((bytesCopied * 100) / contentLength).toInt()
                                downloadState.value = DownloadState.Downloading(progress)
                            }
                        }
                    }
                }
                downloadState.value = DownloadState.Downloaded(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
        }
    }

    fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            downloadState.value = DownloadState.Error("Failed to install APK: ${e.message}")
        }
    }

    fun resetState() {
        downloadState.value = DownloadState.Idle
    }
}
