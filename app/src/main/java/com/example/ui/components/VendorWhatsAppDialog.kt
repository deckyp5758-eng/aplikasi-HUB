package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.VendorPicApiItem
import com.example.utils.CommonUtils

@Composable
fun VendorWhatsAppDialog(
    armadaId: String,
    noPolisi: String,
    kmSaatIni: Int,
    sisaKm: Int,
    kmBatasServis: Int = 0,
    initialKeluhan: String = "",
    fotoOdometerUrl: String? = null,
    fotoGantunganUrl: String? = null,
    isAcService: Boolean = false,
    vendor: VendorPicApiItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var keluhanInput by remember {
        val cleanInitial = initialKeluhan.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) "" else it
        }
        mutableStateOf(cleanInitial)
    }

    var nomorWhatsappInput by remember {
        mutableStateOf(vendor.noWhatsapp.ifBlank { "6281234567890" })
    }

    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    val generatedMessage = remember(
        vendor.namaPic,
        vendor.namaVendor,
        armadaId,
        noPolisi,
        kmSaatIni,
        sisaKm,
        kmBatasServis,
        keluhanInput,
        fotoOdometerUrl,
        fotoGantunganUrl,
        isAcService
    ) {
        buildVendorWhatsAppMessage(
            vendorPic = vendor.namaPic,
            vendorName = vendor.namaVendor,
            armadaId = armadaId,
            noPolisi = noPolisi,
            kmSaatIni = kmSaatIni,
            sisaKm = sisaKm,
            kmBatasServis = kmBatasServis,
            keluhan = keluhanInput,
            fotoOdoUrl = fotoOdometerUrl,
            fotoGantunganUrl = fotoGantunganUrl,
            isAcService = isAcService
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("vendor_whatsapp_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header Banner WhatsApp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF128C7E), Color(0xFF25D366))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_whatsapp),
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isAcService) "Booking Service AC" else "Booking Servis Vendor",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Unit $armadaId • $noPolisi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. Scrollable Content Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card Info Vendor Tujuan
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF0FDF4)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TUJUAN VENDOR BENGKEL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF15803D)
                                )
                                Surface(
                                    color = Color(0xFF22C55E),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = vendor.kategori,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = vendor.namaVendor.ifBlank { if (isAcService) "Prima AC Kediri" else "Bengkel Rekanan" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF14532D)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PIC: ${vendor.namaPic.ifBlank { "Service Advisor" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF166534)
                                )
                            }

                            if (vendor.alamatBengkel.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = vendor.alamatBengkel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF166534).copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Field Nomor WhatsApp PIC (Bisa disesuaikan jika perlu)
                    OutlinedTextField(
                        value = nomorWhatsappInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                                nomorWhatsappInput = input
                            }
                        },
                        label = { Text("Nomor WhatsApp PIC") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF128C7E)
                            )
                        },
                        trailingIcon = {
                            if (nomorWhatsappInput != vendor.noWhatsapp && vendor.noWhatsapp.isNotBlank()) {
                                IconButton(onClick = { nomorWhatsappInput = vendor.noWhatsapp }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Nomor Asli")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wa_vendor_phone_field")
                    )

                    // Ringkasan Odometer & Sisa KM
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "KM Saat Ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${CommonUtils.formatKm(kmSaatIni)} KM",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Column {
                                Text(
                                    text = "Sisa KM Servis",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${CommonUtils.formatKm(sisaKm)} KM",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (sisaKm < 1000) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    )
                                )
                            }
                            if (kmBatasServis > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Target Servis",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${CommonUtils.formatKm(kmBatasServis)} KM",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // Field Keluhan Driver (Bisa diedit/ditambah)
                    OutlinedTextField(
                        value = keluhanInput,
                        onValueChange = { keluhanInput = it },
                        label = { Text("Keluhan Driver / Keterangan Servis") },
                        placeholder = { Text("Contoh: Rem bunyi decit, tarikan gigi 2 berat, ganti oli & filter") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wa_vendor_keluhan_input")
                    )

                    // Pratinjau Foto Odometer & Gantungan Servis
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Dokumentasi Foto Terlampir:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Thumbnail Odometer
                            ThumbnailPhotoCard(
                                title = "Foto Odometer",
                                photoUrl = fotoOdometerUrl,
                                modifier = Modifier.weight(1f),
                                onImageClick = { url -> fullScreenPhotoUrl = url }
                            )

                            // 2. Thumbnail Gantungan Service
                            ThumbnailPhotoCard(
                                title = "Gantungan Servis",
                                photoUrl = fotoGantunganUrl,
                                modifier = Modifier.weight(1f),
                                onImageClick = { url -> fullScreenPhotoUrl = url }
                            )
                        }
                    }

                    // Pratinjau Teks Pesan WhatsApp
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = Color(0xFF128C7E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Pratinjau Pesan WhatsApp",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF128C7E)
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Pesan Servis WhatsApp", generatedMessage)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Teks pesan berhasil disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salin Teks", fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = generatedMessage,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 3. Bottom Action Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.35f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                launchWhatsAppChat(
                                    context = context,
                                    rawPhone = nomorWhatsappInput,
                                    message = generatedMessage
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(0.65f)
                                .height(48.dp)
                                .testTag("btn_launch_whatsapp_vendor"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366)
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_whatsapp),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buka Chat WhatsApp",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Fullscreen Photo Preview
    fullScreenPhotoUrl?.let { url ->
        Dialog(onDismissRequest = { fullScreenPhotoUrl = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Fullscreen Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { fullScreenPhotoUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailPhotoCard(
    title: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!photoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onImageClick(photoUrl) }
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 4.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "Lihat",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HideImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Belum Ada",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Membangun format teks pesan WhatsApp booking servis yang rapi dan profesional.
 */
fun buildVendorWhatsAppMessage(
    vendorPic: String,
    vendorName: String,
    armadaId: String,
    noPolisi: String,
    kmSaatIni: Int,
    sisaKm: Int,
    kmBatasServis: Int,
    keluhan: String,
    fotoOdoUrl: String?,
    fotoGantunganUrl: String?,
    isAcService: Boolean
): String {
    val picSalutation = if (vendorPic.isNotBlank()) "Yth. $vendorPic" else "Halo Rekan"
    val vendorLabel = if (vendorName.isNotBlank()) " ($vendorName)" else ""
    val jenisServis = if (isAcService) "SERVIS AC" else "SERVIS BERKALA"

    return buildString {
        appendLine("$picSalutation$vendorLabel,")
        appendLine()
        appendLine("Mohon pengajuan booking jadwal *$jenisServis* untuk armada operasional kami:")
        appendLine("🚛 *Unit Armada:* $armadaId ($noPolisi)")
        appendLine("📊 *KM Saat Ini:* ${CommonUtils.formatKm(kmSaatIni)} KM")
        appendLine("⚠️ *Sisa KM Servis:* ${CommonUtils.formatKm(sisaKm)} KM")
        if (kmBatasServis > 0) {
            appendLine("🎯 *Target Batas Servis:* ${CommonUtils.formatKm(kmBatasServis)} KM")
        }
        appendLine()
        appendLine("📝 *Keluhan / Catatan Driver Terakhir:*")
        appendLine(if (keluhan.isNotBlank()) keluhan.trim() else "Tidak ada keluhan khusus (Rutin)")
        appendLine()
        if (!fotoOdoUrl.isNullOrBlank()) {
            appendLine("📸 *Foto Odometer Terakhir:*")
            appendLine(fotoOdoUrl)
            appendLine()
        }
        if (!fotoGantunganUrl.isNullOrBlank()) {
            appendLine("🏷️ *Foto Gantungan Servis:*")
            appendLine(fotoGantunganUrl)
            appendLine()
        }
        appendLine("_Mohon konfirmasi ketersediaan jadwal servis. Terima kasih._")
        appendLine("*Tim Operasional HUB Kediri*")
    }
}

/**
 * Meluncurkan WhatsApp Intent secara andal dengan fallback browser jika app tidak terpasang.
 */
fun launchWhatsAppChat(context: Context, rawPhone: String, message: String) {
    val cleanPhone = rawPhone.replace(Regex("[^0-9]"), "").let {
        if (it.startsWith("0")) "62" + it.substring(1) else it
    }

    if (cleanPhone.isBlank()) {
        Toast.makeText(context, "Nomor WhatsApp vendor tidak valid!", Toast.LENGTH_SHORT).show()
        return
    }

    val encodedText = Uri.encode(message)
    val whatsappAppUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText")

    val intent = Intent(Intent.ACTION_VIEW, whatsappAppUri).apply {
        setPackage("com.whatsapp")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback 1: Coba WhatsApp Business
        val businessIntent = Intent(Intent.ACTION_VIEW, whatsappAppUri).apply {
            setPackage("com.whatsapp.w4b")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(businessIntent)
        } catch (_: Exception) {
            // Fallback 2: Buka via web browser umum
            val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone?text=$encodedText")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(genericIntent)
            } catch (errGeneric: Exception) {
                Toast.makeText(context, "Gagal membuka WhatsApp: ${errGeneric.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun DaftarKontakVendorDialog(
    vendorList: List<VendorPicApiItem>,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("dialog_daftar_kontak_vendor"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0054A6), Color(0xFF1E88E5))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_whatsapp),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Daftar PIC Vendor Servis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Terhubung Sheet: KONTAK_VENDOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Body List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Info Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FF)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF90CDF4))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2B6CB0),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Data PIC di bawah disinkronkan langsung dari Google Spreadsheet tab 'KONTAK_VENDOR'. Saat ada pergantian PIC, admin cukup mengedit di Google Sheet.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Color(0xFF2C5282)
                            )
                        }
                    }

                    val displayVendors = if (vendorList.isNotEmpty()) vendorList else listOf(
                        VendorPicApiItem(
                            idVendor = "VND-HINO",
                            namaVendor = "Bengkel Resmi Hino Motors Kediri",
                            kategori = "SERVIS MESIN",
                            armadaHandle = "HK01, HK02, HK04",
                            namaPic = "Service Advisor Hino",
                            noWhatsapp = "6281234567890",
                            alamatBengkel = "Jl. Mayor Bismo, Kediri"
                        ),
                        VendorPicApiItem(
                            idVendor = "VND-DAIHATSU",
                            namaVendor = "Bengkel Resmi Daihatsu Kediri",
                            kategori = "SERVIS MESIN",
                            armadaHandle = "HK03",
                            namaPic = "Service Advisor Daihatsu",
                            noWhatsapp = "6281234567891",
                            alamatBengkel = "Jl. Joyoboyo, Kediri"
                        ),
                        VendorPicApiItem(
                            idVendor = "VND-PRIMA-AC",
                            namaVendor = "Prima AC Mobil Kediri",
                            kategori = "SERVIS AC",
                            armadaHandle = "ALL",
                            namaPic = "PIC Prima AC",
                            noWhatsapp = "6281234567892",
                            alamatBengkel = "Jl. Kilisuci, Kediri"
                        )
                    )

                    displayVendors.forEach { v ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = v.namaVendor,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val isAc = v.kategori.contains("AC", ignoreCase = true)
                                    Surface(
                                        color = if (isAc) Color(0xFFE0F2FE) else Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = v.kategori,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            color = if (isAc) Color(0xFF0369A1) else Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Unit: ${v.armadaHandle}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "• PIC: ${v.namaPic}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (v.alamatBengkel.isNotBlank()) {
                                    Text(
                                        text = "📍 ${v.alamatBengkel}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WA: ${v.noWhatsapp.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF128C7E)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val clip = ClipData.newPlainText("No WA", v.noWhatsapp)
                                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                                    .setPrimaryClip(clip)
                                                Toast.makeText(context, "Nomor ${v.namaPic} disalin!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Salin",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Salin", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val greeting = "Halo ${v.namaPic} (${v.namaVendor}), saya dari tim operasional HUB Kediri."
                                                launchWhatsAppChat(context, v.noWhatsapp, greeting)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_whatsapp),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chat WA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isRefreshing = true
                                onRefresh()
                                Toast.makeText(context, "Memperbarui kontak dari Google Sheet...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sinkronkan",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sinkronkan Sheet", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0054A6))
                        ) {
                            Text("Tutup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
