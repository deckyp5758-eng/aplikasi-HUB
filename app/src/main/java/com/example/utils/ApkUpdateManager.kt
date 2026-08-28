package com.example.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.ApiService
import com.example.data.AppUpdateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(
        val info: AppUpdateResponse
    ) : UpdateUiState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateUiState()
    data class ReadyToInstall(
        val apkUri: Uri
    ) : UpdateUiState()
    data class Error(
        val message: String
    ) : UpdateUiState()
}

class ApkUpdateManager(
    private val context: Context,
    private val apiService: ApiService
) {
    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState

    private var downloadId: Long = -1L

    suspend fun checkForUpdates(): AppUpdateResponse? = withContext(Dispatchers.IO) {
        // Debug APK tidak boleh memaksa unduhan dari URL release produksi.
        if (BuildConfig.DEBUG) {
            _updateState.value = UpdateUiState.Idle
            return@withContext null
        }
        try {
            _updateState.value = UpdateUiState.Checking
            val response = apiService.checkUpdate()
            val currentVersionCode = BuildConfig.VERSION_CODE

            Log.d("ApkUpdateManager", "Server VersionCode: ${response.latestVersionCode}, Current: $currentVersionCode")

            if (response.success && response.latestVersionCode != null && response.latestVersionCode > currentVersionCode) {
                _updateState.value = UpdateUiState.UpdateAvailable(response)
                response
            } else {
                _updateState.value = UpdateUiState.Idle
                null
            }
        } catch (e: Exception) {
            Log.e("ApkUpdateManager", "Check update failed: ${e.message}", e)
            _updateState.value = UpdateUiState.Idle
            null
        }
    }

    fun downloadAndInstallApk(apkUrl: String) {
        try {
            val destinationFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "FleetOdoTracker_update.apk"
            )
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("Mengunduh Pembaruan Fleet Tracker")
                setDescription("Mengunduh versi terbaru aplikasi...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            _updateState.value = UpdateUiState.Downloading(0, 0, 0)

            // Register receiver for download completion
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e("ApkUpdateManager", "Receiver unregister error: ${e.message}")
                        }
                        installApk(destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            Log.e("ApkUpdateManager", "Download failed: ${e.message}", e)
            _updateState.value = UpdateUiState.Error("Gagal memulai unduhan: ${e.localizedMessage}")
        }
    }

    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateUiState.Error("File APK tidak ditemukan")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

            _updateState.value = UpdateUiState.ReadyToInstall(apkUri)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("ApkUpdateManager", "Install failed: ${e.message}", e)
            _updateState.value = UpdateUiState.Error("Gagal memasang aplikasi: ${e.localizedMessage}")
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateUiState.Idle
    }
}
