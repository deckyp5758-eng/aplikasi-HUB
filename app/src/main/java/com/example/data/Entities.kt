package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver")
data class DriverEntity(
    @PrimaryKey val idDriver: String,
    val namaDriver: String,
    val pin: String
)

@Entity(tableName = "armada")
data class ArmadaEntity(
    @PrimaryKey val armadaId: String,
    val noPolisi: String,
    val kmSaatIni: Int,
    val kmServiceTerakhir: Int,
    val intervalService: Int,
    val kmServiceBerikutnya: Int,
    val sisaKm: Int,
    val status: String,
    val flag: String,
    val fotoKm: String,
    val catattan: String,
    val pajakTahunan: String? = null,
    val kirDate: String? = null,
    val pajak5Tahunan: String? = null,
    val fotoTruck: String? = null,
    val fotoService: String? = null
)

@Entity(tableName = "log_harian")
data class LogHarianEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: String,
    val armadaId: String,
    val kmTerdeteksi: Int,
    val linkFoto: String,
    val catatan: String,
    val namaDriver: String
)

@Entity(tableName = "ban_armada")
data class BanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val armadaId: String,
    val noPolisi: String,
    val posisi: String,
    val noSeri: String,
    val ukuran: String,
    val merk: String,
    val kondisi: String,
    val tekanan: String,
    val keterangan: String,
    val barcode: String? = null,
    val tahun: String? = null,
    val kodeBan: String? = null,
    val tanggalUpdate: String? = null
)

@Entity(tableName = "pengiriman")
data class PengirimanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noDokumen: String = "",
    val noSuratJalan: String = "",
    val tanggal: String = "",
    val driver: String = "",
    val driver1: String = "",
    val driver2: String = "",
    val armada: String = "",
    val gudangAsal: String = "",
    val tujuan: String = "",
    val alamat: String = "",
    val remarks: String = "",
    val penerima: String = "",
    val noTelpCustomer: String = "",
    val jumlahKoli: Int = 1,
    val volumeCbm: Double = 0.0,
    val status: String = "Belum Berangkat", // "Terkirim", "Dalam Perjalanan", "Belum Berangkat"
    val catatan: String = ""
)

@Entity(tableName = "catatan_driver")
data class CatatanDriverEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: String,
    val armadaId: String,
    val driverName: String,
    val catatan: String,
    val status: String = "Aktif"
)

@Entity(tableName = "pengajuan")
data class PengajuanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noPengajuan: String = "",
    val tanggal: String = "",
    val driver: String = "",
    val armadaId: String = "",
    val noPolisi: String = "",
    val kategori: String = "", // "Aksesoris" or "Ban"
    val detail: String = "",
    val catatan: String = "",
    val foto1Url: String = "",
    val foto2Url: String = "",
    val foto3Url: String = "",
    val foto4Url: String = "",
    val fotoLainnyaUrls: String = "",
    val status: String = "PENDING"
)

data class SubmitSuccessData(
    val armadaId: String,
    val sisaKm: Int,
    val serviceAlert: Boolean = false,
    val linkFoto: String? = null
)

data class ChatMediaItem(
    val title: String,
    val description: String,
    val urlOrPath: String,
    val type: String = "IMAGE", // "IMAGE", "VIDEO", "FILE"
    val source: String = "Pengiriman" // "Pengiriman", "Odometer", "Armada"
)

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: String = "",
    val mediaItems: List<ChatMediaItem> = emptyList()
)

