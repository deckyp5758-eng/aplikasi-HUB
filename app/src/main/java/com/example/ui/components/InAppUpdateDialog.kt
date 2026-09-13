package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.utils.ApkUpdateManager
import com.example.utils.UpdateUiState
import java.io.File

@Composable
fun InAppUpdateDialog(
    updateState: UpdateUiState,
    onUpdateClick: (apkUrl: String) -> Unit,
    onInstallClick: ((apkFile: File) -> Unit)? = null,
    onCancelDownload: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    when (updateState) {
        is UpdateUiState.UpdateAvailable -> {
            val info = updateState.info
            val forceUpdate = info.forceUpdate ?: false

            Dialog(
                onDismissRequest = {
                    if (!forceUpdate) onDismiss()
                },
                properties = DialogProperties(
                    dismissOnBackPress = !forceUpdate,
                    dismissOnClickOutside = !forceUpdate
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Update Available",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Pembaruan Tersedia",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Versi Terbaru: ${info.latestVersionName ?: "Rilis Terbaru"}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        val changelog = info.changelog
                        if (!changelog.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tombol Utama: Unduh Langsung via OkHttp Streaming
                        Button(
                            onClick = {
                                info.apkDownloadUrl?.let { url ->
                                    onUpdateClick(url)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UPDATE OTOMATIS", fontWeight = FontWeight.Bold)
                        }

                        // Tombol Cadangan: Buka di Browser jika driver kesulitan sinyal
                        info.apkDownloadUrl?.let { url ->
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { openBrowser(url) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unduh via Browser (Chrome)", fontSize = 13.sp)
                            }
                        }

                        if (!forceUpdate) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Nanti Saja")
                            }
                        }
                    }
                }
            }
        }

        is UpdateUiState.Downloading -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { updateState.progressPercent / 100f },
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.5.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Mengunduh Pembaruan...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Indikator teks persentase & ukuran file
                        val percent = updateState.progressPercent
                        val downloadedStr = ApkUpdateManager.formatBytes(updateState.downloadedBytes)
                        val totalStr = if (updateState.totalBytes > 0) {
                            ApkUpdateManager.formatBytes(updateState.totalBytes)
                        } else {
                            "Menghitung..."
                        }

                        Text(
                            text = "$percent%  ($downloadedStr / $totalStr)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            drawStopIndicator = {}
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Mengunduh langsung dengan streaming berkecepatan tinggi. Mohon tunggu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cadangan jika koneksi lambat
                            updateState.downloadUrl?.let { url ->
                                OutlinedButton(
                                    onClick = { openBrowser(url) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Buka Browser", fontSize = 12.sp)
                                }
                            }

                            TextButton(
                                onClick = {
                                    onCancelDownload?.invoke() ?: onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        is UpdateUiState.ReadyToInstall -> {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Ready",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Unduhan Selesai!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Berkas APK telah diverifikasi dan siap dipasang di perangkat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Catatan: Jika muncul pop-up izin 'Izinkan dari sumber ini', silakan aktifkan centang izin pada pengaturan HP.",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                onInstallClick?.invoke(updateState.apkFile)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("PASANG PEMBARUAN SEKARANG", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tutup")
                        }
                    }
                }
            }
        }

        is UpdateUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Pemberitahuan Pembaruan") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(updateState.message)
                        if (updateState.canOpenInBrowser && !updateState.browserUrl.isNullOrEmpty()) {
                            Text(
                                text = "Anda dapat mengunduh berkas langsung melalui peramban web (Chrome).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    if (updateState.canOpenInBrowser && !updateState.browserUrl.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                openBrowser(updateState.browserUrl)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh di Browser")
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Tutup")
                        }
                    }
                },
                dismissButton = {
                    if (updateState.canOpenInBrowser && !updateState.browserUrl.isNullOrEmpty()) {
                        TextButton(onClick = onDismiss) {
                            Text("Batal")
                        }
                    }
                }
            )
        }

        else -> {
            // Idle or Checking
        }
    }
}
