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

@Composable
fun LoginScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.selectedDriverName.collectAsStateWithLifecycle()
    val pin by viewModel.pinInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.loginLoading.collectAsStateWithLifecycle()
    val error by viewModel.loginError.collectAsStateWithLifecycle()
    val isSheetsMode by viewModel.isGoogleSheetsMode.collectAsStateWithLifecycle()
    val urlInput by viewModel.appsScriptUrl.collectAsStateWithLifecycle()
    val googleSheetId by viewModel.googleSheetId.collectAsStateWithLifecycle()

    var urlTextState by remember { mutableStateOf(urlInput) }
    var sheetIdTextState by remember { mutableStateOf(googleSheetId) }
    LaunchedEffect(urlInput) {
        urlTextState = urlInput
    }
    LaunchedEffect(googleSheetId) {
        sheetIdTextState = googleSheetId
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    val loginBgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = loginBgBrush)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Beautiful Fleet Hero Banner
                AsyncImage(
                    model = R.drawable.img_fleet_banner,
                    contentDescription = "Fleet Hero Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "HO33",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Text(
                    text = "HUB KEDIRI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Fleet Odo Tracker",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Pencatatan KM Harian Armada Driver",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Log Masuk Driver",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // ID Driver Input
                        OutlinedTextField(
                            value = selectedDriver,
                            onValueChange = { viewModel.setSelectedDriver(it) },
                            label = { Text("ID Driver") },
                            placeholder = { Text("Contoh: D01") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "ID Driver Icon") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_id_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // PIN Input
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { viewModel.setPin(it) },
                            label = { Text("PIN Keamanan") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "PIN Icon") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.login { }
                                }
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        error?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Button
                        Button(
                            onClick = {
                                viewModel.login {
                                    // Callback handled inside
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_button"),
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
                                Text(
                                    "Log Masuk",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Removed settings toggle button to prevent access to credentials
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

@Composable
fun FormScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    val selectedArmadaId by viewModel.selectedArmadaId.collectAsStateWithLifecycle()
    val kmInput by viewModel.kmInput.collectAsStateWithLifecycle()
    val catatanInput by viewModel.catatanInput.collectAsStateWithLifecycle()
    val selectedPhoto by viewModel.selectedPhoto.collectAsStateWithLifecycle()
    val isLoading by viewModel.submitLoading.collectAsStateWithLifecycle()
    val error by viewModel.submitError.collectAsStateWithLifecycle()
    val ocrLoading by viewModel.ocrLoading.collectAsStateWithLifecycle()
    val ocrSuccessMessage by viewModel.ocrSuccessMessage.collectAsStateWithLifecycle()

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
    }
}

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
    val serviceImgUrl = if (!armada.fotoService.isNullOrEmpty()) {
        getDirectDriveImageUrl(armada.fotoService)
    } else null
    
    val formatKm = { num: Int ->
        String.format("%,d", num).replace(',', '.')
    }

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
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Dokumentasi Foto Armada",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // 1. Foto Profil (Kolom L)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Profil Armada (L)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                                    .clickable { if (!imgUrl.isNullOrEmpty()) fullScreenImageUrl = imgUrl },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = imgUrl,
                                                    contentDescription = "Foto Profil Armada",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                if (isFotoTruckUploading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { imageLauncher.launch("image/*") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 36.dp),
                                                enabled = !isFotoTruckUploading,
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ganti Profil", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                            }
                                            fotoTruckUploadStatus?.let { status ->
                                                Text(
                                                    text = status,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = if (status.startsWith("Error") || status.startsWith("Gagal")) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                                )
                                            }
                                        }

                                        // 2. Foto Gantungan Service (Kolom M)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Gantungan Service (M)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                                    .clickable { if (!serviceImgUrl.isNullOrEmpty()) fullScreenImageUrl = serviceImgUrl },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!serviceImgUrl.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = serviceImgUrl,
                                                        contentDescription = "Foto Gantungan Service",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.padding(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AddPhotoAlternate,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        Text(
                                                            text = "Belum Ada Foto",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }

                                                if (isFotoServiceUploading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.4f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { serviceImageLauncher.launch("image/*") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 36.dp),
                                                enabled = !isFotoServiceUploading,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (serviceImgUrl.isNullOrEmpty()) "Upload Service" else "Ganti Service", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                            }
                                            fotoServiceUploadStatus?.let { status ->
                                                Text(
                                                    text = status,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = if (status.startsWith("Error") || status.startsWith("Gagal")) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                                )
                                            }
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
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Riwayat Log ${armada.armadaId}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
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
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        armadaLogs.forEach { log ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(log.tanggal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("Driver: ${log.namaDriver}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                                        }
                                                        Text(
                                                            "${formatKm(log.kmTerdeteksi)} KM",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    if (log.catatan.isNotEmpty()) {
                                                        Text(
                                                            "Catatan: ${log.catatan}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (log.linkFoto.isNotEmpty() && log.linkFoto.startsWith("http")) {
                                                        val directUrl = getDirectDriveImageUrl(log.linkFoto)
                                                        AsyncImage(
                                                            model = directUrl,
                                                            contentDescription = "Odometer Odo Photo",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(180.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable { fullScreenImageUrl = directUrl },
                                                            contentScale = ContentScale.Crop
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

@Composable
fun SuccessDialog(data: SubmitSuccessData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (data.serviceAlert) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = "Alert Icon",
                    tint = if (data.serviceAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Kirim Data Berhasil!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Log KM Harian armada ${data.armadaId} berhasil disimpan ke database.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Service Threshold alert triggers warning dialog if sisa KM < 1000 KM
                if (data.serviceAlert) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "⚠️ PERINGATAN SERVIS!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Sisa KM armada ini adalah ${data.sisaKm} KM. Harap segera laporkan ke Admin untuk dijadwalkan servis!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val syncChecking by viewModel.syncChecking.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val syncSuccess by viewModel.syncSuccess.collectAsStateWithLifecycle()
    val isSheetsMode by viewModel.isGoogleSheetsMode.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()

    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(true)
            android.widget.Toast.makeText(context, "Izin notifikasi diberikan!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setNotificationsEnabled(false)
            android.widget.Toast.makeText(context, "Izin notifikasi ditolak.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Pengaturan Aplikasi",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Skema Warna (Tema Terang / Gelap) Section
        item {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            "Tema Aplikasi (Skema Warna)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Membantu meningkatkan kenyamanan visual di berbagai kondisi pencahayaan jalan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            Triple("light", "Terang", Icons.Default.LightMode),
                            Triple("dark", "Gelap", Icons.Default.DarkMode),
                            Triple("system", "Sistem", Icons.Default.SettingsSuggest)
                        )

                        options.forEach { (mode, label, icon) ->
                            val isSelected = themeMode == mode
                            val containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            val borderColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }

                            Surface(
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = containerColor,
                                contentColor = contentColor,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("theme_btn_$mode")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // System Notifications Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notifikasi Sistem",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Kirim notifikasi lokal saat entri odometer atau servis berhasil disimpan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setNotificationsEnabled(true)
                                        }
                                    } else {
                                        viewModel.setNotificationsEnabled(true)
                                    }
                                } else {
                                    viewModel.setNotificationsEnabled(false)
                                }
                            },
                            modifier = Modifier.testTag("notifications_switch")
                        )
                    }

                    // Background 08:00 AM Daily Scheduler Info
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Pengingat Otomatis Jam 08:00 Pagi",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Text(
                                "Fitur latar belakang (WorkManager) otomatis aktif setiap pukul 08:00 WIB untuk mengecek jatuh tempo KIR, Pajak STNK, dan KM servis meskipun aplikasi tertutup total.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.scheduleDaily8AmReminder() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20))
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Jadwalkan 08:00", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { viewModel.triggerBackgroundReminderNow() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tes Background", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = notificationsEnabled) {
                        Button(
                            onClick = { viewModel.triggerTestNotification() },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("test_notification_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Uji Coba")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uji Coba Notifikasi Langsung")
                        }
                    }
                }
            }
        }

        // Spreadsheet Synchronization Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Cloud Sync Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sinkronisasi Google Spreadsheet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Status: ${if (isSheetsMode) "Aktif" else "Nonaktif"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSheetsMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        "Data armada dan daftar pengemudi disinkronkan langsung dengan Google Spreadsheet di cloud untuk pelaporan real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = syncMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (syncSuccess) {
                                    true -> MaterialTheme.colorScheme.primaryContainer
                                    false -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (syncChecking) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (syncSuccess == true) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = "Status Icon",
                                            tint = if (syncSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = if (syncChecking) "Sedang memeriksa..." else if (syncSuccess == true) "Sukses Terhubung" else "Gagal Terhubung",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (syncSuccess == true) MaterialTheme.colorScheme.onPrimaryContainer else if (syncSuccess == false) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = syncMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (syncSuccess == true) MaterialTheme.colorScheme.onPrimaryContainer else if (syncSuccess == false) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.checkSyncStatus() },
                        enabled = !syncChecking,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("check_sync_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (syncChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Memeriksa...")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Icon")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Periksa Sinkronisasi")
                        }
                    }
                }
            }
        }

        // Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Informasi Aplikasi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Aplikasi: HUB KEDIRI Odometer Tracker",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Status Mode: ${if (isSheetsMode) "Online (Google Sheets Sync)" else "Offline (Lokal)"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val armadaList by viewModel.armadaList.collectAsStateWithLifecycle()
    val isSheetsMode by viewModel.isGoogleSheetsMode.collectAsStateWithLifecycle()

    val serviceLoading by viewModel.serviceLoading.collectAsStateWithLifecycle()
    val serviceSuccessMessage by viewModel.serviceSuccessMessage.collectAsStateWithLifecycle()
    val serviceErrorMessage by viewModel.serviceErrorMessage.collectAsStateWithLifecycle()

    var selectedArmadaId by remember { mutableStateOf("") }
    var kmServisInput by remember { mutableStateOf("") }
    var catatanInput by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(serviceSuccessMessage) {
        serviceSuccessMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            selectedArmadaId = ""
            kmServisInput = ""
            catatanInput = ""
            viewModel.clearServiceMessages()
        }
    }

    LaunchedEffect(serviceErrorMessage) {
        serviceErrorMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearServiceMessages()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Catat Servis Armada",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Form Input Servis Bengkel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Masukkan data kilometer dan catatan servis setelah armada selesai diservis di bengkel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // 1. Dropdown Pilihan Armada
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
                                .testTag("service_armada_dropdown_select"),
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
                                                        "KM Saat Ini: ${item.kmSaatIni}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    item.status,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (item.status.contains("AMAN")) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedArmadaId = item.armadaId
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. KM Saat Servis
                    OutlinedTextField(
                        value = kmServisInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                kmServisInput = input
                            }
                        },
                        label = { Text("KM Saat Servis (Wajib)") },
                        placeholder = { Text("e.g. 52000") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = "Speedometer Icon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_km_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // 3. Catatan Servis / Penggantian Part
                    OutlinedTextField(
                        value = catatanInput,
                        onValueChange = { catatanInput = it },
                        label = { Text("Catatan Servis / Penggantian Part (Opsional)") },
                        placeholder = { Text("e.g. Ganti oli mesin Shell Rimula, ganti filter oli") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = "Wrench Icon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_catatan_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Tombol: Simpan Data Servis
                    Button(
                        onClick = {
                            if (selectedArmadaId.isEmpty()) {
                                android.widget.Toast.makeText(context, "Pilih armada terlebih dahulu!", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val kmVal = kmServisInput.toIntOrNull()
                            if (kmVal == null || kmVal <= 0) {
                                android.widget.Toast.makeText(context, "KM Saat Servis wajib diisi dengan angka valid!", android.widget.Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val matchingArmada = armadaList.find { it.armadaId == selectedArmadaId }
                            if (matchingArmada != null && kmVal < matchingArmada.kmSaatIni) {
                                android.widget.Toast.makeText(context, "KM Servis tidak boleh kurang dari KM saat ini (${matchingArmada.kmSaatIni})!", android.widget.Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            viewModel.submitServiceLog(
                                armadaId = selectedArmadaId,
                                kmServis = kmVal,
                                catatan = catatanInput.ifBlank { null }
                            )
                        },
                        enabled = !serviceLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_service_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (serviceLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Build, contentDescription = "Save Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Data Servis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


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
