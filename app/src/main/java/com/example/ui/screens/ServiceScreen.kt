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

    // Odometer / Service state
    var showConfirmNotice by remember { mutableStateOf(false) }

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
                        label = { Text("KM Saat Servis Dilakukan (Wajib)") },
                        placeholder = { Text("e.g. 52000") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = "Speedometer Icon") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_km_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Kalkulasi Otomatis KM Servis Berikutnya (+10.000 KM)
                    val kmInputNumber = kmServisInput.toIntOrNull()
                    val matchingArmada = armadaList.find { it.armadaId == selectedArmadaId }
                    if (kmInputNumber != null && kmInputNumber > 0) {
                        val nextServiceTarget = kmInputNumber + 10000
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Target Servis Berikutnya: ${CommonUtils.formatKm(nextServiceTarget)} KM",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    "Otomatis disetel +10.000 KM dari KM servis yang diinput (${CommonUtils.formatKm(kmInputNumber)} KM).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                if (matchingArmada != null && kmInputNumber < matchingArmada.kmSaatIni) {
                                    Text(
                                        "ℹ️ KM servis dicatat di ${CommonUtils.formatKm(kmInputNumber)} KM (Odometer unit: ${CommonUtils.formatKm(matchingArmada.kmSaatIni)} KM).",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

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

                            viewModel.submitServiceLog(
                                armadaId = selectedArmadaId,
                                kmServis = kmVal,
                                catatan = catatanInput.ifBlank { null },
                                allowLowerKm = true,
                                correctionReason = "Update Servis Berkala (+10.000 KM)"
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyimpan & Menyetel Jadwal Servis...")
                        } else {
                            Icon(Icons.Default.Build, contentDescription = "Save Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Data Servis (+10.000 KM)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


