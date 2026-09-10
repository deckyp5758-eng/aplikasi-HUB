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
fun StatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    circleBgColor: Color
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(circleBgColor.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = circleBgColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DetailArmadaDialog(
    armada: ArmadaEntity,
    logs: List<LogHarianEntity>,
    banList: List<BanEntity>,
    onDismiss: () -> Unit,
    onUpdateFotoTruck: (String) -> Unit,
    onUpdateFotoService: (String) -> Unit = {},
    onUpdateBan: (posisi: String, barcode: String, tahun: String, kodeBan: String, tanggalUpdate: String) -> Unit,
    onUpdateAki: (barcode: String, tanggalPasang: String, merk: String, status: String) -> Unit,
    banUpdateStatus: String?,
    isBanUpdating: Boolean,
    onClearBanUpdateStatus: () -> Unit,
    isFotoTruckUploading: Boolean = false,
    fotoTruckUploadStatus: String? = null,
    isFotoServiceUploading: Boolean = false,
    fotoServiceUploadStatus: String? = null
) {
    val truckImages = listOf(
        "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1516574187841-cb9cc2ca948b?w=600&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1591768793355-74d7189607f7?w=600&auto=format&fit=crop&q=80"
    )
    val imgUrl = if (!armada.fotoTruck.isNullOrEmpty()) {
        getDirectDriveImageUrl(armada.fotoTruck)
    } else {
        val index = armada.armadaId.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
        truckImages.getOrElse((index - 1) % truckImages.size) { truckImages[0] }
    }
    val serviceImgUrl: String? = if (!armada.fotoService.isNullOrEmpty()) {
        getDirectDriveImageUrl(armada.fotoService)
    } else null
    
    val formatKm = { num: Int ->
        String.format("%,d", num).replace(',', '.')
    }

    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onUpdateFotoTruck(uri.toString())
        }
    }

    val serviceImageLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onUpdateFotoService(uri.toString())
        }
    }

    var showEditBanDialog by remember { mutableStateOf<BanEntity?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showAllLogs by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        var selectedTabState by remember { mutableStateOf(0) }
        val tabTitles = listOf("Pemeliharaan", "Aki & Ban", "Log Harian")

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Image Container - Brand Gradient matching theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .height(130.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: ID, Police Label, No Polisi
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = armada.armadaId,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                            Text(
                                text = "Police Number",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = armada.noPolisi,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                        
                        // Right: Truck Photo with edit button overlay
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { if (!imgUrl.isNullOrEmpty()) fullScreenImageUrl = imgUrl }
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Detail Truck",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            IconButton(
                                onClick = { imageLauncher.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.White, shape = CircleShape)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ganti Foto",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    
                    // Close button outside blue card top corner or inside it. Let's make a beautiful dismiss button at TopEnd
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // TabRow for organized, compact structure
                androidx.compose.material3.TabRow(
                    selectedTabIndex = selectedTabState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        androidx.compose.material3.Tab(
                            selected = selectedTabState == index,
                            onClick = { selectedTabState = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selectedTabState == index) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selectedTabState == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val cArmadaKey = armada.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                val cNopolKey = armada.noPolisi.replace(Regex("[\\s\\-\\.]"), "").uppercase()

                // Aki Information Section (Sheet GID 1886867333)
                val armadaAki = banList.firstOrNull { 
                    it.posisi.trim().uppercase() == "AKI" && (
                        it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cArmadaKey ||
                        (cNopolKey.isNotEmpty() && it.noPolisi.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cNopolKey) ||
                        (cNopolKey.isNotEmpty() && it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cNopolKey)
                    )
                } ?: BanEntity(
                    armadaId = armada.armadaId,
                    noPolisi = armada.noPolisi,
                    posisi = "AKI",
                    noSeri = "0255KDR",
                    ukuran = "12V",
                    merk = "GS Astra 12V",
                    kondisi = "8/2/2023",
                    tekanan = "GS Astra 12V",
                    keterangan = "AMAN",
                    barcode = "0255KDR",
                    tahun = "2023"
                )

                val akiResult = com.example.utils.AkiUtils.calculateAkiStatus(
                    armadaId = armada.armadaId,
                    noPolisi = armada.noPolisi,
                    barcode = armadaAki.barcode,
                    tanggalPasangStr = armadaAki.kondisi,
                    userStatus = armadaAki.keterangan,
                    merk = armadaAki.merk
                )

                // Tire Information Section
                val armadaTires = banList.filter { 
                    it.posisi.trim().uppercase() != "AKI" && (
                        it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cArmadaKey ||
                        (cNopolKey.isNotEmpty() && it.noPolisi.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cNopolKey) ||
                        (cNopolKey.isNotEmpty() && it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cNopolKey)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTabState == 0) {
                        // TAB 0: Maintenance & Documents
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Status Pemeliharaan",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("KM Saat Ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${formatKm(armada.kmSaatIni)} KM", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Batas Servis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${formatKm(armada.kmServiceBerikutnya)} KM", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    
                                    val isAman = armada.sisaKm >= 1000
                                    val progress = (armada.sisaKm.toFloat() / armada.intervalService.coerceAtLeast(1000).toFloat()).coerceIn(0f, 1f)
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = progress,
                                        color = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444),
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Sisa KM: ${formatKm(armada.sisaKm)} KM",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "Interval: ${formatKm(armada.intervalService)} KM",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Dokumen & Perizinan",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text("KIR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(armada.kirDate ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                Text("Pajak Tahunan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(armada.pajakTahunan ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("STNK / Pajak 5 Tahunan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(armada.pajak5Tahunan ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Foto Gantungan Service",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Kolom M • Google Sheets Armada",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { serviceImageLauncher.launch("image/*") },
                                            enabled = !isFotoServiceUploading,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (serviceImgUrl.isNullOrEmpty()) "Upload Foto" else "Ganti Foto",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .clickable { if (!serviceImgUrl.isNullOrEmpty()) fullScreenImageUrl = serviceImgUrl },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!serviceImgUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = serviceImgUrl,
                                                contentDescription = "Foto Gantungan Service Armada",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Text(
                                                    text = "Belum Ada Foto Gantungan Service",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = "Klik 'Upload Foto' untuk menambahkan dokumentasi tag service.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }

                                        if (isFotoServiceUploading) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                                                    Text(
                                                        text = "Mengunggah foto...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    fotoServiceUploadStatus?.let { status ->
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (status.startsWith("Error") || status.startsWith("Gagal")) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedTabState == 1) {
                        // TAB 1: Aki & Ban
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (akiResult.isDue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(1.dp, if (akiResult.isDue) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        if (akiResult.isDue) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BatteryChargingFull,
                                                    contentDescription = null,
                                                    tint = if (akiResult.isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Informasi Aki Unit",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Sheet GID 1886867333 • Masa Pakai 2 Thn",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Edit Aki button with prominent Pencil Icon
                                        IconButton(
                                            onClick = { showEditBanDialog = armadaAki },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Aki",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Warning Notice (Compact style)
                                    if (akiResult.isDue) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (akiResult.isExpired) 
                                                    "🚨 Aki Expired! Harap segera ganti Aki." 
                                                else 
                                                    "⚠️ Usia Aki Mendekati 2 Tahun (<30 Hari).",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    // Grid Details (Highly polished & compact)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Col 1: Tanggal Pasang
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("TANGGAL PASANG AKI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(akiResult.tanggalPasang, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                            }

                                            // Col 2: Tanggal Ganti berikutnya (2 Tahun)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("JADWAL GANTI BERIKUTNYA", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(akiResult.tanggalGantiBerikutnya, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (akiResult.isDue) MaterialTheme.colorScheme.error else Color(0xFF166534))
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Col 3: Barcode
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("BARCODE AKI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(akiResult.barcode, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }

                                            // Col 4: Status Badge
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                                Text("KELAYAKAN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                val badgeColor = when {
                                                    akiResult.isExpired -> MaterialTheme.colorScheme.error
                                                    akiResult.isWarning30Days -> Color(0xFFD97706)
                                                    else -> Color(0xFF16A34A)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(badgeColor, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = akiResult.statusLabel.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Header matching "Informasi Ban Unit • 5 Ban Terdaftar"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TripOrigin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = buildAnnotatedString {
                                            append("Informasi Ban Unit")
                                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)) {
                                                append(" • ")
                                            }
                                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Normal)) {
                                                append("${armadaTires.size} Ban Terdaftar")
                                            }
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                // Interactive Chassis Map Blueprint (V2 visual enhancement)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Denah Posisi Ban Armada",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = "Sentuh ban untuk edit",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        // Visual Blueprint Interactive Container
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF090D16)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Blueprint background image
                                            AsyncImage(
                                                model = R.drawable.truck_chassis_diagram_1786962282503,
                                                contentDescription = "Diagram Sasis Truk",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer(alpha = 0.45f)
                                            )

                                            // Interactive Tire Position Pills
                                            val flBan = armadaTires.find { it.posisi.contains("Kiri", true) && it.posisi.contains("Depan", true) }
                                            val frBan = armadaTires.find { it.posisi.contains("Kanan", true) && it.posisi.contains("Depan", true) }
                                            val rlBan = armadaTires.find { it.posisi.contains("Kiri", true) && it.posisi.contains("Belakang", true) }
                                            val rrBan = armadaTires.find { it.posisi.contains("Kanan", true) && it.posisi.contains("Belakang", true) }
                                            val spBan = armadaTires.find { it.posisi.contains("Serep", true) || it.posisi.contains("SP", true) }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Front Row (FL & FR)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // FL Pill
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                                            .clickable { flBan?.let { showEditBanDialog = it } }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("FL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
                                                            Text(flBan?.barcode ?: flBan?.noSeri ?: "-", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                                        }
                                                    }

                                                    // FR Pill
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                                            .clickable { frBan?.let { showEditBanDialog = it } }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("FR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
                                                            Text(frBan?.barcode ?: frBan?.noSeri ?: "-", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                                        }
                                                    }
                                                }

                                                // Rear Row (RL & RR)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // RL Pill
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                                            .clickable { rlBan?.let { showEditBanDialog = it } }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("RL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
                                                            Text(rlBan?.barcode ?: rlBan?.noSeri ?: "-", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                                        }
                                                    }

                                                    // RR Pill
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                                            .clickable { rrBan?.let { showEditBanDialog = it } }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("RR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
                                                            Text(rrBan?.barcode ?: rrBan?.noSeri ?: "-", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                                                        }
                                                    }
                                                }

                                                // Spare Tire (SEREP) Center Bottom
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                                            .clickable { spBan?.let { showEditBanDialog = it } }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("SEREP", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF59E0B))
                                                            Text(spBan?.barcode ?: spBan?.noSeri ?: "-", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTabState == 2) {
                        // TAB 2: Catatan & Daily Logs
                        val catVal = armada.catattan.trim()
                        if (catVal.isNotEmpty()) {
                            val isUrl = catVal.startsWith("http://") || catVal.startsWith("https://")
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isUrl) {
                                        Text(
                                            text = "Foto Odometer Terakhir", 
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), 
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val directUrl = getDirectDriveImageUrl(catVal)
                                                AsyncImage(
                                                    model = directUrl,
                                                    contentDescription = "Odometer Terakhir",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(220.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { fullScreenImageUrl = directUrl },
                                                    contentScale = ContentScale.Crop
                                                )
                                                Text(
                                                    text = "Klik gambar untuk melihat ukuran penuh / mengunduh",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Text("Catatan Terakhir Armada", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = catVal,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        val armadaLogs = logs.filter { it.armadaId.trim().uppercase() == armada.armadaId.trim().uppercase() }
                        val displayedLogs = if (showAllLogs) armadaLogs else armadaLogs.take(7)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Riwayat Log ${armada.armadaId}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (armadaLogs.isNotEmpty() && !showAllLogs && armadaLogs.size > 7) {
                                            Text(
                                                text = "Menampilkan 7 hari terakhir (${armadaLogs.size} total)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (armadaLogs.size > 7) {
                                        TextButton(
                                            onClick = { showAllLogs = !showAllLogs },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (showAllLogs) "7 Hari" else "Lihat Semua",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                                
                                if (armadaLogs.isEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Belum ada riwayat pengiriman log harian.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        displayedLogs.forEach { log ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(log.tanggal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("Driver: ${log.namaDriver}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                "${formatKm(log.kmTerdeteksi)} KM",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    if (log.catatan.isNotEmpty()) {
                                                        Text(
                                                            "Catatan: ${log.catatan}",
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
                                                                        .height(130.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .clickable { fullScreenImageUrl = directOdoUrl },
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
                                                                        .height(130.dp),
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
                                                                            modifier = Modifier.size(24.dp)
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

                                                            // Kolom Kanan: Nota BBM
                                                            if (hasNotaBbm) {
                                                                val directNotaUrl = getDirectDriveImageUrl(log.notaBbmUrl)
                                                                val isPdf = log.notaBbmUrl.lowercase().contains(".pdf") || log.notaBbmUrl.lowercase().contains("application%2fpdf")
                                                                Card(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(130.dp)
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
                                                                                fullScreenImageUrl = directNotaUrl
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
                                                                                    modifier = Modifier.size(32.dp)
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
                                                                        .height(130.dp),
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
                                                                            modifier = Modifier.size(24.dp)
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
            }
        }
    }

    // Sub-dialog for Editing Ban / Aki Details
    if (showEditBanDialog != null) {
        val banToEdit = showEditBanDialog!!
        val isAkiEdit = banToEdit.posisi.trim().uppercase() == "AKI"
        var inputBarcode by remember { mutableStateOf(banToEdit.barcode ?: banToEdit.noSeri) }
        var inputTahun by remember { mutableStateOf(banToEdit.tahun ?: "2023") }
        var inputKodeBan by remember { mutableStateOf(banToEdit.kodeBan ?: "") }
        var inputTanggalPasang by remember { mutableStateOf(banToEdit.kondisi) }
        var inputMerk by remember { mutableStateOf(banToEdit.merk) }
        var inputKeterangan by remember { mutableStateOf(banToEdit.keterangan) }
        
        Dialog(onDismissRequest = { showEditBanDialog = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isAkiEdit) "Edit Data Aki Armada (Sheet GID 1886867333)" else "Edit Data Ban",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isAkiEdit) "Aki Armada Unit ${banToEdit.armadaId} (${banToEdit.noPolisi})" else "Posisi: ${banToEdit.posisi}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Armada ID: ${banToEdit.armadaId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    
                    OutlinedTextField(
                        value = inputBarcode,
                        onValueChange = { inputBarcode = it },
                        label = { Text(if (isAkiEdit) "Barcode Aki" else "Barcode Ban") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (!isAkiEdit) {
                        OutlinedTextField(
                            value = inputTahun,
                            onValueChange = { inputTahun = it },
                            label = { Text("Tahun Ban") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = inputKodeBan,
                            onValueChange = { inputKodeBan = it },
                            label = { Text("Kode Ban") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = inputTanggalPasang,
                            onValueChange = { inputTanggalPasang = it },
                            label = { Text("Tanggal Pasang Aki (Bln/Tgl/Thn, ex: 8/2/2023)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputMerk,
                            onValueChange = { inputMerk = it },
                            label = { Text("Merk / Tipe Aki (ex: GS Astra 12V)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputKeterangan,
                            onValueChange = { inputKeterangan = it },
                            label = { Text("Status Kelayakan (AMAN / GANTI)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEditBanDialog = null }
                        ) {
                            Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                               if (isAkiEdit) {
                                   onUpdateAki(inputBarcode, inputTanggalPasang, inputMerk, inputKeterangan)
                               } else {
                                   val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                   val currentDate = sdf.format(java.util.Date())
                                   onUpdateBan(banToEdit.posisi, inputBarcode, inputTahun, inputKodeBan, currentDate)
                               }
                               showEditBanDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simpan", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }

    // Status / Progress overlay dialogue
    if (banUpdateStatus != null) {
        val isError = banUpdateStatus!!.contains("Error", ignoreCase = true)
        val isSuccess = banUpdateStatus!!.contains("Sukses", ignoreCase = true)
        
        AlertDialog(
            onDismissRequest = { if (!isBanUpdating) onClearBanUpdateStatus() },
            title = {
                Text(
                    text = if (isError) "Gagal" else if (isSuccess) "Sukses" else "Memproses...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isError) Color(0xFFEF4444) else if (isSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isBanUpdating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = banUpdateStatus!!.replace("Sukses: ", "").replace("Error: ", ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                if (!isBanUpdating) {
                    TextButton(onClick = onClearBanUpdateStatus) {
                        Text("OK", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (fullScreenImageUrl != null) {
        FullScreenImageDialog(
            imageUrl = fullScreenImageUrl!!,
            onDismiss = { fullScreenImageUrl = null }
        )
    }
}

@Composable
fun HistoryScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val banList by viewModel.banList.collectAsStateWithLifecycle()
    var selectedArmadaDetail by remember { mutableStateOf<ArmadaEntity?>(null) }
    
    val formatKm = { num: Int ->
        String.format("%,d", num).replace(',', '.')
    }
    
    val truckImages = listOf(
        "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1516574187841-cb9cc2ca948b?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1591768793355-74d7189607f7?w=400&auto=format&fit=crop&q=80"
    )
    
    selectedArmadaDetail?.let { armada ->
        val liveArmada = armadaList.find { it.armadaId == armada.armadaId } ?: armada
        val banUpdateStatus by viewModel.banUpdateStatus.collectAsStateWithLifecycle()
        val isBanUpdating by viewModel.isBanUpdating.collectAsStateWithLifecycle()
        val isFotoTruckUploading by viewModel.isFotoTruckUploading.collectAsStateWithLifecycle()
        val fotoTruckUploadStatus by viewModel.fotoTruckUploadStatus.collectAsStateWithLifecycle()
        val isFotoServiceUploading by viewModel.isFotoServiceUploading.collectAsStateWithLifecycle()
        val fotoServiceUploadStatus by viewModel.fotoServiceUploadStatus.collectAsStateWithLifecycle()
        
        DetailArmadaDialog(
            armada = liveArmada,
            logs = logs,
            banList = banList,
            onDismiss = { selectedArmadaDetail = null },
            onUpdateFotoTruck = { uriString ->
                viewModel.updateArmadaFotoTruck(armada.armadaId, uriString)
            },
            onUpdateFotoService = { uriString ->
                viewModel.updateArmadaFotoService(armada.armadaId, uriString)
            },
            onUpdateBan = { posisi, barcode, tahun, kodeBan, tanggalUpdate ->
                viewModel.updateBan(armada.armadaId, posisi, barcode, tahun, kodeBan, tanggalUpdate)
            },
            onUpdateAki = { barcode, tanggalPasang, merk, status ->
                viewModel.updateAki(armada.armadaId, barcode, tanggalPasang, merk, status)
            },
            banUpdateStatus = banUpdateStatus,
            isBanUpdating = isBanUpdating,
            onClearBanUpdateStatus = { viewModel.clearBanUpdateStatus() },
            isFotoTruckUploading = isFotoTruckUploading,
            fotoTruckUploadStatus = fotoTruckUploadStatus,
            isFotoServiceUploading = isFotoServiceUploading,
            fotoServiceUploadStatus = fotoServiceUploadStatus
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Beautiful Header Banner Overlaid with Title
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = R.drawable.img_fleet_banner,
                        contentDescription = "Fleet Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x330054A6),
                                        Color(0x880054A6),
                                        Color(0xFF0054A6)
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Surface(
                                    color = Color.Red,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("2", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Column {
                            Text(
                                text = "Status Armada",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Monitor kondisi armada secara real-time",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // 2. Horizontal Stats Row Box (Dynamic values computed real-time!)
            item {
                val totalArmada = armadaList.size
                val amanCount = armadaList.count { it.sisaKm >= 1000 }
                val perluPerhatianCount = armadaList.count { it.sisaKm < 1000 }
                val averageSisaKm = if (armadaList.isNotEmpty()) {
                    armadaList.map { it.sisaKm }.average().toInt()
                } else {
                    0
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Armada",
                        value = "$totalArmada",
                        unit = "Unit",
                        icon = Icons.Default.LocalShipping,
                        iconColor = Color(0xFF0054A6),
                        circleBgColor = Color(0xFFE6F0FA)
                    )
                    StatCard(
                        title = "Aman",
                        value = "$amanCount",
                        unit = "Unit",
                        icon = Icons.Default.CheckCircle,
                        iconColor = Color(0xFF2E7D32),
                        circleBgColor = Color(0xFFE8F5E9)
                    )
                    StatCard(
                        title = "Perlu Perhatian",
                        value = "$perluPerhatianCount",
                        unit = "Unit",
                        icon = Icons.Default.Warning,
                        iconColor = Color(0xFFD32F2F),
                        circleBgColor = Color(0xFFFFEBEE)
                    )
                    StatCard(
                        title = "Rata-rata Sisa KM",
                        value = formatKm(averageSisaKm),
                        unit = "KM",
                        icon = Icons.Default.TrendingUp,
                        iconColor = Color(0xFF8E24AA),
                        circleBgColor = Color(0xFFF3E5F5)
                    )
                }
            }
            
            // 3. Monitor Armada Title Section
            item {
                val currentTimeStr = remember {
                    val formatter = java.text.SimpleDateFormat("HH:mm 'WIB'", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                    }
                    formatter.format(java.util.Date())
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Monitor Armada",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), shape = CircleShape)
                        )
                        Text(
                            text = "Terakhir diperbarui Hari ini, $currentTimeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // 4. Fleet Vehicles List
            if (armadaList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada data armada tersedia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(armadaList.withIndex().toList()) { indexedValue ->
                    val index = indexedValue.index
                    val item = indexedValue.value
                    val imgUrl = if (!item.fotoTruck.isNullOrEmpty()) {
                        getDirectDriveImageUrl(item.fotoTruck)
                    } else {
                        truckImages.getOrElse(index % truckImages.size) { truckImages[0] }
                    }
                    val isAman = item.sisaKm >= 1000
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                selectedArmadaDetail = item
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Image, License Plate, ID Tag, and Status Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Image Thumbnail with Index badge
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Truck Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    Surface(
                                        color = Color(0xFF0054A6),
                                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = String.format("%02d", index + 1),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                
                                // 2. Right info details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Stylized License Plate and Fleet ID Tag
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.noPolisi,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = item.armadaId,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                )
                                            }
                                        }
                                        
                                        // Status Pill (Aman vs Perlu Servis)
                                        Surface(
                                            color = if (isAman) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (isAman) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(if (isAman) Color(0xFF10B981) else Color(0xFFEF4444), shape = CircleShape)
                                                )
                                                Text(
                                                    text = if (isAman) "Aman" else "Butuh Servis",
                                                    color = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Sisa KM Description
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAman) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = if (isAman) "Kondisi prima & aman jalan" else "Batas servis terlampaui/hampir dekat",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                            color = if (isAman) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                            
                            // Horizontal divider
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 1.dp
                            )
                            
                            // Row 2: Grid-like display of KM statistics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "KM Saat Ini",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = "${formatKm(item.kmSaatIni)} KM",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Batas Servis",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                                        Text(
                                            text = "${formatKm(item.kmServiceBerikutnya)} KM",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Column(modifier = Modifier.weight(1.1f)) {
                                    Text(
                                        text = "Sisa Pemakaian",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444), modifier = Modifier.size(11.dp))
                                        Text(
                                            text = "${formatKm(item.sisaKm)} KM",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                            color = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                            
                            // Row 3: Modern Progress Bar with percentage info
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                val progress = (item.sisaKm.toFloat() / item.intervalService.coerceAtLeast(1000).toFloat()).coerceIn(0f, 1f)
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = progress,
                                    color = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sisa KM sebelum servis: ${formatKm(item.sisaKm)} KM",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Normal),
                                        color = if (isAman) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFEF4444)
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                                        color = if (isAman) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                            
                            // Row 4: Administration Documents (PJK, KIR, STNK) and Action CTA
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(8.dp))
                                            Text(
                                                text = "PJK: ${item.pajakTahunan ?: "-"}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp))
                                            Text(
                                                text = "KIR: ${item.kirDate ?: "-"}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
 
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(8.dp))
                                            Text(
                                                text = "STNK: ${item.pajak5Tahunan ?: "-"}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedArmadaDetail = item
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Detail",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

