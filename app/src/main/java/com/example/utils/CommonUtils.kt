package com.example.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

object CommonUtils {

    /**
     * Converts a standard Google Drive sharing URL to a direct high-speed image stream URL.
     */
    fun getDirectDriveImageUrl(url: String): String {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return ""
        if (cleanUrl.startsWith("data:") || (!cleanUrl.contains("drive.google.com") && !cleanUrl.contains("google.com"))) return cleanUrl

        var fileId = ""
        val idMatch = Regex("[?&]id=([^&#]+)").find(cleanUrl)
        if (idMatch != null) {
            fileId = idMatch.groupValues[1]
        } else {
            val dMatch = Regex("/d/([^/]+)").find(cleanUrl)
            if (dMatch != null) {
                fileId = dMatch.groupValues[1]
            }
        }

        return if (fileId.isNotEmpty()) {
            "https://lh3.googleusercontent.com/d/$fileId"
        } else {
            cleanUrl
        }
    }

    /**
     * Safely downloads an image file to the user's Download directory.
     */
    fun downloadImage(context: Context, imageUrl: String) {
        try {
            if (imageUrl.isBlank()) {
                Toast.makeText(context, "URL Foto tidak valid.", Toast.LENGTH_SHORT).show()
                return
            }

            val directUrl = getDirectDriveImageUrl(imageUrl)
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                Toast.makeText(context, "Layanan unduhan tidak tersedia.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = Uri.parse(directUrl)
            val fileName = "HUB_KEDIRI_${System.currentTimeMillis()}.jpg"

            val request = DownloadManager.Request(uri).apply {
                setTitle("Mengunduh Foto Bukti")
                setDescription("Mengunduh foto: $fileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            downloadManager.enqueue(request)
            Toast.makeText(context, "Mulai mengunduh foto...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengunduh foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats integer odometer KM with thousand separators (e.g., 125000 -> 125.000).
     */
    fun formatKm(km: Int): String {
        return try {
            val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
            formatter.format(km)
        } catch (_: Exception) {
            km.toString()
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1 && result != null) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "nota_bbm_${System.currentTimeMillis()}"
    }

    fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (index >= 0) {
                        size = cursor.getLong(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        return size
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
