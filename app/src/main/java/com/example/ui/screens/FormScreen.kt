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
fun FormScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    val selectedArmadaId by viewModel.selectedArmadaId.collectAsStateWithLifecycle()
    val kmInput by viewModel.kmInput.collectAsStateWithLifecycle()
    val catatanInput by viewModel.catatanInput.collectAsStateWithLifecycle()
    val selectedPhoto by viewModel.selectedPhoto.collectAsStateWithLifecycle()
    val notaBbmState by viewModel.notaBbmState.collectAsStateWithLifecycle()
    val notaBbmStatus by viewModel.notaBbmStatus.collectAsStateWithLifecycle()
    val isLoading by viewModel.submitLoading.collectAsStateWithLifecycle()
    val error by viewModel.submitError.collectAsStateWithLifecycle()
    val ocrLoading by viewModel.ocrLoading.collectAsStateWithLifecycle()
    val ocrSuccessMessage by viewModel.ocrSuccessMessage.collectAsStateWithLifecycle()
    val allLogs by viewModel.logs.collectAsStateWithLifecycle()
    var formFullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var formShowAllLogs by remember { mutableStateOf(false) }

    LaunchedEffect(ocrSuccessMessage) {
        ocrSuccessMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearOcrMessage()
        }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    // Camera and gallery launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setPhoto(bitmap)
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
            android.widget.Toast.makeText(
                context,
                "Izin kamera diperlukan untuk mengambil foto.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = ImageCompressor.decodeSampledBitmapFromUri(context, uri, maxDimension = 1280)
                if (bitmap != null) {
                    viewModel.setPhoto(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Nota BBM Launchers (Camera & Document/Image Picker)
    val notaCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val fileName = "NOTA_BBM_${System.currentTimeMillis()}.jpg"
            viewModel.setNotaBbmImage(
                uri = null,
                bitmap = bitmap,
                fileName = fileName,
                mimeType = "image/jpeg",
                size = 0L
            )
        }
    }

    val notaCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                notaCameraLauncher.launch(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Izin kamera diperlukan untuk memotret nota BBM.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val notaFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = getFileNameFromUri(context, uri)
                val fileSize = getFileSizeFromUri(context, uri)
                if (fileSize > 8 * 1024 * 1024) {
                    android.widget.Toast.makeText(context, "Ukuran file melebihi 8 MB! Silakan pilih berkas lebih kecil.", android.widget.Toast.LENGTH_LONG).show()
                    return@rememberLauncherForActivityResult
                }
                val mimeType = context.contentResolver.getType(uri) ?: if (fileName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
                if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        viewModel.setNotaBbmPdf(uri, bytes, fileName, if (fileSize > 0) fileSize else bytes.size.toLong())
                    }
                } else {
                    val bitmap = ImageCompressor.decodeSampledBitmapFromUri(context, uri, maxDimension = 1280)
                    if (bitmap != null) {
                        viewModel.setNotaBbmImage(uri, bitmap, fileName, mimeType, fileSize)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Gagal membuka berkas nota: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Pencatatan KM Harian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Fleet Dropdown Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val matching = armadaList.find { it.armadaId == selectedArmadaId }
                        val displayText = if (matching != null) {
                            "${matching.armadaId} - ${matching.noPolisi} (KM: ${matching.kmSaatIni})"
                        } else {
                            "Pilih Armada..."
                        }

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Armada") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Car Icon") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                                .testTag("armada_dropdown_select"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (armadaList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Memuat data armada...") },
                                    onClick = { dropdownExpanded = false }
                                )
                            } else {
                                armadaList.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        "${item.armadaId} - ${item.noPolisi}",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "KM: ${item.kmSaatIni} | Sisa KM: ${item.sisaKm}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                // Status Indicator
                                                Surface(
                                                    shape = CircleShape,
                                                    color = when {
                                                        item.sisaKm < 0 -> MaterialTheme.colorScheme.errorContainer
                                                        item.sisaKm < 1000 -> MaterialTheme.colorScheme.tertiaryContainer
                                                        else -> MaterialTheme.colorScheme.primaryContainer
                                                    },
                                                    modifier = Modifier.size(12.dp)
                                                ) {}
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSelectedArmada(item.armadaId)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // KM Terdeteksi Input
                    OutlinedTextField(
                        value = kmInput,
                        onValueChange = { viewModel.setKmInput(it) },
                        label = { Text(if (ocrLoading) "Memindai Odometer..." else "KM Terdeteksi saat ini") },
                        enabled = !ocrLoading,
                        leadingIcon = {
                            if (ocrLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = "Speedometer")
                            }
                        },
                        suffix = { Text("KM", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("km_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Photo KM upload (Optional)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Foto Odometer KM (Opsional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        try {
                                            cameraLauncher.launch(null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("camera_button"),
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
                                    .testTag("gallery_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Gallery")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih Berkas")
                            }
                        }

                        // Photo Preview Block
                        selectedPhoto?.let { bitmap ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Odometer Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                        .clickable { viewModel.setPhoto(null) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Foto",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Catatan Input (Opsional)
                    OutlinedTextField(
                        value = catatanInput,
                        onValueChange = { viewModel.setCatatanInput(it) },
                        label = { Text("Catatan / Keterangan (Opsional)") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = "Catatan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catatan_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Nota Isi BBM (Opsional)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nota_bbm_section"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = "Nota BBM",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Nota Isi BBM (Opsional)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "JPG/PNG/PDF (Maks 8MB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (notaBbmState == null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.UploadFile,
                                        contentDescription = "Upload Nota",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Belum ada nota BBM dipilih (opsional). Anda dapat melampirkan foto struk BBM atau dokumen PDF.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPermission) {
                                                    try {
                                                        notaCameraLauncher.launch(null)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    notaCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("nota_camera_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = "Foto Nota", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Foto Nota", style = MaterialTheme.typography.labelLarge)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    notaFilePickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/jpg", "application/pdf"))
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("nota_file_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.AttachFile, contentDescription = "Pilih Berkas", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pilih Berkas", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }
                        } else {
                            val currentNota = notaBbmState!!
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("nota_preview_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (currentNota.isPdf) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                modifier = Modifier.size(56.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.PictureAsPdf,
                                                        contentDescription = "Dokumen PDF",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        } else if (currentNota.bitmap != null) {
                                            Image(
                                                bitmap = currentNota.bitmap.asImageBitmap(),
                                                contentDescription = "Thumbnail Nota BBM",
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(56.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Receipt,
                                                        contentDescription = "Nota BBM",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (currentNota.isPdf) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = if (currentNota.isPdf) "PDF" else "GAMBAR",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (currentNota.isPdf) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (currentNota.fileSize > 0) {
                                                    Text(
                                                        text = formatFileSize(currentNota.fileSize),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = currentNota.fileName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Ready",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = notaBbmStatus ?: "Nota BBM siap diunggah.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    notaFilePickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/jpg", "application/pdf"))
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .testTag("nota_change_button"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Ganti", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ganti File", style = MaterialTheme.typography.labelMedium)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.clearNotaBbm() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .testTag("nota_delete_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Hapus File", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error text inside card
                    error?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error Info",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Button Kirim Data
                    Button(
                        onClick = { viewModel.submitLog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_button"),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
                                Text(
                                    "Kirim Data",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Riwayat 7 Hari Terakhir
        val relevantLogs = if (selectedArmadaId.isNotEmpty()) {
            allLogs.filter { it.armadaId.trim().uppercase() == selectedArmadaId.trim().uppercase() }
        } else {
            allLogs
        }

        if (relevantLogs.isNotEmpty()) {
            val displayedFormLogs = if (formShowAllLogs) relevantLogs else relevantLogs.take(7)
            val formatKm = { num: Int ->
                String.format("%,d", num).replace(',', '.')
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (selectedArmadaId.isNotEmpty()) "Riwayat 7 Hari ($selectedArmadaId)" else "Riwayat 7 Hari Terakhir",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (relevantLogs.size > 7 && !formShowAllLogs) {
                                    Text(
                                        text = "Menampilkan 7 hari terakhir dari ${relevantLogs.size} data",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (relevantLogs.size > 7) {
                                TextButton(
                                    onClick = { formShowAllLogs = !formShowAllLogs },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (formShowAllLogs) "7 Hari" else "Lihat Semua",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayedFormLogs.forEach { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${log.armadaId} • ${log.tanggal}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Driver: ${log.namaDriver}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${formatKm(log.kmTerdeteksi)} KM",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (log.catatan.isNotEmpty()) {
                                            Text(
                                                text = "Catatan: ${log.catatan}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val hasOdoPhoto = log.linkFoto.isNotEmpty() && log.linkFoto.startsWith("http")
                                        val hasNotaBbm = log.notaBbmUrl.isNotEmpty() && log.notaBbmUrl.startsWith("http")

                                        if (hasOdoPhoto || hasNotaBbm) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Kolom Kiri: Foto Odometer
                                                if (hasOdoPhoto) {
                                                    val directOdoUrl = getDirectDriveImageUrl(log.linkFoto)
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(120.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { formFullScreenImageUrl = directOdoUrl },
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Box(modifier = Modifier.fillMaxSize()) {
                                                            AsyncImage(
                                                                model = directOdoUrl,
                                                                contentDescription = "Foto Odometer",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.65f),
                                                                shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                                modifier = Modifier.align(Alignment.TopStart)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Speed,
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(11.dp)
                                                                    )
                                                                    Text(
                                                                        "Odometer",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                                        color = Color.White
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(120.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxSize(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Speed,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                "Tanpa Foto Odo",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Kolom Kanan: Foto / PDF Nota BBM
                                                if (hasNotaBbm) {
                                                    val directNotaUrl = getDirectDriveImageUrl(log.notaBbmUrl)
                                                    val isPdf = log.notaBbmUrl.lowercase().contains(".pdf") || log.notaBbmUrl.lowercase().contains("application%2fpdf")
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(120.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                if (isPdf) {
                                                                    try {
                                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(log.notaBbmUrl))
                                                                        context.startActivity(intent)
                                                                    } catch (e: Exception) {
                                                                        e.printStackTrace()
                                                                    }
                                                                } else {
                                                                    formFullScreenImageUrl = directNotaUrl
                                                                }
                                                            },
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Box(modifier = Modifier.fillMaxSize()) {
                                                            if (isPdf) {
                                                                Column(
                                                                    modifier = Modifier
                                                                        .fillMaxSize()
                                                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalArrangement = Arrangement.Center
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.PictureAsPdf,
                                                                        contentDescription = "PDF Nota",
                                                                        tint = MaterialTheme.colorScheme.error,
                                                                        modifier = Modifier.size(28.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        "Dokumen PDF",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                                        color = MaterialTheme.colorScheme.error
                                                                    )
                                                                    Text(
                                                                        "Ketuk untuk buka",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            } else {
                                                                AsyncImage(
                                                                    model = directNotaUrl,
                                                                    contentDescription = "Nota BBM",
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                            Surface(
                                                                color = Color(0xFF1E88E5).copy(alpha = 0.85f),
                                                                shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                                modifier = Modifier.align(Alignment.TopStart)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.LocalGasStation,
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(11.dp)
                                                                    )
                                                                    Text(
                                                                        "Nota BBM",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                                                        color = Color.White
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(120.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxSize(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.ReceiptLong,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                "Tanpa Nota BBM",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (formFullScreenImageUrl != null) {
        FullScreenImageDialog(
            imageUrl = formFullScreenImageUrl!!,
            onDismiss = { formFullScreenImageUrl = null }
        )
    }
}

