package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class FleetRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: PreferenceManager
) {
    companion object {
        const val SPREADSHEET_PENGIRIMAN = "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw"
        const val SPREADSHEET_PENGIRIMAN_LOG = "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw"
        const val SPREADSHEET_ARMADA = "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw"
        const val SPREADSHEET_AI_DATA = "1nCxvNqo7d0zRdLDAxWorFGOXOxfhr9S1x1man9O9xrw"

        const val GID_ARSIP_PENGIRIMAN = "1878433267"
        const val GID_LOG_HARIAN = "1263706817"
        const val GID_KIR_PAJAK = "2062052578"
        const val GID_AKI = "1886867333"
        const val GID_PENGAJUAN = "1517362778"
        const val GID_DAFTAR_DRIVER = "479314622"
        const val GID_ARMADA = "1850941825"
        const val GID_BAN = "817527065"
        const val GID_CATATAN_DRIVER = "1562754278"
        const val GID_AI_DATA = "888604592"
        private const val TAG = "FleetRepository"
    }

    val localDrivers: Flow<List<DriverEntity>> = db.driverDao().getAllDrivers()
    val localArmada: Flow<List<ArmadaEntity>> = db.armadaDao().getAllArmada()
    val localLogs: Flow<List<LogHarianEntity>> = db.logHarianDao().getAllLogs()
    val localBan: Flow<List<BanEntity>> = db.banDao().getAllBan()
    val localPengiriman: Flow<List<PengirimanEntity>> = db.pengirimanDao().getAllPengiriman()
    val localPengajuan: Flow<List<PengajuanEntity>> = db.pengajuanDao().getAllPengajuan()

    private var cachedKnowledgeList: List<AiKnowledgeApiItem>? = null
    private var knowledgeCacheTimestamp: Long = 0L
    private val KNOWLEDGE_CACHE_TTL = 300_000L // 5 minutes TTL

    suspend fun getAiKnowledgeList(forceRefresh: Boolean = false): List<AiKnowledgeApiItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedKnowledgeList != null && (now - knowledgeCacheTimestamp < KNOWLEDGE_CACHE_TTL)) {
            return cachedKnowledgeList!!
        }
        return if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.getAiKnowledge(
                    action = "getAiKnowledge",
                    spreadsheetId = SPREADSHEET_AI_DATA
                )
                if (response.success && response.data != null) {
                    cachedKnowledgeList = response.data
                    knowledgeCacheTimestamp = now
                    response.data
                } else {
                    cachedKnowledgeList ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("FleetRepository", "Failed to fetch AI knowledge: ${e.localizedMessage}", e)
                cachedKnowledgeList ?: emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun submitTerkirim(
        pengiriman: PengirimanEntity,
        catatanDriver: String,
        mediaFiles: List<TerkirimMediaFile>
    ): CommonWriteApiResponse {
        val updatedEntity = pengiriman.copy(
            status = "TERKIRIM",
            catatan = catatanDriver
        )
        db.pengirimanDao().updatePengiriman(updatedEntity)

        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val targetSheetId = if (prefs.googleSheetId.isNotEmpty()) prefs.googleSheetId else SPREADSHEET_PENGIRIMAN
                val response = service.submitTerkirim(
                    request = SubmitTerkirimApiRequest(
                        id = if (pengiriman.id > 0) pengiriman.id else null,
                        noDokumen = pengiriman.noDokumen.ifEmpty { pengiriman.noSuratJalan },
                        noSuratJalan = pengiriman.noSuratJalan.ifEmpty { pengiriman.noDokumen },
                        tanggal = pengiriman.tanggal,
                        driver = pengiriman.driver,
                        armada = pengiriman.armada,
                        alamat = if (pengiriman.alamat.isNotEmpty()) pengiriman.alamat else pengiriman.tujuan,
                        penerima = pengiriman.penerima,
                        noTelpCustomer = pengiriman.noTelpCustomer,
                        volumeCbm = pengiriman.volumeCbm,
                        catatan = catatanDriver,
                        files = mediaFiles,
                        spreadsheetId = targetSheetId,
                        sheetId = GID_ARSIP_PENGIRIMAN
                    ),
                    spreadsheetId = targetSheetId,
                    sheetId = GID_ARSIP_PENGIRIMAN
                )
                response
            } catch (e: Exception) {
                e.printStackTrace()
                CommonWriteApiResponse(success = false, message = e.localizedMessage ?: "Gagal terhubung ke Apps Script")
            }
        }
        return CommonWriteApiResponse(success = true, message = "Status terkirim disimpan secara lokal")
    }

    suspend fun submitArsipPengiriman(
        noDokumen: String,
        noReceive: String,
        driverName: String,
        mediaFiles: List<TerkirimMediaFile>
    ): CommonWriteApiResponse {
        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val targetSheetId = if (prefs.googleSheetId.isNotEmpty()) prefs.googleSheetId else SPREADSHEET_PENGIRIMAN_LOG
                
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                val nowStr = sdf.format(java.util.Date())
                
                val response = service.submitTerkirim(
                    request = SubmitTerkirimApiRequest(
                        action = "submitterkirim",
                        id = null,
                        noDokumen = noDokumen,
                        noSuratJalan = noReceive,
                        tanggal = nowStr,
                        driver = driverName,
                        armada = "",
                        alamat = "",
                        penerima = "",
                        noTelpCustomer = "",
                        volumeCbm = 0.0,
                        catatan = "",
                        files = mediaFiles,
                        spreadsheetId = targetSheetId,
                        sheetId = GID_ARSIP_PENGIRIMAN
                    ),
                    spreadsheetId = targetSheetId,
                    sheetId = GID_ARSIP_PENGIRIMAN
                )
                response
            } catch (e: Exception) {
                e.printStackTrace()
                CommonWriteApiResponse(success = false, message = e.localizedMessage ?: "Gagal terhubung ke Apps Script")
            }
        }
        return CommonWriteApiResponse(success = false, message = "Mode Google Sheets tidak aktif atau URL kosong.")
    }

    suspend fun insertPengiriman(pengiriman: PengirimanEntity) {
        db.pengirimanDao().insertPengiriman(pengiriman)
        
        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                service.addPengiriman(
                    request = AddPengirimanApiRequest(
                        noSuratJalan = pengiriman.noSuratJalan,
                        tanggal = pengiriman.tanggal,
                        driver = pengiriman.driver,
                        armada = pengiriman.armada,
                        gudangAsal = pengiriman.gudangAsal,
                        tujuan = pengiriman.tujuan,
                        jumlahKoli = pengiriman.jumlahKoli,
                        volumeCbm = pengiriman.volumeCbm,
                        status = pengiriman.status,
                        catatan = pengiriman.catatan
                    ),
                    spreadsheetId = SPREADSHEET_PENGIRIMAN
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updatePengiriman(pengiriman: PengirimanEntity) {
        db.pengirimanDao().updatePengiriman(pengiriman)
        
        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                service.updatePengiriman(
                    request = UpdatePengirimanApiRequest(
                        id = pengiriman.id,
                        noSuratJalan = pengiriman.noSuratJalan,
                        tanggal = pengiriman.tanggal,
                        driver = pengiriman.driver,
                        armada = pengiriman.armada,
                        gudangAsal = pengiriman.gudangAsal,
                        tujuan = pengiriman.tujuan,
                        jumlahKoli = pengiriman.jumlahKoli,
                        volumeCbm = pengiriman.volumeCbm,
                        status = pengiriman.status,
                        catatan = pengiriman.catatan
                    ),
                    spreadsheetId = SPREADSHEET_PENGIRIMAN
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deletePengirimanById(id: Int) {
        db.pengirimanDao().deletePengirimanById(id)
        
        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                service.deletePengiriman(
                    request = DeletePengirimanApiRequest(id = id),
                    spreadsheetId = SPREADSHEET_PENGIRIMAN
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private suspend fun getDriverDefaults(): List<DriverEntity> {
        val list = db.driverDao().getAllDrivers().first()
        return if (list.isEmpty()) {
            val defaults = listOf(
                DriverEntity("D01", "Driver HUB 1", "1234"),
                DriverEntity("D02", "Driver HUB 2", "5678")
            )
            db.driverDao().insertDrivers(defaults)
            defaults
        } else {
            list
        }
    }

    suspend fun getDrivers(): List<DriverEntity> {
        return if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.getDrivers(spreadsheetId = SPREADSHEET_ARMADA, sheetId = GID_DAFTAR_DRIVER)
                if (response.success && response.drivers != null && response.drivers.isNotEmpty()) {
                    val entities = response.drivers.map {
                        DriverEntity(
                            idDriver = it.id,
                            namaDriver = it.name,
                            pin = it.pin?.takeIf { p -> p.isNotBlank() } ?: "1234"
                        )
                    }
                    db.driverDao().deleteAllDrivers() // Hapus total cache driver lama agar bersih
                    db.driverDao().insertDrivers(entities)
                    db.driverDao().getAllDrivers().first()
                } else {
                    db.driverDao().getAllDrivers().first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                db.driverDao().getAllDrivers().first()
            }
        } else {
            db.driverDao().getAllDrivers().first()
        }
    }

    suspend fun validateLogin(driverIdOrName: String, pin: String): LoginResult {
        val cleanDriverId = driverIdOrName.trim()
        val cleanPin = pin.trim()

        // 1. Ambil daftar semua pengemudi terdaftar dari database lokal
        var allDrivers = db.driverDao().getAllDrivers().first()
        if (allDrivers.isEmpty()) {
            allDrivers = getDriverDefaults()
        }

        val cleanInput = cleanDriverId.lowercase().replace(" ", "").replace("-", "")
        var matchingDriver = allDrivers.find {
            val cleanId = it.idDriver.trim().lowercase().replace(" ", "").replace("-", "")
            val cleanName = it.namaDriver.trim().lowercase().replace(" ", "").replace("-", "")

            it.idDriver.trim().equals(cleanDriverId, ignoreCase = true) ||
            it.namaDriver.trim().equals(cleanDriverId, ignoreCase = true) ||
            cleanId == cleanInput ||
            cleanName == cleanInput
        }

        // 2. Jika tidak ditemukan secara lokal dan terhubung online, coba sinkronkan daftar driver dari server lebih dahulu
        if (matchingDriver == null && prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val freshList = getDrivers()
                allDrivers = freshList
                matchingDriver = allDrivers.find {
                    val cleanId = it.idDriver.trim().lowercase().replace(" ", "").replace("-", "")
                    val cleanName = it.namaDriver.trim().lowercase().replace(" ", "").replace("-", "")

                    it.idDriver.trim().equals(cleanDriverId, ignoreCase = true) ||
                    it.namaDriver.trim().equals(cleanDriverId, ignoreCase = true) ||
                    cleanId == cleanInput ||
                    cleanName == cleanInput
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal sinkronisasi driver awal: ${e.message}")
            }
        }

        // 3. Proses Validasi PIN Server / Lokal
        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.login(
                    request = LoginApiRequest(
                        driverId = matchingDriver?.idDriver ?: cleanDriverId,
                        driverName = matchingDriver?.namaDriver ?: cleanDriverId,
                        username = cleanDriverId,
                        pin = cleanPin,
                        spreadsheetId = SPREADSHEET_ARMADA,
                        sheetId = GID_DAFTAR_DRIVER
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_DAFTAR_DRIVER
                )

                if (response.success && response.driverName != null) {
                    val returnedId = response.driverId ?: matchingDriver?.idDriver ?: cleanDriverId
                    val returnedName = response.driverName ?: matchingDriver?.namaDriver ?: cleanDriverId

                    prefs.loggedInDriverName = returnedName
                    prefs.loggedInDriverId = returnedId
                    // Simpan ke DB lokal
                    db.driverDao().insertDrivers(listOf(DriverEntity(returnedId, returnedName, cleanPin)))
                    LoginResult.Success(returnedName, returnedId)
                } else {
                    LoginResult.Error(response.message ?: "ID Driver atau PIN salah.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error server login, fallback offline: ${e.message}")
                if (matchingDriver != null) {
                    if (matchingDriver.pin.isBlank() || matchingDriver.pin == cleanPin) {
                        prefs.loggedInDriverName = matchingDriver.namaDriver
                        prefs.loggedInDriverId = matchingDriver.idDriver
                        LoginResult.Success(matchingDriver.namaDriver, matchingDriver.idDriver)
                    } else {
                        LoginResult.Error("PIN Keamanan salah.")
                    }
                } else {
                    LoginResult.Error("Gagal terhubung ke server dan ID Driver tidak ada di cache lokal.")
                }
            }
        } else {
            // Mode luring (Offline) penuh
            if (matchingDriver == null) {
                return LoginResult.Error("ID Driver atau Nama tidak terdaftar di sistem.")
            }
            return if (matchingDriver.pin.isBlank() || matchingDriver.pin == cleanPin) {
                prefs.loggedInDriverName = matchingDriver.namaDriver
                prefs.loggedInDriverId = matchingDriver.idDriver
                LoginResult.Success(matchingDriver.namaDriver, matchingDriver.idDriver)
            } else {
                LoginResult.Error("PIN Keamanan salah.")
            }
        }
    }

    private suspend fun getArmadaDefaults(): List<ArmadaEntity> {
        val list = db.armadaDao().getAllArmada().first()
        return if (list.isEmpty()) {
            val defaults = listOf(
                ArmadaEntity("HK01", "W8795PV", 48000, 45000, 5000, 50000, 2000, "AMAN", "", "", "", "01/12/2026", "05/09/2026", "01/12/2028"),
                ArmadaEntity("HK02", "A8653ZU", 59100, 55000, 5000, 60000, 900, "⚠️ SERVICE <1000 KM", "", "", "", "19/07/2027", "11/09/2026", "19/07/2031"),
                ArmadaEntity("HK03", "W8649QK", 72000, 70000, 5000, 75000, 3000, "AMAN", "", "", "", "22/06/2027", "11/12/2026", "22/06/2031"),
                ArmadaEntity("HK04", "A8190ZV", 81000, 80000, 5000, 85000, 4000, "AMAN", "", "", "", "08/06/2027", "30/09/2026", "08/06/2030")
            )
            db.armadaDao().insertArmada(defaults)
            defaults
        } else {
            list
        }
    }

    suspend fun getArmadaList(): List<ArmadaEntity> {
        return if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.getArmada(spreadsheetId = SPREADSHEET_ARMADA, sheetId = GID_ARMADA)
                if (response.success && response.armada != null && response.armada.isNotEmpty()) {
                    val existingLocal = db.armadaDao().getAllArmada().first().associateBy { it.armadaId }
                    val entities = response.armada.map {
                        val local = existingLocal[it.armadaId]
                        ArmadaEntity(
                            armadaId = it.armadaId,
                            noPolisi = it.noPolisi,
                            kmSaatIni = it.kmSaatIni,
                            kmServiceTerakhir = it.kmServiceTerakhir,
                            intervalService = it.intervalService,
                            kmServiceBerikutnya = it.kmServiceBerikutnya,
                            sisaKm = it.sisaKm,
                            status = it.status,
                            flag = it.flag,
                            fotoKm = it.fotoKm,
                            catattan = it.catattan,
                            pajakTahunan = it.pajakTahunan,
                            kirDate = it.kir,
                            pajak5Tahunan = it.pajak5Tahunan,
                            fotoTruck = it.fotoTruck ?: local?.fotoTruck
                        )
                    }
                    db.armadaDao().insertArmada(entities)
                    entities
                } else {
                    db.armadaDao().getAllArmada().first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                db.armadaDao().getAllArmada().first()
            }
        } else {
            db.armadaDao().getAllArmada().first()
        }
    }

    suspend fun getLogsList(): List<LogHarianEntity> {
        return if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.getLogs(spreadsheetId = SPREADSHEET_ARMADA, sheetId = GID_LOG_HARIAN)
                if (response.success && response.logs != null) {
                    val entities = response.logs.map {
                        LogHarianEntity(
                            tanggal = it.tanggal,
                            armadaId = it.armadaId,
                            kmTerdeteksi = it.kmTerdeteksi,
                            linkFoto = it.linkFoto,
                            catatan = it.catatan,
                            namaDriver = it.namaDriver
                        )
                    }
                    db.logHarianDao().clearLogs()
                    db.logHarianDao().insertLogs(entities)
                    entities
                } else {
                    db.logHarianDao().getAllLogs().first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                db.logHarianDao().getAllLogs().first()
            }
        } else {
            db.logHarianDao().getAllLogs().first()
        }
    }

    suspend fun getBanArmadaList(): List<BanEntity> {
        return if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.getBanArmada(spreadsheetId = SPREADSHEET_ARMADA, sheetId = GID_BAN)
                
                if (response.success && response.banArmada != null) {
                    val fetchedEntities = mutableListOf<BanEntity>()
                    var lastArmadaId = ""
                    var lastNoPolisi = ""
                    response.banArmada.forEach { item ->
                        val rawArmadaId = item.armadaId.trim().uppercase()
                        val rawNoPolisi = item.noPolisi.trim().uppercase()
                        val isAki = item.posisi.orEmpty().trim().uppercase() == "AKI"
                        
                        if (rawArmadaId.isNotEmpty()) lastArmadaId = rawArmadaId
                        if (rawNoPolisi.isNotEmpty()) lastNoPolisi = rawNoPolisi

                        if (!isAki) {
                            val resolvedBarcode = listOfNotNull(
                                item.noSeri?.takeIf { it.isNotBlank() },
                                item.codeBan?.takeIf { it.isNotBlank() },
                                item.barcode?.takeIf { it.isNotBlank() },
                                item.merk?.takeIf { it.isNotBlank() && !it.contains("Aki", ignoreCase = true) }
                            ).firstOrNull() ?: ""

                            val resolvedTahun = item.tahun?.takeIf { it.isNotBlank() } ?: "2023"
                            val resolvedKodeBan = item.codeBan?.takeIf { it.isNotBlank() } ?: item.merk?.takeIf { it.isNotBlank() } ?: resolvedBarcode

                            fetchedEntities.add(
                                BanEntity(
                                    armadaId = if (rawArmadaId.isNotEmpty()) rawArmadaId else lastArmadaId,
                                    noPolisi = if (rawNoPolisi.isNotEmpty()) rawNoPolisi else lastNoPolisi,
                                    posisi = item.posisi.orEmpty().ifBlank { "Depan Kiri (FL)" },
                                    noSeri = resolvedBarcode,
                                    ukuran = item.ukuran ?: "",
                                    merk = item.merk ?: "",
                                    kondisi = item.kondisi ?: "",
                                    tekanan = item.tekanan ?: "",
                                    keterangan = item.keterangan ?: "",
                                    barcode = resolvedBarcode,
                                    tahun = resolvedTahun,
                                    kodeBan = resolvedKodeBan,
                                    tanggalUpdate = item.tanggalUpdate
                                )
                            )
                        } else {
                            val tglAki = item.tanggalPasangAki.orEmpty().ifBlank { item.kondisi.orEmpty() }
                            val barAki = item.barcodeAki.orEmpty().ifBlank { item.barcode.orEmpty().ifBlank { item.noSeri.orEmpty() } }.ifBlank { "0255KDR" }
                            val akiMerk = item.merk.orEmpty().ifBlank { item.tekanan.orEmpty() }.ifBlank { "Aki Standard" }
                            
                            fetchedEntities.add(
                                BanEntity(
                                    armadaId = if (rawArmadaId.isNotEmpty()) rawArmadaId else lastArmadaId,
                                    noPolisi = if (rawNoPolisi.isNotEmpty()) rawNoPolisi else lastNoPolisi,
                                    posisi = "AKI",
                                    noSeri = barAki,
                                    ukuran = "12V",
                                    merk = akiMerk,
                                    kondisi = tglAki,
                                    tekanan = akiMerk,
                                    keterangan = item.keterangan.orEmpty().ifBlank { "AMAN" },
                                    barcode = barAki,
                                    tahun = item.tahun ?: "2025"
                                )
                            )
                        }
                    }

                    // Ensure every armada in Room has an Aki entry and preserve local user edits
                    try {
                        val existingLocalItems = try { db.banDao().getAllBan().first() } catch (e: Exception) { emptyList() }
                        val allArmadaList = db.armadaDao().getAllArmadaList()
                        allArmadaList.forEach { armada ->
                            val cArmKey = armada.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                            val cPolKey = armada.noPolisi.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                            
                            val serverAkiIdx = fetchedEntities.indexOfFirst { 
                                val itArmKey = it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                                val itPolKey = it.noPolisi.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                                it.posisi.trim().uppercase() == "AKI" && (
                                    (cArmKey.isNotEmpty() && (itArmKey == cArmKey || itPolKey == cArmKey)) ||
                                    (cPolKey.isNotEmpty() && (itPolKey == cPolKey || itArmKey == cPolKey))
                                )
                            }

                            val localAki = existingLocalItems.firstOrNull {
                                val itArmKey = it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                                val itPolKey = it.noPolisi.replace(Regex("[\\s\\-\\.]"), "").uppercase()
                                it.posisi.trim().uppercase() == "AKI" && (
                                    (cArmKey.isNotEmpty() && (itArmKey == cArmKey || itPolKey == cArmKey)) ||
                                    (cPolKey.isNotEmpty() && (itPolKey == cPolKey || itArmKey == cPolKey))
                                )
                            }

                            if (serverAkiIdx >= 0) {
                                val serverAki = fetchedEntities[serverAkiIdx]
                                if (localAki != null && (serverAki.kondisi.isBlank() || serverAki.kondisi == "8/2/2023")) {
                                    if (localAki.kondisi.isNotBlank()) {
                                        fetchedEntities[serverAkiIdx] = serverAki.copy(
                                            kondisi = localAki.kondisi,
                                            merk = localAki.merk,
                                            tekanan = localAki.tekanan,
                                            keterangan = localAki.keterangan,
                                            barcode = localAki.barcode,
                                            noSeri = localAki.noSeri
                                        )
                                    }
                                }
                            } else if (localAki != null) {
                                fetchedEntities.add(localAki)
                            } else if (armada.armadaId.isNotEmpty()) {
                                fetchedEntities.add(
                                    BanEntity(
                                        armadaId = armada.armadaId,
                                        noPolisi = armada.noPolisi,
                                        posisi = "AKI",
                                        noSeri = "0255KDR",
                                        ukuran = "12V",
                                        merk = "Aki Standard",
                                        kondisi = "8/2/2023",
                                        tekanan = "Aki Standard",
                                        keterangan = "AMAN",
                                        barcode = "0255KDR",
                                        tahun = "2023"
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    db.banDao().clearAllBan()
                    db.banDao().insertAllBan(fetchedEntities)
                    return db.banDao().getAllBan().first()
                } else {
                    return getBanDefaults()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return getBanDefaults()
            }
        } else {
            return getBanDefaults()
        }
    }

    private suspend fun getBanDefaults(): List<BanEntity> {
        val list = db.banDao().getAllBan().first()
        return if (list.isEmpty()) {
            val defaults = listOf(
                // HK01
                BanEntity(0, "HK01", "W8795PV", "Depan Kiri (FL)", "BS-FL-9281", "7.50-16", "Bridgestone", "Tebal (8.5mm)", "105 Psi", "Aman", "BS-FL-9281", "2023"),
                BanEntity(0, "HK01", "W8795PV", "Depan Kanan (FR)", "BS-FR-9282", "7.50-16", "Bridgestone", "Tebal (8.2mm)", "102 Psi", "Aman", "BS-FR-9282", "2023"),
                BanEntity(0, "HK01", "W8795PV", "Belakang Kiri (RL)", "GT-RL-1102", "7.50-16", "Gajah Tunggal", "Sedang (5.1mm)", "98 Psi", "Aman", "GT-RL-1102", "2024"),
                BanEntity(0, "HK01", "W8795PV", "Belakang Kanan (RR)", "GT-RR-1104", "7.50-16", "Gajah Tunggal", "Sedang (5.0mm)", "97 Psi", "Aman", "GT-RR-1104", "2024"),
                BanEntity(0, "HK01", "W8795PV", "Serep (SP)", "GT-SP-1105", "7.50-16", "Gajah Tunggal", "Sedang (4.8mm)", "96 Psi", "Aman", "GT-SP-1105", "2022"),
                BanEntity(0, "HK01", "W8795PV", "AKI", "0255KDR", "12V", "GS Astra 12V", "8/2/2023", "GS Astra 12V", "GANTI", "0255KDR", "2023"),

                // HK02
                BanEntity(0, "HK02", "A8653ZU", "Depan Kiri (FL)", "MI-FL-8811", "7.50-16", "Michelin", "Tebal (9.0mm)", "110 Psi", "Aman", "MI-FL-8811", "2024"),
                BanEntity(0, "HK02", "A8653ZU", "Depan Kanan (FR)", "MI-FR-8812", "7.50-16", "Michelin", "Tebal (9.1mm)", "108 Psi", "Aman", "MI-FR-8812", "2024"),
                BanEntity(0, "HK02", "A8653ZU", "Belakang Kiri (RL)", "BS-RL-4451", "7.50-16", "Bridgestone", "Gundul (2.1mm)", "90 Psi", "⚠️ Perlu Ganti", "BS-RL-4451", "2021"),
                BanEntity(0, "HK02", "A8653ZU", "Belakang Kanan (RR)", "BS-RR-4453", "7.50-16", "Bridgestone", "Tebal (7.2mm)", "100 Psi", "Aman", "BS-RR-4453", "2024"),
                BanEntity(0, "HK02", "A8653ZU", "Serep (SP)", "BS-SP-4454", "7.50-16", "Bridgestone", "Sedang (4.8mm)", "96 Psi", "Aman", "BS-SP-4454", "2022"),
                BanEntity(0, "HK02", "A8653ZU", "AKI", "0255KDR", "12V", "Incoe 12V", "2/16/2023", "Incoe 12V", "AMAN", "0255KDR", "2023"),

                // HK03
                BanEntity(0, "HK03", "W8649QK", "Depan Kiri (FL)", "GT-FL-3321", "7.50-16", "Gajah Tunggal", "Sedang (6.2mm)", "100 Psi", "Aman", "GT-FL-3321", "2023"),
                BanEntity(0, "HK03", "W8649QK", "Depan Kanan (FR)", "GT-FR-3322", "7.50-16", "Gajah Tunggal", "Sedang (6.0mm)", "102 Psi", "Aman", "GT-FR-3322", "2023"),
                BanEntity(0, "HK03", "W8649QK", "Belakang Kiri (RL)", "GT-RL-3323", "7.50-16", "Gajah Tunggal", "Tebal (7.8mm)", "100 Psi", "Aman", "GT-RL-3323", "2024"),
                BanEntity(0, "HK03", "W8649QK", "Belakang Kanan (RR)", "GT-RR-3325", "7.50-16", "Gajah Tunggal", "Tebal (8.0mm)", "101 Psi", "Aman", "GT-RR-3325", "2024"),
                BanEntity(0, "HK03", "W8649QK", "Serep (SP)", "GT-SP-3326", "7.50-16", "Gajah Tunggal", "Tebal (7.9mm)", "100 Psi", "Aman", "GT-SP-3326", "2022"),
                BanEntity(0, "HK03", "W8649QK", "AKI", "0255KDR", "12V", "Yuasa 12V", "3/5/2026", "Yuasa 12V", "AMAN", "0255KDR", "2026"),

                // HK04
                BanEntity(0, "HK04", "A8190ZV", "Depan Kiri (FL)", "GT-FL-4401", "7.50-16", "Gajah Tunggal", "Tebal", "100 Psi", "Aman", "GT-FL-4401", "2024"),
                BanEntity(0, "HK04", "A8190ZV", "Depan Kanan (FR)", "GT-FR-4402", "7.50-16", "Gajah Tunggal", "Tebal", "100 Psi", "Aman", "GT-FR-4402", "2024"),
                BanEntity(0, "HK04", "A8190ZV", "Belakang Kiri (RL)", "GT-RL-4403", "7.50-16", "Gajah Tunggal", "Tebal", "100 Psi", "Aman", "GT-RL-4403", "2024"),
                BanEntity(0, "HK04", "A8190ZV", "Belakang Kanan (RR)", "GT-RR-4404", "7.50-16", "Gajah Tunggal", "Tebal", "100 Psi", "Aman", "GT-RR-4404", "2024"),
                BanEntity(0, "HK04", "A8190ZV", "Serep (SP)", "GT-SP-4405", "7.50-16", "Gajah Tunggal", "Tebal", "100 Psi", "Aman", "GT-SP-4405", "2024"),
                BanEntity(0, "HK04", "A8190ZV", "AKI", "0255KDR", "12V", "Incoe 12V", "7/29/2026", "Incoe 12V", "AMAN", "0255KDR", "2026")
            )
            db.banDao().insertAllBan(defaults)
            defaults
        } else {
            list
        }
    }

    suspend fun updateBan(
        armadaId: String,
        posisi: String,
        barcode: String,
        tahun: String,
        kodeBan: String,
        tanggalUpdate: String,
        kondisi: String? = null,
        tekanan: String? = null,
        keterangan: String? = null
    ): UpdateBanResult {
        if (posisi.trim().uppercase() == "AKI") {
            return updateAki(
                armadaId = armadaId,
                barcode = barcode,
                tanggalPasang = kondisi ?: "8/2/2023",
                merk = tekanan ?: "Aki Standard",
                status = keterangan ?: "AMAN",
                tahun = tahun
            )
        }
        val cArmadaKey = armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase()
        val armadaObj = db.armadaDao().getArmadaById(armadaId) ?: db.armadaDao().getAllArmadaList().firstOrNull { 
            it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cArmadaKey 
        }
        val noPolisiStr = armadaObj?.noPolisi ?: ""

        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.updateBan(
                    request = UpdateBanApiRequest(
                        action = "update_ban",
                        banData = UpdateBanApiData(
                            armadaId = armadaId,
                            noPolisi = noPolisiStr,
                            posisi = posisi,
                            barcode = barcode,
                            tahun = tahun,
                            codeBan = if (kodeBan.isNotBlank()) kodeBan else barcode,
                            noSeri = barcode,
                            tanggalUpdate = tanggalUpdate,
                            sheetName = "BAN ARMADA"
                        ),
                        spreadsheetId = SPREADSHEET_ARMADA,
                        sheetName = "BAN ARMADA"
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA
                )

                val updatedRows = db.banDao().updateTireInfo(armadaId, posisi, barcode, tahun, kodeBan, tanggalUpdate)
                if (updatedRows == 0) {
                    db.banDao().insertAllBan(listOf(
                        BanEntity(
                            id = 0,
                            armadaId = armadaId,
                            noPolisi = noPolisiStr,
                            posisi = posisi,
                            noSeri = barcode,
                            ukuran = "-",
                            merk = "-",
                            kondisi = "Bagus",
                            tekanan = "-",
                            keterangan = "Aman",
                            barcode = barcode,
                            tahun = tahun,
                            kodeBan = kodeBan,
                            tanggalUpdate = tanggalUpdate
                        )
                    ))
                }

                if (response.success) {
                    UpdateBanResult.Success(response.message ?: "Berhasil memperbarui data ban.")
                } else {
                    UpdateBanResult.Success("Tersimpan lokal (Note: ${response.message})")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val updatedRows = db.banDao().updateTireInfo(armadaId, posisi, barcode, tahun, kodeBan, tanggalUpdate)
                if (updatedRows == 0) {
                    db.banDao().insertAllBan(listOf(
                        BanEntity(
                            id = 0,
                            armadaId = armadaId,
                            noPolisi = noPolisiStr,
                            posisi = posisi,
                            noSeri = barcode,
                            ukuran = "-",
                            merk = "-",
                            kondisi = "Bagus",
                            tekanan = "-",
                            keterangan = "Aman",
                            barcode = barcode,
                            tahun = tahun,
                            kodeBan = kodeBan,
                            tanggalUpdate = tanggalUpdate
                        )
                    ))
                }
                UpdateBanResult.Success("Tersimpan secara lokal.")
            }
        } else {
            val updatedRows = db.banDao().updateTireInfo(armadaId, posisi, barcode, tahun, kodeBan, tanggalUpdate)
            if (updatedRows == 0) {
                db.banDao().insertAllBan(listOf(
                    BanEntity(
                        id = 0,
                        armadaId = armadaId,
                        noPolisi = noPolisiStr,
                        posisi = posisi,
                        noSeri = barcode,
                        ukuran = "-",
                        merk = "-",
                        kondisi = "Bagus",
                        tekanan = "-",
                        keterangan = "Aman",
                        barcode = barcode,
                        tahun = tahun,
                        kodeBan = kodeBan,
                        tanggalUpdate = tanggalUpdate
                    )
                ))
            }
            return UpdateBanResult.Success("Berhasil memperbarui data ban secara lokal.")
        }
    }

    suspend fun updateAki(
        armadaId: String,
        barcode: String,
        tanggalPasang: String,
        merk: String,
        status: String,
        tahun: String = "2025"
    ): UpdateBanResult {
        val cArmadaKey = armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase()
        val armadaObj = db.armadaDao().getArmadaById(armadaId) ?: db.armadaDao().getAllArmadaList().firstOrNull { 
            it.armadaId.replace(Regex("[\\s\\-\\.]"), "").uppercase() == cArmadaKey 
        }
        val noPolisiStr = armadaObj?.noPolisi ?: ""

        // Always update local database first so UI reflects change immediately
        val updatedRows = db.banDao().updateAkiInfo(
            armadaId = armadaId,
            barcode = barcode,
            kondisi = tanggalPasang,
            merk = merk,
            keterangan = status,
            tahun = tahun,
            kodeBan = null,
            tanggalUpdate = null
        )
        if (updatedRows == 0) {
            db.banDao().insertAllBan(listOf(
                BanEntity(
                    id = 0,
                    armadaId = armadaId,
                    noPolisi = noPolisiStr,
                    posisi = "AKI",
                    noSeri = barcode,
                    ukuran = "12V",
                    merk = merk.ifBlank { "Aki Standard" },
                    kondisi = tanggalPasang.ifBlank { "8/2/2023" },
                    tekanan = merk.ifBlank { "Aki Standard" },
                    keterangan = status.ifBlank { "AMAN" },
                    barcode = barcode,
                    tahun = tahun,
                    kodeBan = null,
                    tanggalUpdate = null
                )
            ))
        }

        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.updateAki(
                    request = UpdateAkiApiRequest(
                        action = "update_aki",
                        akiData = UpdateAkiApiData(
                            armadaId = armadaId,
                            noPolisi = noPolisiStr,
                            barcode = barcode,
                            tanggalPasangAki = tanggalPasang,
                            merk = merk,
                            status = status,
                            kondisi = tanggalPasang,
                            tekanan = merk,
                            keterangan = status,
                            sheetId = GID_AKI,
                            sheetName = "AKI ARMADA"
                        ),
                        spreadsheetId = SPREADSHEET_ARMADA,
                        sheetId = GID_AKI,
                        sheetName = "AKI ARMADA"
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_AKI
                )

                if (response.success) {
                    UpdateBanResult.Success(response.message ?: "Berhasil memperbarui data Aki ke Google Sheets (GID ${GID_AKI}).")
                } else {
                    UpdateBanResult.Success("Aki tersimpan lokal (Sheets notice: ${response.message})")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateBanResult.Success("Berhasil memperbarui data Aki secara lokal.")
            }
        } else {
            return UpdateBanResult.Success("Berhasil memperbarui data Aki secara lokal.")
        }
    }

    suspend fun updateFotoArmada(
        armadaId: String,
        photoBytes: ByteArray,
        photoMimeType: String
    ): UpdateFotoArmadaResult {
        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val base64Photo = android.util.Base64.encodeToString(photoBytes, android.util.Base64.NO_WRAP)
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.updateFotoArmada(
                    request = UpdateFotoArmadaApiRequest(
                        armadaId = armadaId,
                        base64Photo = base64Photo,
                        photoMimeType = photoMimeType,
                        spreadsheetId = SPREADSHEET_ARMADA,
                        sheetId = GID_ARMADA
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_ARMADA
                )
                if (response.success) {
                    UpdateFotoArmadaResult.Success(response.linkFoto ?: "", response.message ?: "Berhasil.")
                } else {
                    UpdateFotoArmadaResult.Error(response.message ?: "Gagal memperbarui foto profil armada di server.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateFotoArmadaResult.Error("Gagal memperbarui foto profil armada ke server: ${e.localizedMessage}")
            }
        } else {
            return UpdateFotoArmadaResult.Success("", "Berhasil memperbarui foto profil secara lokal.")
        }
    }

    suspend fun submitDailyLog(
        driverName: String,
        armadaId: String,
        kmTerdeteksi: Int,
        photoBytes: ByteArray?,
        photoMimeType: String?,
        catatan: String?
    ): SubmitResult {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val tanggalStr = dateFormat.format(Date())

        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                var base64Photo: String? = null
                var photoName: String? = null
                if (photoBytes != null) {
                    base64Photo = Base64.encodeToString(photoBytes, Base64.NO_WRAP)
                    photoName = "ODO_${armadaId}_${System.currentTimeMillis()}.jpg"
                }

                val logData = LogDataApiItem(
                    driverName = driverName,
                    armadaId = armadaId,
                    kmTerdeteksi = kmTerdeteksi,
                    base64Photo = base64Photo,
                    photoName = photoName,
                    photoMimeType = photoMimeType ?: "image/jpeg",
                    catatan = catatan
                )

                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.submitLog(
                    request = SubmitLogApiRequest(
                        logData = logData,
                        spreadsheetId = SPREADSHEET_ARMADA,
                        sheetId = GID_LOG_HARIAN
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_LOG_HARIAN
                )

                if (response.success) {
                    val localArmada = db.armadaDao().getArmadaById(armadaId)
                    val sisaKm = response.sisaKm ?: if (localArmada != null) (localArmada.kmServiceBerikutnya - kmTerdeteksi) else 5000
                    
                    if (localArmada != null) {
                        val updated = localArmada.copy(
                            kmSaatIni = kmTerdeteksi,
                            sisaKm = sisaKm,
                            fotoKm = response.linkFoto ?: localArmada.fotoKm,
                            status = if (sisaKm < 0) "🚨 HARUS SERVICE" else if (sisaKm < 1000) "⚠️ SERVICE <1000 KM" else "AMAN"
                        )
                        db.armadaDao().updateArmada(updated)
                    }

                    db.logHarianDao().insertLog(
                        LogHarianEntity(
                            tanggal = tanggalStr,
                            armadaId = armadaId,
                            kmTerdeteksi = kmTerdeteksi,
                            linkFoto = response.linkFoto ?: "",
                            catatan = catatan ?: "",
                            namaDriver = driverName
                        )
                    )

                    SubmitResult.Success(
                        sisaKm = sisaKm,
                        serviceAlert = response.serviceAlert ?: (sisaKm < 1000),
                        linkFoto = response.linkFoto ?: ""
                    )
                } else {
                    SubmitResult.Error(response.message ?: "Gagal submit log.")
                }
            } catch (e: Exception) {
                SubmitResult.Error("Gagal submit log ke Google Sheets: ${e.localizedMessage}")
            }
        } else {
            // Local Mode
            val localArmada = db.armadaDao().getArmadaById(armadaId)
            val baseArmada = localArmada ?: ArmadaEntity(
                armadaId = armadaId,
                noPolisi = "B 1234 CD",
                kmSaatIni = 0,
                kmServiceTerakhir = 0,
                intervalService = 5000,
                kmServiceBerikutnya = kmTerdeteksi + 5000,
                sisaKm = 5000,
                status = "AMAN",
                flag = "",
                fotoKm = "",
                catattan = ""
            )

            val sisaKm = baseArmada.kmServiceBerikutnya - kmTerdeteksi
            val threshold = 1000
            val statusStr = if (sisaKm < 0) {
                "🚨 HARUS SERVICE"
            } else if (sisaKm < threshold) {
                "⚠️ SERVICE <1000 KM"
            } else {
                "AMAN"
            }

            val mockPhotoLink = if (photoBytes != null) {
                "https://drive.google.com/open?id=mock_file_id"
            } else {
                ""
            }

            val updated = baseArmada.copy(
                kmSaatIni = kmTerdeteksi,
                sisaKm = sisaKm,
                status = statusStr,
                fotoKm = mockPhotoLink
            )
            
            if (localArmada != null) {
                db.armadaDao().updateArmada(updated)
            } else {
                db.armadaDao().insertArmada(listOf(updated))
            }

            db.logHarianDao().insertLog(
                LogHarianEntity(
                    tanggal = tanggalStr,
                    armadaId = armadaId,
                    kmTerdeteksi = kmTerdeteksi,
                    linkFoto = mockPhotoLink,
                    catatan = catatan ?: "",
                    namaDriver = driverName
                )
            )

            val serviceAlert = sisaKm < threshold

            return SubmitResult.Success(
                sisaKm = sisaKm,
                serviceAlert = serviceAlert,
                linkFoto = mockPhotoLink
            )
        }
    }

    suspend fun addDriver(driver: DriverEntity) {
        db.driverDao().insertDriver(driver)
    }

    suspend fun deleteDriver(id: String) {
        db.driverDao().deleteDriverById(id)
    }

    val catatanDriverList: Flow<List<CatatanDriverEntity>> = db.catatanDriverDao().getAllCatatanDriver()

    suspend fun submitCatatanDriver(
        armadaId: String,
        driverName: String,
        catatan: String,
        tanggal: String = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    ): Boolean {
        val localArmada = db.armadaDao().getArmadaById(armadaId)
        if (localArmada != null) {
            val updated = localArmada.copy(catattan = catatan)
            db.armadaDao().updateArmada(updated)
        }

        db.catatanDriverDao().insertCatatanDriver(
            CatatanDriverEntity(
                tanggal = tanggal,
                armadaId = armadaId,
                driverName = driverName,
                catatan = catatan,
                status = "Aktif"
            )
        )

        if (prefs.isGoogleSheetsMode) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                service.submitCatatanDriver(
                    request = SubmitCatatanDriverApiRequest(
                        spreadsheetId = SPREADSHEET_ARMADA,
                        armadaId = armadaId,
                        driverName = driverName,
                        tanggal = tanggal,
                        catatan = catatan
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_CATATAN_DRIVER
                )
            } catch (e: Exception) {
                // Ignore transient network errors
            }
        }
        return true
    }

    suspend fun clearCatatanArmada(armadaId: String) {
        val localArmada = db.armadaDao().getArmadaById(armadaId)
        if (localArmada != null) {
            val updated = localArmada.copy(catattan = "")
            db.armadaDao().updateArmada(updated)
        }
        db.catatanDriverDao().clearCatatanByArmadaId(armadaId)

        if (prefs.isGoogleSheetsMode) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                service.clearCatatanDriver(
                    request = ClearCatatanDriverApiRequest(
                        spreadsheetId = SPREADSHEET_ARMADA,
                        armadaId = armadaId
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_CATATAN_DRIVER
                )
            } catch (e: Exception) {
                // Ignore transient network errors
            }
        }
    }

    suspend fun submitServiceLog(
        armadaId: String,
        kmServis: Int,
        catatan: String?
    ): SubmitServiceResult {
        // Otomatis hapus catatan/keluhan armada saat service berkala dilakukan
        db.catatanDriverDao().clearCatatanByArmadaId(armadaId)

        val isSheetsMode = prefs.isGoogleSheetsMode
        if (isSheetsMode) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.submitServiceLog(
                    request = ServiceLogApiRequest(
                        spreadsheetId = SPREADSHEET_ARMADA,
                        armadaId = armadaId,
                        kmServis = kmServis,
                        catatan = catatan
                    ),
                    spreadsheetId = SPREADSHEET_ARMADA,
                    sheetId = GID_ARMADA
                )

                if (response.success) {
                    val localArmada = db.armadaDao().getArmadaById(armadaId)
                    if (localArmada != null) {
                        val interval = localArmada.intervalService
                        val nextService = kmServis + interval
                        val updated = localArmada.copy(
                            kmServiceTerakhir = kmServis,
                            kmSaatIni = kmServis,
                            kmServiceBerikutnya = nextService,
                            sisaKm = interval,
                            status = "🟢 AMAN",
                            catattan = "" // Otomatis terhapus setelah service berkala
                        )
                        db.armadaDao().updateArmada(updated)
                    }
                    SubmitServiceResult.Success(response.message ?: "Data servis berhasil diperbarui. Catatan/keluhan driver otomatis terhapus.")
                } else {
                    SubmitServiceResult.Error(response.message ?: "Gagal memperbarui data servis.")
                }
            } catch (e: Exception) {
                SubmitServiceResult.Error("Gagal memperbarui data servis ke Google Sheets: ${e.localizedMessage}")
            }
        } else {
            // Local Mode
            val localArmada = db.armadaDao().getArmadaById(armadaId)
            if (localArmada != null) {
                val interval = localArmada.intervalService
                val nextService = kmServis + interval
                val updated = localArmada.copy(
                    kmServiceTerakhir = kmServis,
                    kmSaatIni = kmServis,
                    kmServiceBerikutnya = nextService,
                    sisaKm = interval,
                    status = "AMAN",
                    catattan = "" // Otomatis terhapus setelah service berkala
                )
                db.armadaDao().updateArmada(updated)
                return SubmitServiceResult.Success("Data servis ${armadaId} berhasil diperbarui secara lokal. Status armada kembali AMAN dan catatan driver telah dibersihkan!")
            } else {
                return SubmitServiceResult.Error("Armada tidak ditemukan.")
            }
        }
    }

    suspend fun performOcr(photoBytes: ByteArray): OcrResult {
        val isSheetsAvailable = prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()
        if (isSheetsAvailable) {
            try {
                val base64Photo = android.util.Base64.encodeToString(photoBytes, android.util.Base64.NO_WRAP)
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.performOcr(
                    request = OcrApiRequest(
                        base64Photo = base64Photo,
                        apiKey = getActiveGeminiApiKey(),
                        spreadsheetId = prefs.googleSheetId,
                        sheetId = prefs.googleSheetId
                    ),
                    spreadsheetId = prefs.googleSheetId,
                    sheetId = prefs.googleSheetId
                )

                val message = response.message ?: ""
                val isKeyError = !response.success && (
                    message.contains("API Key", ignoreCase = true) ||
                    message.contains("placeholder", ignoreCase = true) ||
                    message.contains("belum dikonfigurasi", ignoreCase = true)
                )

                if (response.success && response.km != null) {
                    return OcrResult.Success(response.km)
                } else if (isKeyError) {
                    val fallbackResult = performOcrLocally(photoBytes)
                    if (fallbackResult is OcrResult.Success) {
                        return fallbackResult
                    } else {
                        return OcrResult.Error(response.message ?: "Gagal mendeteksi odometer dari foto.")
                    }
                } else {
                    return OcrResult.Error(response.message ?: "Gagal mendeteksi odometer dari foto.")
                }
            } catch (e: Exception) {
                val fallbackResult = performOcrLocally(photoBytes)
                if (fallbackResult is OcrResult.Success) {
                    return fallbackResult
                } else {
                    return OcrResult.Error("Gagal memproses OCR: ${e.localizedMessage}")
                }
            }
        } else {
            return performOcrLocally(photoBytes)
        }
    }

    private fun compressImageIfNeeded(bytes: ByteArray): ByteArray {
        return try {
            if (bytes.size <= 500_000) return bytes
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val maxDim = 1200
            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val (newW, newH) = if (ratio > 1) {
                    maxDim to (maxDim / ratio).toInt()
                } else {
                    (maxDim * ratio).toInt() to maxDim
                }
                android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }
            val baos = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            
            val resultBytes = baos.toByteArray()
            
            // Clean up native resources to reduce memory footprint and prevent ashmem warning
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            
            resultBytes
        } catch (e: Exception) {
            bytes
        }
    }

    private suspend fun performOcrLocally(photoBytes: ByteArray): OcrResult {
        val apiKey = getActiveGeminiApiKey()
        if (apiKey.isBlank()) {
            return OcrResult.Error("API Key Gemini belum dikonfigurasi di aplikasi (Silakan isi di menu Pengaturan).")
        }

        val compressedBytes = compressImageIfNeeded(photoBytes)

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val models = listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")
            var lastError = ""

            for (model in models) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val base64Photo = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                    val mediaType = "application/json".toMediaType()
                    val requestBodyBuilder = StringBuilder()
                    requestBodyBuilder.append("{\n")
                    requestBodyBuilder.append("  \"contents\": [\n")
                    requestBodyBuilder.append("    {\n")
                    requestBodyBuilder.append("      \"parts\": [\n")
                    requestBodyBuilder.append("        { \"text\": \"Tolong baca angka odometer (kilometer saat ini) dari foto ini. Hanya kembalikan angkanya saja dalam format integer murni tanpa teks/tambahan/simbol apa pun (contoh: 124530). Jika angka tidak terbaca, kembalikan 'null'.\" },\n")
                    requestBodyBuilder.append("        {\n")
                    requestBodyBuilder.append("          \"inlineData\": {\n")
                    requestBodyBuilder.append("            \"mimeType\": \"image/jpeg\",\n")
                    requestBodyBuilder.append("            \"data\": \"$base64Photo\"\n")
                    requestBodyBuilder.append("          }\n")
                    requestBodyBuilder.append("        }\n")
                    requestBodyBuilder.append("      ]\n")
                    requestBodyBuilder.append("    }\n")
                    requestBodyBuilder.append("  ],\n")
                    requestBodyBuilder.append("  \"generationConfig\": {\n")
                    requestBodyBuilder.append("    \"responseMimeType\": \"text/plain\"\n")
                    requestBodyBuilder.append("  }\n")
                    requestBodyBuilder.append("}")

                    val body = requestBodyBuilder.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        val adapter = moshi.adapter(GeminiDirectResponse::class.java)
                        val geminiResponse = adapter.fromJson(responseBody)
                        val text = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
                        
                        val digits = text.filter { it.isDigit() }
                        val km = digits.toIntOrNull()
                        if (km != null) {
                            return@withContext OcrResult.Success(km)
                        } else {
                            lastError = "Gagal mendeteksi angka odometer dari foto: Respon AI bukan angka valid ('$text')."
                        }
                    } else if (response.code == 404) {
                        lastError = "Model $model tidak ditemukan (404)."
                        continue
                    } else {
                        val rawMessage = responseBody ?: response.message
                        if (response.code == 429 || rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || rawMessage.contains("quota", ignoreCase = true)) {
                            return@withContext OcrResult.Error("Batas kuota Gemini API telah terlampaui (RESOURCE_EXHAUSTED). Harap tunggu beberapa saat atau perbarui API Key di menu Pengaturan.")
                        }
                        if (response.code == 400 && rawMessage.contains("API_KEY_INVALID", ignoreCase = true)) {
                            return@withContext OcrResult.Error("API Key Gemini tidak valid atau belum dikonfigurasi. Silakan periksa kembali di menu Pengaturan.")
                        }
                        lastError = "Gemini OCR Error (${response.code}): $rawMessage"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FleetRepository", "OCR error with model $model: ${e.localizedMessage}", e)
                    lastError = "Gagal scan odometer secara lokal ($model): ${e.localizedMessage}"
                }
            }

            return@withContext OcrResult.Error(if (lastError.isNotEmpty()) lastError else "Gagal memproses OCR foto.")
        }
    }

    suspend fun callAsistenAi(chatMessage: String, fileBytes: ByteArray?, mimeType: String?, dbContext: String? = null): AsistenAiResult {
        val apiKey = getActiveGeminiApiKey()
        val processedBytes = if (fileBytes != null && mimeType?.startsWith("image") == true) {
            compressImageIfNeeded(fileBytes)
        } else {
            fileBytes
        }

        val knowledgeList = getAiKnowledgeList()
        val knowledgeContext = if (knowledgeList.isNotEmpty()) {
            val kSb = StringBuilder()
            kSb.append("\n=== KNOWLEDGE BASE (Google Sheets AI DATA) ===\n")
            knowledgeList.forEach { item ->
                kSb.append("Kategori: ${item.kategori}\nPertanyaan/Topik: ${item.pertanyaan}\nJawaban/SOP: ${item.jawaban}\n---\n")
            }
            kSb.toString()
        } else {
            ""
        }

        val enrichedDbContext = if (dbContext != null) {
            "$dbContext\n$knowledgeContext"
        } else {
            knowledgeContext
        }

        val isSheetsAvailable = prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()
        if (isSheetsAvailable) {
            try {
                val base64Data = processedBytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.callAsistenAi(
                    request = AsistenAiApiRequest(
                        base64Data = base64Data,
                        mimeType = mimeType,
                        chatMessage = if (enrichedDbContext.isNotEmpty()) "$chatMessage\n\n[CONTEXT DATA: $enrichedDbContext]" else chatMessage,
                        apiKey = apiKey,
                        spreadsheetId = SPREADSHEET_AI_DATA,
                        sheetId = GID_AI_DATA
                    ),
                    spreadsheetId = SPREADSHEET_AI_DATA,
                    sheetId = GID_AI_DATA
                )

                if (response.success && !response.message.isNullOrEmpty()) {
                    return AsistenAiResult.Success(response.message)
                } else {
                    android.util.Log.w("FleetRepository", "Apps Script AI response unsuccessful or empty: ${response.message}")
                    return callGeminiDirectly(chatMessage, processedBytes, mimeType, enrichedDbContext)
                }
            } catch (e: Exception) {
                android.util.Log.e("FleetRepository", "Error calling Apps Script AI, falling back to direct Gemini: ${e.localizedMessage}", e)
                return callGeminiDirectly(chatMessage, processedBytes, mimeType, enrichedDbContext)
            }
        } else {
            return callGeminiDirectly(chatMessage, processedBytes, mimeType, enrichedDbContext)
        }
    }

    private suspend fun callGeminiDirectly(chatMessage: String, fileBytes: ByteArray?, mimeType: String?, dbContext: String? = null): AsistenAiResult {
        val apiKey = getActiveGeminiApiKey()
        if (apiKey.isBlank()) {
            return AsistenAiResult.Error("API Key Gemini belum dikonfigurasi di aplikasi (Silakan isi di menu Pengaturan).")
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val models = listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")
            var lastError = ""

            for (model in models) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val mediaType = "application/json".toMediaType()
                    val requestBodyBuilder = StringBuilder()
                    requestBodyBuilder.append("{\n")
                    requestBodyBuilder.append("  \"contents\": [\n")
                    requestBodyBuilder.append("    {\n")
                    requestBodyBuilder.append("      \"parts\": [\n")
                    
                    requestBodyBuilder.append("        { \"text\": ${escapeJsonString(chatMessage.ifEmpty { "Halo, tolong bantu baca dokumen ini." })} }")
                    
                    if (fileBytes != null && mimeType != null) {
                        val base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
                        requestBodyBuilder.append(",\n")
                        requestBodyBuilder.append("        {\n")
                        requestBodyBuilder.append("          \"inlineData\": {\n")
                        requestBodyBuilder.append("            \"mimeType\": \"$mimeType\",\n")
                        requestBodyBuilder.append("            \"data\": \"$base64Data\"\n")
                        requestBodyBuilder.append("          }\n")
                        requestBodyBuilder.append("        }\n")
                    } else {
                        requestBodyBuilder.append("\n")
                    }
                    
                    requestBodyBuilder.append("      ]\n")
                    requestBodyBuilder.append("    }\n")
                    requestBodyBuilder.append("  ],\n")
                    
                    requestBodyBuilder.append("  \"systemInstruction\": {\n")
                    requestBodyBuilder.append("    \"parts\": [\n")
                    val baseInstruction = """
IDENTITAS DAN PERAN:
Kamu adalah JONI, asisten pintar aplikasi manajemen armada kendaraan komersial. Tugasmu:
1. Menampilkan dan menganalisis data armada dari database.
2. Memberikan diagnosa awal dan solusi masalah kendaraan.
3. Mengelola jadwal perawatan dan maintenance armada.

KARAKTER DAN TONALITAS:
- Bersikap tegas, profesional, dan langsung ke inti (tanpa basa-basi).
- Gunakan bahasa Indonesia yang padat dan jelas.
- Tegur pengguna secara langsung jika ada kesalahan input atau data.
- Jangan meminta maaf berlebihan. Cukup "Baik." atau "Diterima."
- Dilarang menggunakan emoji atau bahasa santai.
- Gunakan istilah teknis otomotif yang tepat dan akhiri dengan rekomendasi tindakan konkret.

KNOWLEDGE PERAWATAN:
- HINO PICKUP: Oli & Filter (10.000-15.000 km), Fuel Filter (20.000-30.000 km), Air Filter (15.000 km), Transmisi (40.000-60.000 km), Coolant (60.000 km), Rem (10.000 km).
- DAIHATSU PICKUP (Gran Max/Hijet/Luxio): Oli & Filter (3.000-5.000 km), Coolant (cek bulanan), Rem (15.000 km), Rotasi Ban (8.000 km), Baterai/Aki (3-4 tahun).

DIAGNOSA KENDARAAN:
- Mesin susah hidup: Aki lemah, glow plug, fuel filter.
- Asap putih tebal: Coolant masuk ruang bakar, head gasket leak -> [KRITIS: STOP OPERASI].
- Tarikan berat: Filter udara/injektor kotor, turbo leak.
- Rem berdecit: Kampas rem habis/kotor.
- Transmisi susah masuk: Oli transmisi kotor, kopling aus.

MANAJEMEN MAINTENANCE:
- Service A (Ringan): 5.000 km / 1 bulan.
- Service B (Sedang): 10.000 km / 3 bulan.
- Service C (Besar): 20.000 km / 6 bulan.
- Service D (Overhaul): 40.000 km / 12 bulan.
- Status: OVERDUE (terlewat >7 hari/1.000 km), MENDEKATI (sisa 7 hari/1.000 km), NORMAL.

ATURAN KETAT:
1. Dilarang membuat data palsu.
2. Gunakan satuan METRIK (km, liter, kg, C).
3. Format tanggal: DD/MM/YYYY, Mata Uang: Rp X.XXX.XXX.
4. Jika ingin update KM, sertakan tag di baris pertama: [TAG_UPDATE] | [NAMA_UNIT] | [ANGKA_KM]
""".trimIndent()
                    val systemInstruction = if (dbContext != null) {
                        "$baseInstruction\n\nData database real-time:\n$dbContext"
                    } else {
                        baseInstruction
                    }
                    requestBodyBuilder.append("      { \"text\": ${escapeJsonString(systemInstruction)} }\n")
                    requestBodyBuilder.append("    ]\n")
                    requestBodyBuilder.append("  }\n")
                    requestBodyBuilder.append("}")

                    val body = requestBodyBuilder.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        val adapter = moshi.adapter(GeminiDirectResponse::class.java)
                        val geminiResponse = adapter.fromJson(responseBody)
                        val candidate = geminiResponse?.candidates?.firstOrNull()
                        val text = candidate?.content?.parts?.firstOrNull()?.text
                        
                        if (!text.isNullOrEmpty()) {
                            return@withContext AsistenAiResult.Success(text)
                        } else if (candidate?.finishReason != null && candidate.finishReason != "STOP") {
                            lastError = "Respon dibatasi AI (Reason: ${candidate.finishReason})"
                        } else {
                            lastError = "Respon kosong dari AI Gemini."
                        }
                    } else if (response.code == 404) {
                        lastError = "Model $model tidak ditemukan (404)."
                        continue
                    } else {
                        val rawMessage = responseBody ?: response.message
                        if (response.code == 429 || rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || rawMessage.contains("quota", ignoreCase = true)) {
                            return@withContext AsistenAiResult.Error("Batas kuota Gemini API telah terlampaui (RESOURCE_EXHAUSTED). Harap tunggu beberapa saat atau perbarui API Key di menu Pengaturan.")
                        }
                        if (response.code == 400 && rawMessage.contains("API_KEY_INVALID", ignoreCase = true)) {
                            return@withContext AsistenAiResult.Error("API Key Gemini tidak valid. Silakan periksa kembali API Key Anda di menu Pengaturan.")
                        }
                        lastError = "Gemini API Error (${response.code}): $rawMessage"
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FleetRepository", "Direct Gemini API call failed ($model): ${e.localizedMessage}", e)
                    lastError = "Error ($model): ${e.localizedMessage}"
                }
            }

            return@withContext AsistenAiResult.Error(if (lastError.isNotEmpty()) lastError else "Gagal menghubungi Gemini API.")
        }
    }

    private fun escapeJsonString(input: String): String {
        return buildString {
            append("\"")
            for (ch in input) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (ch.code < 32) {
                            append(String.format("\\u%04x", ch.code))
                        } else {
                            append(ch)
                        }
                    }
                }
            }
            append("\"")
        }
    }

    private fun getActiveGeminiApiKey(): String {
        val storedKey = prefs.geminiApiKey.trim()
        if (storedKey.isNotEmpty() && storedKey != "null" && storedKey != "MY_GEMINI_API_KEY") {
            return storedKey
        }
        val buildConfigKey = com.example.BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey != "null") {
            return buildConfigKey
        }
        return ""
    }

    suspend fun getPengajuanList(): List<PengajuanEntity> {
        return if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.getPengajuan(sheetId = GID_PENGAJUAN)
                if (response.success && response.data != null) {
                    val entities = response.data.map { item ->
                        PengajuanEntity(
                            id = item.id ?: 0,
                            noPengajuan = item.noPengajuan ?: "",
                            tanggal = item.tanggal ?: "",
                            driver = item.driver ?: "",
                            armadaId = item.armadaId ?: "",
                            noPolisi = item.noPolisi ?: "",
                            kategori = item.kategori ?: "",
                            detail = item.detail ?: "",
                            catatan = item.catatan ?: "",
                            foto1Url = item.foto1Url ?: "",
                            foto2Url = item.foto2Url ?: "",
                            foto3Url = item.foto3Url ?: "",
                            foto4Url = item.foto4Url ?: "",
                            fotoLainnyaUrls = item.fotoLainnyaUrls ?: "",
                            status = item.status ?: "PENDING"
                        )
                    }
                    db.pengajuanDao().clearAll()
                    db.pengajuanDao().insertAllPengajuan(entities)
                    entities
                } else {
                    db.pengajuanDao().getAllPengajuan().first()
                }
            } catch (e: Exception) {
                db.pengajuanDao().getAllPengajuan().first()
            }
        } else {
            db.pengajuanDao().getAllPengajuan().first()
        }
    }

    suspend fun submitPengajuan(
        driver: String,
        armadaId: String,
        noPolisi: String,
        kategori: String,
        detail: String,
        catatan: String,
        files: List<MediaFileItem>
    ): SubmitPengajuanResult {
        val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val generatedNo = "PGJ-" + System.currentTimeMillis()

        val localEntity = PengajuanEntity(
            noPengajuan = generatedNo,
            tanggal = todayStr,
            driver = driver,
            armadaId = armadaId,
            noPolisi = noPolisi,
            kategori = kategori,
            detail = detail,
            catatan = catatan,
            foto1Url = if (files.isNotEmpty()) "Local Base64" else "",
            foto2Url = if (files.size > 1) "Local Base64" else "",
            foto3Url = if (files.size > 2) "Local Base64" else "",
            foto4Url = if (files.size > 3) "Local Base64" else "",
            status = "PENDING"
        )
        db.pengajuanDao().insertPengajuan(localEntity)

        if (prefs.isGoogleSheetsMode && prefs.appsScriptUrl.isNotEmpty()) {
            return try {
                val service = RetrofitClient.getApiService(prefs.appsScriptUrl)
                val response = service.submitPengajuan(
                    request = SubmitPengajuanApiRequest(
                        driver = driver,
                        armadaId = armadaId,
                        noPolisi = noPolisi,
                        kategori = kategori,
                        detail = detail,
                        catatan = catatan,
                        files = files
                    ),
                    sheetId = GID_PENGAJUAN
                )
                if (response.success) {
                    SubmitPengajuanResult.Success(generatedNo, response.message ?: "Pengajuan berhasil disimpan ke Google Sheets!")
                } else {
                    SubmitPengajuanResult.Error(response.message ?: "Gagal submit pengajuan.")
                }
            } catch (e: Exception) {
                SubmitPengajuanResult.Error("Offline / Gagal kirim ke Apps Script: ${e.localizedMessage}")
            }
        } else {
            return SubmitPengajuanResult.Success(generatedNo, "Pengajuan disimpan secara lokal.")
        }
    }
}

data class GeminiDirectResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>?
)

data class GeminiPart(
    val text: String?
)

sealed interface AsistenAiResult {
    data class Success(val message: String) : AsistenAiResult
    data class Error(val message: String) : AsistenAiResult
}

sealed interface OcrResult {
    data class Success(val km: Int) : OcrResult
    data class Error(val message: String) : OcrResult
}

sealed interface SubmitServiceResult {
    data class Success(val message: String) : SubmitServiceResult
    data class Error(val message: String) : SubmitServiceResult
}

sealed interface LoginResult {
    data class Success(val driverName: String, val driverId: String) : LoginResult
    data class Error(val message: String) : LoginResult
}

sealed interface SubmitResult {
    data class Success(val sisaKm: Int, val serviceAlert: Boolean, val linkFoto: String) : SubmitResult
    data class Error(val message: String) : SubmitResult
}

sealed interface UpdateBanResult {
    data class Success(val message: String) : UpdateBanResult
    data class Error(val message: String) : UpdateBanResult
}

sealed interface UpdateFotoArmadaResult {
    data class Success(val linkFoto: String, val message: String) : UpdateFotoArmadaResult
    data class Error(val message: String) : UpdateFotoArmadaResult
}

sealed interface SubmitPengajuanResult {
    data class Success(val noPengajuan: String, val message: String) : SubmitPengajuanResult
    data class Error(val message: String) : SubmitPengajuanResult
}
