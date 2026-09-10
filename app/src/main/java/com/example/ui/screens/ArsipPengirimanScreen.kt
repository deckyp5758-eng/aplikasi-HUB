package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.graphics.Bitmap
import android.os.Environment
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import java.io.ByteArrayOutputStream
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.FleetViewModel
import com.example.utils.CommonUtils
import com.example.utils.ImageCompressor
import com.example.ui.components.InAppUpdateDialog

@Composable
fun PengirimanScreen(viewModel: FleetViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Fitur Jalur Pengiriman Telah Dihapus")
    }
}

@Composable
fun ArsipPengirimanScreen(viewModel: FleetViewModel, driverName: String) {
    val context = LocalContext.current
    var noDokumen by remember { mutableStateOf("") }
    var noReceive by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf(emptyList<Bitmap>()) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var validationErrorMsg by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photos = photos + bitmap
            validationErrorMsg = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            android.widget.Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            try {
                val contentResolver = context.contentResolver
                val bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                    android.graphics.BitmapFactory.decodeStream(inputStream)
                }
                if (bitmap != null) {
                    photos = photos + bitmap
                    validationErrorMsg = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text("Arsip Berhasil Disimpan", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Bukti pengiriman Anda telah berhasil diarsipkan ke sistem Google Sheets GID 1878433267.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                    }
                ) {
                    Text("Selesai")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Input Data Dokumen & Receive",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // NO DOKUMEN (Column C)
                    OutlinedTextField(
                        value = noDokumen,
                        onValueChange = { 
                            noDokumen = it
                            validationErrorMsg = null
                        },
                        label = { Text("No Dokumen (Kolom C)") },
                        placeholder = { Text("Contoh: 1598210") },
                        leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = "Dokumen Icon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("arsip_no_dokumen_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // NO RECEIVE (Column D)
                    OutlinedTextField(
                        value = noReceive,
                        onValueChange = { 
                            noReceive = it
                            validationErrorMsg = null
                        },
                        label = { Text("No Receive / Surat Jalan (Kolom D)") },
                        placeholder = { Text("Contoh: OD.9513951146") },
                        leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Receive Icon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("arsip_no_receive_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Lampiran Foto Bukti Pengiriman",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Bebas mengunggah berapa saja foto bukti kirim.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    try {
                                        cameraLauncher.launch(null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("arsip_camera_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ambil Foto")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("arsip_gallery_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Gallery")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Galeri")
                        }
                    }

                    if (photos.isNotEmpty()) {
                        Text(
                            text = "${photos.size} Foto Terpilih:",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(photos) { bitmap ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .clickable { photos = photos - bitmap },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        validationErrorMsg?.let { msg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (noDokumen.trim().isEmpty() && noReceive.trim().isEmpty()) {
                        validationErrorMsg = "⚠️ Mohon isi minimal salah satu antara No Dokumen atau No Receive!"
                        return@Button
                    }
                    if (photos.isEmpty()) {
                        validationErrorMsg = "⚠️ Mohon lampirkan minimal 1 foto bukti pengiriman!"
                        return@Button
                    }

                    isSubmitting = true
                    validationErrorMsg = null

                    val mediaFiles = mutableListOf<TerkirimMediaFile>()
                    val keyNo = noDokumen.ifEmpty { noReceive }.replace("/", "_").replace("\\", "_")
                    
                    photos.forEachIndexed { index, bitmap ->
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val bytes = stream.toByteArray()
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        mediaFiles.add(
                            TerkirimMediaFile(
                                base64 = b64,
                                fileName = "arsip_${keyNo}_foto_${index + 1}.jpg",
                                mimeType = "image/jpeg"
                            )
                        )
                    }

                    viewModel.submitArsipPengiriman(
                        noDokumen = noDokumen.trim(),
                        noReceive = noReceive.trim(),
                        driverName = driverName,
                        mediaFiles = mediaFiles,
                        onResult = { success, msg ->
                            isSubmitting = false
                            if (success) {
                                noDokumen = ""
                                noReceive = ""
                                photos = emptyList()
                                showSuccessDialog = true
                            } else {
                                validationErrorMsg = "❌ Gagal mengunggah arsip: $msg"
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_arsip_button"),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0054A6))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Menyimpan Arsip...", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Kirim Arsip Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KIRIM ARSIP BUKTI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

