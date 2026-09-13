package com.example.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.ApiService
import com.example.data.AppUpdateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(
        val info: AppUpdateResponse
    ) : UpdateUiState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val downloadUrl: String? = null
    ) : UpdateUiState()
    data class ReadyToInstall(
        val apkFile: File,
        val apkUri: Uri,
        val downloadUrl: String? = null
    ) : UpdateUiState()
    data class Error(
        val message: String,
        val canOpenInBrowser: Boolean = false,
        val browserUrl: String? = null
    ) : UpdateUiState()
}

class ApkUpdateManager(
    private val context: Context,
    private val apiService: ApiService
) {
    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState

    private var downloadJob: Job? = null
    private val updateScope = CoroutineScope(Dispatchers.IO)

    // Dedicated OkHttpClient with auto-redirect for GitHub Release S3 CDN & long timeouts
    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkForUpdates(): AppUpdateResponse? = withContext(Dispatchers.IO) {
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

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }
    }

    /**
     * Mengunduh APK langsung menggunakan streaming OkHttp dan coroutines.
     * Mengatasi keterbatasan DownloadManager pada Android vendor tertentu,
     * otomatis mengikuti redirect AWS S3 GitHub, dan menampilkan progres real-time.
     */
    fun downloadAndInstallApk(apkUrl: String) {
        downloadJob?.cancel()

        downloadJob = updateScope.launch {
            val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            val destinationFile = File(destinationDir, "FleetOdoTracker_update.apk")
            val tempFile = File(destinationDir, "FleetOdoTracker_update.apk.tmp")

            try {
                if (tempFile.exists()) tempFile.delete()

                _updateState.value = UpdateUiState.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    downloadUrl = apkUrl
                )

                val request = Request.Builder()
                    .url(apkUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile) H033FleetTracker")
                    .header("Accept", "*/*")
                    .build()

                val response = downloadClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    _updateState.value = UpdateUiState.Error(
                        message = "Gagal mengunduh berkas dari server (Kode HTTP $code). Driver dapat mengunduh langsung via peramban.",
                        canOpenInBrowser = true,
                        browserUrl = apkUrl
                    )
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    _updateState.value = UpdateUiState.Error(
                        message = "Respon server tidak memiliki data berkas. Silakan coba lewat peramban.",
                        canOpenInBrowser = true,
                        browserUrl = apkUrl
                    )
                    return@launch
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val buffer = ByteArray(32 * 1024) // 32KB buffer untuk transfer cepat
                var lastProgressUpdate = 0L
                var lastPercent = -1

                body.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        while (isActive) {
                            val read = inputStream.read(buffer)
                            if (read == -1) break
                            outputStream.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            val currentPercent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            // Throttle update agar UI Compose tetap responsif tanpa lag
                            if (currentPercent != lastPercent && now - lastProgressUpdate >= 120) {
                                lastPercent = currentPercent
                                lastProgressUpdate = now
                                _updateState.value = UpdateUiState.Downloading(
                                    progressPercent = currentPercent,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    downloadUrl = apkUrl
                                )
                            }
                        }
                        outputStream.flush()
                    }
                }

                if (!isActive) {
                    if (tempFile.exists()) tempFile.delete()
                    _updateState.value = UpdateUiState.Idle
                    return@launch
                }

                // Ganti file sementara ke destinationFile secara atomik
                if (destinationFile.exists()) destinationFile.delete()
                if (!tempFile.renameTo(destinationFile)) {
                    tempFile.copyTo(destinationFile, overwrite = true)
                    tempFile.delete()
                }

                // 1. Verifikasi Integritas File APK (Anti-Korup)
                if (!destinationFile.exists() || destinationFile.length() < 1024 * 100) {
                    destinationFile.delete()
                    _updateState.value = UpdateUiState.Error(
                        message = "Ukuran file APK tidak sesuai atau unduhan terputus. Silakan ulangi atau gunakan link peramban.",
                        canOpenInBrowser = true,
                        browserUrl = apkUrl
                    )
                    return@launch
                }

                val pm = context.packageManager
                val archiveInfo = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageArchiveInfo(destinationFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        pm.getPackageArchiveInfo(destinationFile.absolutePath, 0)
                    }
                } catch (e: Exception) {
                    null
                }

                if (archiveInfo == null) {
                    Log.e("ApkUpdateManager", "PackageArchiveInfo is null, file is invalid APK")
                    destinationFile.delete()
                    _updateState.value = UpdateUiState.Error(
                        message = "Berkas APK hasil unduhan tidak dapat diurai (rusak). Silakan klik tombol di bawah untuk unduh langsung via Chrome.",
                        canOpenInBrowser = true,
                        browserUrl = apkUrl
                    )
                    return@launch
                }

                // File valid dan siap dipasang
                val authority = "${context.packageName}.fileprovider"
                val apkUri: Uri = FileProvider.getUriForFile(context, authority, destinationFile)

                _updateState.value = UpdateUiState.ReadyToInstall(
                    apkFile = destinationFile,
                    apkUri = apkUri,
                    downloadUrl = apkUrl
                )

                // Otomatis luncurkan installer
                withContext(Dispatchers.Main) {
                    installApk(destinationFile)
                }

            } catch (e: Exception) {
                if (isActive) {
                    Log.e("ApkUpdateManager", "Streaming download failed: ${e.message}", e)
                    if (tempFile.exists()) tempFile.delete()
                    _updateState.value = UpdateUiState.Error(
                        message = "Koneksi terputus saat mengunduh: ${e.localizedMessage ?: "Jaringan tidak stabil"}. Driver dapat mengunduh langsung via peramban.",
                        canOpenInBrowser = true,
                        browserUrl = apkUrl
                    )
                }
            }
        }
    }

    /**
     * Membuka paket installer Android dengan pengecekan izin Unknown Sources (Android 8+)
     */
    fun installApk(file: File) {
        try {
            if (!file.exists()) {
                _updateState.value = UpdateUiState.Error("File APK tidak ditemukan pada penyimpanan perangkat.")
                return
            }

            // Pengecekan Izin Sumber Tidak Dikenal di Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Aktifkan 'Izinkan dari sumber ini' pada menu pengaturan HP, lalu kembali pasang.",
                        Toast.LENGTH_LONG
                    ).show()

                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("ApkUpdateManager", "Install failed: ${e.message}", e)
            _updateState.value = UpdateUiState.Error("Gagal membuka penginstal aplikasi: ${e.localizedMessage}")
        }
    }

    fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ApkUpdateManager", "Failed to open browser: ${e.message}", e)
            Toast.makeText(context, "Tidak dapat membuka peramban: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _updateState.value = UpdateUiState.Idle
    }

    fun dismissUpdate() {
        downloadJob?.cancel()
        downloadJob = null
        _updateState.value = UpdateUiState.Idle
    }
}
