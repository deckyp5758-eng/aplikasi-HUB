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
fun PengajuanScreen(viewModel: FleetViewModel, driverName: String) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    val banList by viewModel.banList.collectAsStateWithLifecycle()
    val pengajuanList by viewModel.pengajuanList.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isPengajuanSubmitting.collectAsStateWithLifecycle()
    val statusMsg by viewModel.pengajuanStatusMessage.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Form, 1: Riwayat
    var selectedArmadaId by remember { mutableStateOf(armadaList.firstOrNull()?.armadaId ?: "") }
    var selectedCategory by remember { mutableStateOf("Ban") } // "Ban" or "Aksesoris"

    // Aksesoris State
    var aksesorisNama by remember { mutableStateOf("") }
    var aksesorisCatatan by remember { mutableStateOf("") }
    var aksesorisPhotos by remember { mutableStateOf(emptyList<Bitmap>()) }

    // Ban State
    var selectedBanPosisi by remember { mutableStateOf("") }
    var banMatchedVerified by remember { mutableStateOf(false) }
    var fotoTahunBan by remember { mutableStateOf<Bitmap?>(null) }
    var fotoBarcodeBan by remember { mutableStateOf<Bitmap?>(null) }
    var fotoPenampakan1 by remember { mutableStateOf<Bitmap?>(null) }
    var fotoPenampakan2 by remember { mutableStateOf<Bitmap?>(null) }
    var banCatatan by remember { mutableStateOf("") }

    var validationErrorMsg by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMsg by remember { mutableStateOf("") }

    var armadaDropdownExpanded by remember { mutableStateOf(false) }
    var banDropdownExpanded by remember { mutableStateOf(false) }

    val currentArmada = armadaList.find { it.armadaId == selectedArmadaId }
    val currentArmadaBans = remember(selectedArmadaId, banList) {
        banList.filter { 
            it.armadaId.equals(selectedArmadaId, ignoreCase = true) || 
            it.noPolisi.equals(currentArmada?.noPolisi ?: "", ignoreCase = true)
        }.filter { !it.posisi.contains("AKI", ignoreCase = true) }
    }
    val selectedBan = currentArmadaBans.find { it.posisi.equals(selectedBanPosisi, ignoreCase = true) }

    // Multi-photo gallery launcher for Aksesoris
    val aksesorisGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    aksesorisPhotos = aksesorisPhotos + bitmap
                    validationErrorMsg = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Aksesoris Camera Launcher
    val aksesorisCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            aksesorisPhotos = aksesorisPhotos + bitmap
            validationErrorMsg = null
        }
    }

    // Ban Specific Camera Launchers
    var activeBanPhotoTag by remember { mutableStateOf("") }
    val banCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            when (activeBanPhotoTag) {
                "Tahun Ban" -> fotoTahunBan = bitmap
                "Barcode" -> fotoBarcodeBan = bitmap
                "Penampakan 1" -> fotoPenampakan1 = bitmap
                "Penampakan 2" -> fotoPenampakan2 = bitmap
            }
            validationErrorMsg = null
        }
    }

    // Ban Specific Gallery Launchers
    val banGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    when (activeBanPhotoTag) {
                        "Tahun Ban" -> fotoTahunBan = bitmap
                        "Barcode" -> fotoBarcodeBan = bitmap
                        "Penampakan 1" -> fotoPenampakan1 = bitmap
                        "Penampakan 2" -> fotoPenampakan2 = bitmap
                    }
                    validationErrorMsg = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPengajuan()
    }

    LaunchedEffect(armadaList) {
        if (selectedArmadaId.isEmpty() && armadaList.isNotEmpty()) {
            selectedArmadaId = armadaList.first().armadaId
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text("Pengajuan Berhasil", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(successDialogMsg)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        // Reset Form
                        aksesorisNama = ""
                        aksesorisCatatan = ""
                        aksesorisPhotos = emptyList()
                        banMatchedVerified = false
                        selectedBanPosisi = ""
                        fotoTahunBan = null
                        fotoBarcodeBan = null
                        fotoPenampakan1 = null
                        fotoPenampakan2 = null
                        banCatatan = null.toString().replace("null", "")
                        selectedTabIndex = 1 // Switch to History Tab
                    }
                ) {
                    Text("Lihat Riwayat")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Buat Pengajuan", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { 
                    selectedTabIndex = 1
                    viewModel.refreshPengajuan()
                },
                text = { Text("Riwayat (${pengajuanList.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        if (selectedTabIndex == 0) {
            // Form Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Choice of Armada
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "1. Pilih Armada",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { armadaDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val displayArmada = armadaList.find { it.armadaId == selectedArmadaId }
                                        Text(
                                            text = if (displayArmada != null) "${displayArmada.armadaId} - ${displayArmada.noPolisi}" else "Pilih Armada...",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = armadaDropdownExpanded,
                                    onDismissRequest = { armadaDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    armadaList.forEach { arm ->
                                        DropdownMenuItem(
                                            text = { Text("${arm.armadaId} - ${arm.noPolisi} (${arm.status})") },
                                            onClick = {
                                                selectedArmadaId = arm.armadaId
                                                selectedBanPosisi = ""
                                                armadaDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Choice of Category (Aksesoris vs Ban)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "2. Pilih Kategori Pengajuan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Ban Option
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = "Ban" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedCategory == "Ban") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (selectedCategory == "Ban") BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Circle,
                                            contentDescription = null,
                                            tint = if (selectedCategory == "Ban") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "Ban",
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedCategory == "Ban") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Aksesoris Option
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = "Aksesoris" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedCategory == "Aksesoris") Color(0xFFD81B60).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (selectedCategory == "Aksesoris") BorderStroke(2.dp, Color(0xFFD81B60)) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = if (selectedCategory == "Aksesoris") Color(0xFFD81B60) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "Aksesoris",
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedCategory == "Aksesoris") Color(0xFFD81B60) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Category Form Logic
                if (selectedCategory == "Aksesoris") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "3. Detail Pengajuan Aksesoris",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFD81B60)
                                )

                                OutlinedTextField(
                                    value = aksesorisNama,
                                    onValueChange = { aksesorisNama = it },
                                    label = { Text("Nama Aksesoris / Barang *") },
                                    placeholder = { Text("Contoh: Kaca Spion, Talang Air, Terpal 4x6, Dongkrak") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = aksesorisCatatan,
                                    onValueChange = { aksesorisCatatan = it },
                                    label = { Text("Catatan / Alasan Pengajuan") },
                                    placeholder = { Text("Contoh: Spion pecah terkena ranting pohon di jalan") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 3
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Upload Foto Aksesoris (Bebas / Bebas Jumlah Foto)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                                    if (aksesorisPhotos.isNotEmpty()) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(aksesorisPhotos) { bmp ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(90.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                ) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    IconButton(
                                                        onClick = { aksesorisPhotos = aksesorisPhotos - bmp },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .size(24.dp)
                                                            .background(Color.Red, CircleShape)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                    aksesorisCameraLauncher.launch(null)
                                                } else {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Kamera", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { aksesorisGalleryLauncher.launch("image/*") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Galeri", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Category: Ban
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "3. Pilih Ban & Mencocokkan Data",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (currentArmadaBans.isEmpty()) {
                                    Text(
                                        text = "⚠️ Tidak ada data ban terdaftar untuk armada $selectedArmadaId.",
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { banDropdownExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (selectedBan != null) "${selectedBan.posisi} (${selectedBan.merk} - Barcode: ${selectedBan.barcode ?: "-"})" else "Pilih Ban Armada...",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = banDropdownExpanded,
                                            onDismissRequest = { banDropdownExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.85f)
                                        ) {
                                            currentArmadaBans.forEach { b ->
                                                DropdownMenuItem(
                                                    text = { Text("Posisi: ${b.posisi} | ${b.merk} | Barcode: ${b.barcode ?: "-"}") },
                                                    onClick = {
                                                        selectedBanPosisi = b.posisi
                                                        banMatchedVerified = false
                                                        banDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    selectedBan?.let { b ->
                                        // Detail Ban Card for Verification
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("📋 DETAIL DATA BAN YANG AKAN DIGANTI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                                Text("• Posisi: ${b.posisi}", fontWeight = FontWeight.Medium)
                                                Text("• No Seri / Barcode: ${b.barcode ?: b.noSeri}", fontWeight = FontWeight.Medium)
                                                Text("• Merk / Ukuran: ${b.merk} (${b.ukuran})", fontWeight = FontWeight.Medium)
                                                Text("• Kondisi Saat Ini: ${b.kondisi} (Tekanan: ${b.tekanan})", fontWeight = FontWeight.Medium)
                                                Text("• Tahun Ban: ${b.tahun ?: "-"}", fontWeight = FontWeight.Medium)
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { banMatchedVerified = !banMatchedVerified }
                                        ) {
                                            Checkbox(
                                                checked = banMatchedVerified,
                                                onCheckedChange = { banMatchedVerified = it }
                                            )
                                            Text(
                                                text = "Data di atas sudah SESUAI dengan ban yang akan diajukan penggantian.",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (banMatchedVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mandatory 4 Ban Photos Section
                    if (selectedBan != null && banMatchedVerified) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "4. Upload 4 Foto Ban (Wajib)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Photo Slot 1: Tahun Ban
                                    BanPhotoSlot(
                                        title = "Foto 1: Tahun Ban (DOT Code)",
                                        bitmap = fotoTahunBan,
                                        onCameraClick = {
                                            activeBanPhotoTag = "Tahun Ban"
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                banCameraLauncher.launch(null)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        onGalleryClick = {
                                            activeBanPhotoTag = "Tahun Ban"
                                            banGalleryLauncher.launch("image/*")
                                        },
                                        onRemoveClick = { fotoTahunBan = null }
                                    )

                                    // Photo Slot 2: Barcode
                                    BanPhotoSlot(
                                        title = "Foto 2: Barcode Ban",
                                        bitmap = fotoBarcodeBan,
                                        onCameraClick = {
                                            activeBanPhotoTag = "Barcode"
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                banCameraLauncher.launch(null)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        onGalleryClick = {
                                            activeBanPhotoTag = "Barcode"
                                            banGalleryLauncher.launch("image/*")
                                        },
                                        onRemoveClick = { fotoBarcodeBan = null }
                                    )

                                    // Photo Slot 3: View Penampakan Ban 1
                                    BanPhotoSlot(
                                        title = "Foto 3: Penampakan Ban (Tampak Samping/Tread 1)",
                                        bitmap = fotoPenampakan1,
                                        onCameraClick = {
                                            activeBanPhotoTag = "Penampakan 1"
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                banCameraLauncher.launch(null)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        onGalleryClick = {
                                            activeBanPhotoTag = "Penampakan 1"
                                            banGalleryLauncher.launch("image/*")
                                        },
                                        onRemoveClick = { fotoPenampakan1 = null }
                                    )

                                    // Photo Slot 4: View Penampakan Ban 2
                                    BanPhotoSlot(
                                        title = "Foto 4: Penampakan Ban (Tampak Samping/Tread 2)",
                                        bitmap = fotoPenampakan2,
                                        onCameraClick = {
                                            activeBanPhotoTag = "Penampakan 2"
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                banCameraLauncher.launch(null)
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        onGalleryClick = {
                                            activeBanPhotoTag = "Penampakan 2"
                                            banGalleryLauncher.launch("image/*")
                                        },
                                        onRemoveClick = { fotoPenampakan2 = null }
                                    )

                                    OutlinedTextField(
                                        value = banCatatan,
                                        onValueChange = { banCatatan = it },
                                        label = { Text("Catatan Driver Untuk Pengajuan Ban") },
                                        placeholder = { Text("Contoh: Ban aus gundul / ada kawat keluar") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Validation Message
                validationErrorMsg?.let { err ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(err, color = Color.Red, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Submit Button
                item {
                    Button(
                        onClick = {
                            validationErrorMsg = null
                            val arm = armadaList.find { it.armadaId == selectedArmadaId }
                            val noPolisi = arm?.noPolisi ?: ""

                            if (selectedArmadaId.isEmpty()) {
                                validationErrorMsg = "⚠️ Silakan pilih Armada terlebih dahulu."
                                return@Button
                            }

                            val filesToSubmit = mutableListOf<MediaFileItem>()

                            if (selectedCategory == "Aksesoris") {
                                if (aksesorisNama.trim().isEmpty()) {
                                    validationErrorMsg = "⚠️ Silakan isi Nama Aksesoris yang diajukan."
                                    return@Button
                                }
                                if (aksesorisPhotos.isEmpty()) {
                                    validationErrorMsg = "⚠️ Silakan sertakan minimal 1 foto aksesoris."
                                    return@Button
                                }

                                aksesorisPhotos.forEachIndexed { idx, bmp ->
                                    val stream = ByteArrayOutputStream()
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                                    val b64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                                    filesToSubmit.add(
                                        MediaFileItem(
                                            base64 = b64,
                                            fileName = "Aksesoris_${selectedArmadaId}_${idx+1}.jpg",
                                            fileTag = "Aksesoris_${idx+1}"
                                        )
                                    )
                                }

                                viewModel.submitPengajuan(
                                    armadaId = selectedArmadaId,
                                    noPolisi = noPolisi,
                                    kategori = "Aksesoris",
                                    detail = "Aksesoris: ${aksesorisNama.trim()}",
                                    catatan = aksesorisCatatan,
                                    mediaFiles = filesToSubmit,
                                    onResult = { success, msg ->
                                        if (success) {
                                            successDialogMsg = "Pengajuan Aksesoris berhasil dikirim ke Google Sheets (GID 1517362778) & disimpan di Google Drive 1Kk9f5f8_o8puwA3ZNJAKa_9cVy5TN5Lh!"
                                            showSuccessDialog = true
                                        } else {
                                            validationErrorMsg = "❌ Gagal mengirim: $msg"
                                        }
                                    }
                                )
                            } else {
                                // Ban Validation
                                if (selectedBan == null) {
                                    validationErrorMsg = "⚠️ Silakan pilih Ban armada yang akan diajukan."
                                    return@Button
                                }
                                if (!banMatchedVerified) {
                                    validationErrorMsg = "⚠️ Silakan centang konfirmasi pencocokan data ban."
                                    return@Button
                                }
                                if (fotoTahunBan == null || fotoBarcodeBan == null || fotoPenampakan1 == null || fotoPenampakan2 == null) {
                                    validationErrorMsg = "⚠️ Silakan lengkapi ke-4 foto ban (Tahun, Barcode, Penampakan 1, Penampakan 2)."
                                    return@Button
                                }

                                val listBitmaps = listOf(
                                    Pair("Tahun Ban", fotoTahunBan!!),
                                    Pair("Barcode", fotoBarcodeBan!!),
                                    Pair("Penampakan 1", fotoPenampakan1!!),
                                    Pair("Penampakan 2", fotoPenampakan2!!)
                                )

                                listBitmaps.forEach { (tag, bmp) ->
                                    val stream = ByteArrayOutputStream()
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                                    val b64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                                    filesToSubmit.add(
                                        MediaFileItem(
                                            base64 = b64,
                                            fileName = "${selectedArmadaId}_${tag.replace(" ", "_")}.jpg",
                                            fileTag = tag
                                        )
                                    )
                                }

                                val banDetailStr = "Posisi: ${selectedBan.posisi} | Barcode: ${selectedBan.barcode ?: selectedBan.noSeri} | Merk: ${selectedBan.merk} | Ukuran: ${selectedBan.ukuran} | Kondisi: ${selectedBan.kondisi}"

                                viewModel.submitPengajuan(
                                    armadaId = selectedArmadaId,
                                    noPolisi = noPolisi,
                                    kategori = "Ban",
                                    detail = banDetailStr,
                                    catatan = banCatatan,
                                    mediaFiles = filesToSubmit,
                                    onResult = { success, msg ->
                                        if (success) {
                                            successDialogMsg = "Pengajuan Ban ${selectedBan.posisi} berhasil dikirim ke Google Sheets (GID 1517362778) & disimpan di Drive folder PENGAJUAN!"
                                            showSuccessDialog = true
                                        } else {
                                            validationErrorMsg = "❌ Gagal mengirim: $msg"
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == "Aksesoris") Color(0xFFD81B60) else Color(0xFF0054A6)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Mengirim Pengajuan...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("KIRIM PENGAJUAN ${selectedCategory.uppercase()}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // History Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Pengajuan Tersimpan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { viewModel.refreshPengajuan() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                        }
                    }
                }

                if (pengajuanList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Text("Belum Ada Riwayat Pengajuan", fontWeight = FontWeight.Bold)
                                Text("Data pengajuan yang Anda kirim akan muncul di sini.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(pengajuanList) { item ->
                        PengajuanCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun BanPhotoSlot(
    title: String,
    bitmap: Bitmap?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (bitmap != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (bitmap != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface)
                if (bitmap != null) {
                    Text("✓ Terupload", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCameraClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kamera", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galeri", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PengajuanCard(item: PengajuanEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (item.kategori == "Aksesoris") Icons.Default.Widgets else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (item.kategori == "Aksesoris") Color(0xFFD81B60) else Color(0xFF0054A6),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${item.kategori.uppercase()} • ${item.armadaId}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Surface(
                    color = Color(0xFFE65100).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = item.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    )
                }
            }

            Text("No: ${item.noPengajuan} | ${item.tanggal}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Driver: ${item.driver} (${item.noPolisi})", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = item.detail,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (item.catatan.isNotEmpty()) {
                Text("Catatan: ${item.catatan}", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Photos Row
            val photoUrls = remember(item) {
                listOf(item.foto1Url, item.foto2Url, item.foto3Url, item.foto4Url)
                    .filter { it.isNotEmpty() && !it.startsWith("Local") }
            }

            if (photoUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(photoUrls) { url ->
                        val directUrl = getDirectDriveImageUrl(url)
                        AsyncImage(
                            model = directUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
