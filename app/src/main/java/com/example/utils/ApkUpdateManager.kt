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
        // Debug APK diizinkan memeriksa update untuk pengujian distribusi H033.
        // URL release tetap harus berasal dari GitHub Release publik dan signing key harus kompatibel.
        try {
            _updateState.value = UpdateUiState.Checking

            // 1. Cek pembaruan langsung dari GitHub Releases API
            val githubUpdate = checkGitHubRelease()
            if (githubUpdate != null) {
                _updateState.value = UpdateUiState.UpdateAvailable(githubUpdate)
                return@withContext githubUpdate
            }

            // 2. Fallback cek dari backend Google Apps Script jika tersedia
            val response = try {
                apiService.checkUpdate()
            } catch (e: Exception) {
                null
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            if (response != null && response.success && response.latestVersionCode != null && response.latestVersionCode > currentVersionCode) {
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

    private suspend fun checkGitHubRelease(): AppUpdateResponse? {
        return try {
            val ghRelease = apiService.getLatestGitHubRelease()
            val tagName = ghRelease.tag_name ?: return null
            val cleanRemoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val currentVersionName = BuildConfig.VERSION_NAME

            val isNewer = isNewerSemanticVersion(cleanRemoteVersion, currentVersionName)
            if (!isNewer) {
                Log.d("ApkUpdateManager", "GitHub release $cleanRemoteVersion is not newer than current $currentVersionName")
                return null
            }

            // Cari asset APK
            val apkAsset = ghRelease.assets?.firstOrNull { asset ->
                asset.name?.endsWith(".apk", ignoreCase = true) == true ||
                asset.browser_download_url?.endsWith(".apk", ignoreCase = true) == true
            }

            val downloadUrl = apkAsset?.browser_download_url ?: return null
            val changelog = ghRelease.body ?: ghRelease.name ?: "Pembaruan versi $cleanRemoteVersion"

            AppUpdateResponse(
                success = true,
                latestVersionCode = extractVersionCode(cleanRemoteVersion),
                latestVersionName = cleanRemoteVersion,
                apkDownloadUrl = downloadUrl,
                forceUpdate = false,
                changelog = changelog
            )
        } catch (e: Exception) {
            Log.w("ApkUpdateManager", "GitHub release check failed: ${e.message}")
            null
        }
    }

    companion object {
        fun isNewerSemanticVersion(remoteVersion: String, currentVersion: String): Boolean {
            val cleanRemote = remoteVersion.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V").trim()

            val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        fun extractVersionCode(versionName: String): Int {
            val parts = versionName.removePrefix("v").removePrefix("V").split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return (major * 10000) + (minor * 100) + patch
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
