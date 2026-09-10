package com.example.ui

import com.example.data.*
import com.example.ui.components.InAppUpdateDialog
import com.example.ui.screens.*
import com.example.utils.ImageCompressor
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.example.R
import com.example.data.ArmadaEntity
import com.example.data.LogHarianEntity
import com.example.data.BanEntity
import com.example.utils.CommonUtils
import androidx.compose.foundation.verticalScroll

fun getDirectDriveImageUrl(url: String): String = CommonUtils.getDirectDriveImageUrl(url)

fun downloadImage(context: Context, imageUrl: String) = CommonUtils.downloadImage(context, imageUrl)

@Composable
fun FullScreenImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Full Screen Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "Foto Odometer Ukuran Penuh",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )

            // Header controls (Back / Close and Download)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup"
                    )
                }

                // Download/Save button
                IconButton(
                    onClick = {
                        downloadImage(context, imageUrl)
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Unduh Foto"
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(viewModel: FleetViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    val loggedInDriver by viewModel.loggedInDriverName.collectAsStateWithLifecycle()
    val isSheetsMode by viewModel.isGoogleSheetsMode.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main App Content Layer
            if (loggedInDriver.isEmpty()) {
                LoginScreen(viewModel = viewModel)
            } else {
                MainAppScaffold(viewModel = viewModel, driverName = loggedInDriver)
            }

            // In-App Direct APK Updater Dialog
            InAppUpdateDialog(
                updateState = updateState,
                onUpdateClick = { apkUrl ->
                    viewModel.downloadAndInstallApk(apkUrl)
                },
                onDismiss = {
                    viewModel.dismissUpdateDialog()
                }
            )

            // Cinematic Splash Screen Overlay with Smooth Fade-Out
            androidx.compose.animation.AnimatedVisibility(
                visible = showSplash,
                enter = androidx.compose.animation.EnterTransition.None,
                exit = androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 400)
                )
            ) {
                SplashScreen(onTimeout = { showSplash = false })
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: FleetViewModel, driverName: String) {
    val context = LocalContext.current
    var activeScreen by remember { mutableStateOf("dashboard") }
    var showExitDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    BackHandler(enabled = true) {
        if (activeScreen != "dashboard") {
            activeScreen = "dashboard"
        } else {
            showExitDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            if (activeScreen == "dashboard") {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Circular Theme Logo (No longer hidden, settings are directly on dashboard)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "HUB Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "HUB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        lineHeight = 7.sp
                                    )
                                }
                            }

                            // Text titles
                            Column {
                                Text(
                                    "HUB KEDIRI",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "Driver: ${driverName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Settings Button (Directly visible)
                        IconButton(
                            onClick = { activeScreen = "settings" },
                            modifier = Modifier.testTag("settings_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Notifications with Red Dot
                        IconButton(onClick = { /* No-op notifications */ }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifikasi",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, shape = CircleShape)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.testTag("logout_action")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                val screenTitle = when (activeScreen) {
                    "form" -> "Log Harian"
                    "armada" -> "Status Armada"
                    "service" -> "Catat Servis"
                    "settings" -> "Pengaturan"
                    "arsip_pengiriman" -> "Arsip Pengiriman"
                    "pengajuan" -> "Pengajuan (Ban & Aksesoris)"
                    else -> ""
                }
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { activeScreen = "dashboard" },
                            modifier = Modifier.testTag("back_to_dashboard")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Beranda",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeScreen) {
                "dashboard" -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        driverName = driverName,
                        onNavigateToScreen = { screen -> activeScreen = screen },
                        onLogoutClick = { showLogoutDialog = true }
                    )
                }
                "form" -> FormScreen(viewModel = viewModel)
                "armada" -> HistoryScreen(viewModel = viewModel)
                "service" -> ServiceScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
                "arsip_pengiriman" -> ArsipPengirimanScreen(viewModel = viewModel, driverName = driverName)
                "pengajuan" -> PengajuanScreen(viewModel = viewModel, driverName = driverName)
            }

            // Global dialogs
            val successData by viewModel.submitSuccessData.collectAsStateWithLifecycle()
            successData?.let { data ->
                SuccessDialog(
                    data = data,
                    onDismiss = { 
                        viewModel.dismissSuccessDialog()
                        activeScreen = "dashboard" // Back to dashboard upon success!
                    }
                )
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = {
                        Text(
                            text = "Keluar Aplikasi",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "Apakah Anda yakin ingin keluar dari aplikasi HUB KEDIRI?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                val activity = context as? android.app.Activity
                                activity?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Ya, Keluar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = {
                        Text(
                            text = "Keluar Sesi",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Text(
                            text = "Apakah Anda yakin ingin keluar dari sesi driver ${driverName.uppercase()}?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                viewModel.logout {}
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Ya, Keluar Sesi")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}

