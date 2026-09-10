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
fun DashboardScreen(
    viewModel: FleetViewModel,
    driverName: String,
    onNavigateToScreen: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val banList by viewModel.banList.collectAsStateWithLifecycle()

    val dueAkiList = remember(banList) {
        banList.filter { it.posisi.trim().uppercase() == "AKI" }.map { aki ->
            com.example.utils.AkiUtils.calculateAkiStatus(
                armadaId = aki.armadaId,
                noPolisi = aki.noPolisi,
                barcode = aki.barcode,
                tanggalPasangStr = aki.kondisi,
                userStatus = aki.keterangan,
                merk = aki.merk
            )
        }.filter { it.isDue }
    }
    
    val greetingText = remember {
        val hour = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta")).get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Selamat Pagi"
            in 12..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
        "$greeting, ${driverName.split(" ").firstOrNull() ?: driverName}! \uD83D\uDC4B"
    }

    var showLaporanDialog by remember { mutableStateOf(false) }
    var showCatatanDriverDialog by remember { mutableStateOf(false) }

    if (showCatatanDriverDialog) {
        CatatanDriverDialog(
            viewModel = viewModel,
            driverName = driverName,
            onDismiss = { showCatatanDriverDialog = false }
        )
    }

    if (showLaporanDialog) {
        AlertDialog(
            onDismissRequest = { showLaporanDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF0054A6))
                    Text("Laporan Catatan Armada", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Daftar catatan & keluhan driver per armada:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showCatatanDriverDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Catatan Driver", fontSize = 11.sp)
                        }
                    }
                    
                    Box(modifier = Modifier.heightIn(max = 380.dp)) {
                        if (armadaList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tidak ada data armada tersedia.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(armadaList) { armada ->
                                    val hasNote = armada.catattan.trim().isNotEmpty()
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (hasNote) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        border = if (hasNote) BorderStroke(1.dp, Color(0xFFFFB74D)) else null
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
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AirportShuttle,
                                                        contentDescription = null,
                                                        tint = if (hasNote) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = armada.armadaId,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (hasNote) Color.Black else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Surface(
                                                    color = if (hasNote) Color(0xFFFFE0B2) else Color(0xFFE8F5E9),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = if (hasNote) "⚠️ Ada Catatan" else "🟢 Bebas Keluhan",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (hasNote) Color(0xFFE65100) else Color(0xFF2E7D32),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                                thickness = 1.dp
                                            )
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.Top,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RateReview,
                                                    contentDescription = null,
                                                    tint = if (hasNote) Color(0xFFE65100) else MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                                )
                                                Text(
                                                    text = if (hasNote) armada.catattan else "Tidak ada keluhan/catatan driver untuk armada ini.",
                                                    style = if (hasNote) {
                                                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    } else {
                                                        MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                    },
                                                    color = if (hasNote) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }

                                            if (hasNote) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.clearCatatanArmada(armada.armadaId)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Tandai Selesai / Bersihkan", fontSize = 11.sp, color = Color(0xFF10B981))
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Assignment,
                                                        contentDescription = null,
                                                        tint = if (hasNote) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "KIR: ${armada.kirDate ?: "-"}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarToday,
                                                        contentDescription = null,
                                                        tint = if (hasNote) Color(0xFFE65100) else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Pajak: ${armada.pajakTahunan ?: "-"}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            },
            confirmButton = {
                TextButton(onClick = { showLaporanDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val dashboardBgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.background
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = dashboardBgBrush)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (dueAkiList.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "🚨 Notifikasi Aki Armada (${dueAkiList.size} Unit)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Usia aki mendekati/melebihi 2 tahun dari pemasangan (Sheet GID 1886867333)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        dueAkiList.forEach { due ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Unit ${due.armadaId} (${due.noPolisi})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pasang: ${due.tanggalPasang} • Ganti: ${due.tanggalGantiBerikutnya}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (due.isExpired) Color(0xFFDC2626) else Color(0xFFD97706),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = due.statusLabel.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Beautiful Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF0054A6), Color(0xFF1976D2))
                            )
                        )
                        .drawBehind {
                            // Draw elegant premium overlapping translucent circles/arcs for three-dimensional visual depth
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = this.size.height * 0.95f,
                                center = androidx.compose.ui.geometry.Offset(this.size.width * 0.82f, this.size.height * 0.25f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = this.size.height * 0.6f,
                                center = androidx.compose.ui.geometry.Offset(this.size.width * 0.92f, this.size.height * 0.75f)
                            )
                        }
                ) {
                    // Right illustration: Truck with fallback to newly generated hero asset
                    AsyncImage(
                        model = R.drawable.fleet_dashboard_hero_1786962305848,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(190.dp)
                            .graphicsLayer(alpha = 0.38f),
                        alignment = Alignment.CenterEnd
                    )

                    // Text Content left
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.68f)
                            .padding(22.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pantau kondisi armada dan catat aktivitas harian dengan mudah.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. Menu Utama Title & 4x2 Grid (All 8 menus)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Menu Utama",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Row 1: Status Armada & Log Harian
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuGridItem(
                        title = "Status Armada",
                        description = "Pantau status & kilometer armada",
                        icon = Icons.Default.LocalShipping,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFF0054A6),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("armada") }
                    )
                    MenuGridItem(
                        title = "Log Harian",
                        description = "Catat KM & aktivitas harian armada",
                        icon = Icons.Default.Speed,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("form") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Catat Servis & Catatan Driver
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuGridItem(
                        title = "Catat Servis",
                        description = "Input servis & penggantian part",
                        icon = Icons.Default.Build,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("service") }
                    )
                    MenuGridItem(
                        title = "Catatan Driver",
                        description = "Input & simpan keluhan driver",
                        icon = Icons.Default.RateReview,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { showCatatanDriverDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 3: Laporan & Pengaturan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuGridItem(
                        title = "Laporan",
                        description = "Lihat laporan & rekap data",
                        icon = Icons.Default.BarChart,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { showLaporanDialog = true }
                    )
                    MenuGridItem(
                        title = "Pengaturan",
                        description = "Kelola Google Sheet & API Key",
                        icon = Icons.Default.Settings,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFF475569),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("settings") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 4: Pengajuan & Arsip Pengiriman
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuGridItem(
                        title = "Pengajuan Ban/Aks",
                        description = "Pengajuan ban & aksesoris armada",
                        icon = Icons.Default.ShoppingCart,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFFE11D48),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("pengajuan") }
                    )
                    MenuGridItem(
                        title = "Arsip Pengiriman",
                        description = "Simpan arsip bukti kirim",
                        icon = Icons.Default.CloudUpload,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFF0891B2),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToScreen("arsip_pengiriman") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 5: Keluar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MenuGridItem(
                        title = "Keluar",
                        description = "Keluar dari sesi driver",
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        iconColor = Color.White,
                        circleBgColor = Color(0xFF991B1B),
                        modifier = Modifier.weight(1f),
                        onClick = { onLogoutClick() }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RingkasanCard(
    count: Int,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color
) {
    Card(
        modifier = Modifier
            .width(106.dp)
            .height(118.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = iconColor
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MenuGridItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    circleBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(124.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, circleBgColor.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duo-Tone Soft Squircle Icon Container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = circleBgColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = circleBgColor.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = circleBgColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
fun CatatanDriverDialog(
    viewModel: FleetViewModel,
    driverName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    var selectedArmadaId by remember { mutableStateOf(armadaList.firstOrNull()?.armadaId ?: "") }
    var catatanText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(selectedArmadaId, armadaList) {
        if (selectedArmadaId.isEmpty() && armadaList.isNotEmpty()) {
            selectedArmadaId = armadaList.first().armadaId
        }
        val selectedArmada = armadaList.find { it.armadaId == selectedArmadaId }
        catatanText = selectedArmada?.catattan ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RateReview,
                    contentDescription = null,
                    tint = Color(0xFFE65100)
                )
                Text(
                    text = "Catatan Driver",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Driver dapat memilih armada dan mencatat keluhan/catatan yang akan tersimpan ke Google Sheets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Select Armada
                Text(
                    text = "Pilih Armada:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(armadaList) { armada ->
                        FilterChip(
                            selected = (selectedArmadaId == armada.armadaId),
                            onClick = { selectedArmadaId = armada.armadaId },
                            label = {
                                Text("${armada.armadaId} (${armada.noPolisi})")
                            },
                            leadingIcon = if (selectedArmadaId == armada.armadaId) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE65100),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }

                // Driver & Date Info
                Surface(
                    color = Color(0xFFE65100).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Driver: ${driverName.ifEmpty { "Driver" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val currentDate = remember {
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                        }
                        Text(
                            text = "Waktu Input: $currentDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Row with Label & Append Action
                val currentDate = remember {
                    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keluhan/Catatan Aktif:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    TextButton(
                        onClick = {
                            val prefix = "• [$currentDate - ${driverName.ifEmpty { "Driver" }}]: "
                            catatanText = if (catatanText.trim().isEmpty()) {
                                prefix
                            } else {
                                "${catatanText.trim()}\n$prefix"
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Baris Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Catatan Input
                OutlinedTextField(
                    value = catatanText,
                    onValueChange = { catatanText = it },
                    label = { Text("Isi Catatan / Keluhan Driver") },
                    placeholder = { Text("Contoh: Rem bunyi berdecit, AC kurang dingin, dsb...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE65100),
                        focusedLabelColor = Color(0xFFE65100)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedArmadaId.isNotEmpty() && catatanText.trim().isNotEmpty()) {
                        isSubmitting = true
                        viewModel.submitCatatanDriver(
                            armadaId = selectedArmadaId,
                            driverName = driverName,
                            catatan = catatanText.trim()
                        ) {
                            isSubmitting = false
                            android.widget.Toast.makeText(
                                context,
                                "Catatan driver untuk armada $selectedArmadaId berhasil disimpan!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            onDismiss()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Pilih armada dan isi catatan terlebih dahulu.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSubmitting && selectedArmadaId.isNotEmpty() && catatanText.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Simpan Catatan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}






