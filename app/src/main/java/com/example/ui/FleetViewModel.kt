package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.ApkUpdateManager
import com.example.utils.ImageCompressor
import com.example.utils.InputSanitizer
import com.example.utils.NotificationHelper
import com.example.utils.UpdateUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class FleetViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val prefs = PreferenceManager(application)
    private val repository = FleetRepository(application, db, prefs)

    // Mode Settings
    private val _isGoogleSheetsMode = MutableStateFlow(prefs.isGoogleSheetsMode)
    val isGoogleSheetsMode: StateFlow<Boolean> = _isGoogleSheetsMode.asStateFlow()

    private val _appsScriptUrl = MutableStateFlow(prefs.appsScriptUrl)
    val appsScriptUrl: StateFlow<String> = _appsScriptUrl.asStateFlow()

    private val _googleSheetId = MutableStateFlow(prefs.googleSheetId)
    val googleSheetId: StateFlow<String> = _googleSheetId.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.geminiApiKey)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // In-App Direct APK Updater
    private val apkUpdateManager = ApkUpdateManager(
        application,
        RetrofitClient.getApiService(prefs.appsScriptUrl)
    )
    val updateState: StateFlow<UpdateUiState> = apkUpdateManager.updateState

    fun checkForUpdates() {
        viewModelScope.launch {
            apkUpdateManager.checkForUpdates()
        }
    }

    fun downloadAndInstallApk(url: String) {
        apkUpdateManager.downloadAndInstallApk(url)
    }

    fun dismissUpdateDialog() {
        apkUpdateManager.dismissUpdate()
    }

    // Synchronization Check States
    private val _syncChecking = MutableStateFlow(false)
    val syncChecking: StateFlow<Boolean> = _syncChecking.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _syncSuccess = MutableStateFlow<Boolean?>(null)
    val syncSuccess: StateFlow<Boolean?> = _syncSuccess.asStateFlow()

    // Login Session
    private val _loggedInDriverName = MutableStateFlow(prefs.loggedInDriverName)
    val loggedInDriverName: StateFlow<String> = _loggedInDriverName.asStateFlow()

    // Loaded Lists
    private val _drivers = MutableStateFlow<List<DriverEntity>>(emptyList())
    val drivers: StateFlow<List<DriverEntity>> = _drivers.asStateFlow()

    private val _armadaList = MutableStateFlow<List<ArmadaEntity>>(emptyList())
    val armadaList: StateFlow<List<ArmadaEntity>> = _armadaList.asStateFlow()

    private val _logs = MutableStateFlow<List<LogHarianEntity>>(emptyList())
    val logs: StateFlow<List<LogHarianEntity>> = _logs.asStateFlow()

    private val _banList = MutableStateFlow<List<BanEntity>>(emptyList())
    val banList: StateFlow<List<BanEntity>> = _banList.asStateFlow()

    private val _pengirimanList = MutableStateFlow<List<PengirimanEntity>>(emptyList())
    val pengirimanList: StateFlow<List<PengirimanEntity>> = _pengirimanList.asStateFlow()

    private val _selectedPengirimanTab = MutableStateFlow("Hari Ini")
    val selectedPengirimanTab: StateFlow<String> = _selectedPengirimanTab.asStateFlow()

    // States for Screens
    // Login Screen State
    private val _selectedDriverName = MutableStateFlow("")
    val selectedDriverName: StateFlow<String> = _selectedDriverName.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Form Input State
    private val _selectedArmadaId = MutableStateFlow("")
    val selectedArmadaId: StateFlow<String> = _selectedArmadaId.asStateFlow()

    private val _kmInput = MutableStateFlow("")
    val kmInput: StateFlow<String> = _kmInput.asStateFlow()

    private val _catatanInput = MutableStateFlow("")
    val catatanInput: StateFlow<String> = _catatanInput.asStateFlow()

    private val _selectedPhoto = MutableStateFlow<Bitmap?>(null)
    val selectedPhoto: StateFlow<Bitmap?> = _selectedPhoto.asStateFlow()

    private val _submitLoading = MutableStateFlow(false)
    val submitLoading: StateFlow<Boolean> = _submitLoading.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError: StateFlow<String?> = _submitError.asStateFlow()

    // Submit dialog alert
    private val _submitSuccessData = MutableStateFlow<SubmitSuccessData?>(null)
    val submitSuccessData: StateFlow<SubmitSuccessData?> = _submitSuccessData.asStateFlow()

    // OCR Auto-Scanner State
    private val _ocrLoading = MutableStateFlow(false)
    val ocrLoading: StateFlow<Boolean> = _ocrLoading.asStateFlow()

    private val _ocrSuccessMessage = MutableStateFlow<String?>(null)
    val ocrSuccessMessage: StateFlow<String?> = _ocrSuccessMessage.asStateFlow()

    // Global Data Fetch States
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _dataErrorMessage = MutableStateFlow<String?>(null)
    val dataErrorMessage: StateFlow<String?> = _dataErrorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun clearOcrMessage() {
        _ocrSuccessMessage.value = null
    }

    init {
        // Collect DB updates in background
        viewModelScope.launch {
            repository.localLogs.collectLatest {
                _logs.value = it
            }
        }
        viewModelScope.launch {
            repository.localArmada.collectLatest {
                _armadaList.value = it
                checkServiceDistanceAndNotify(it)
                checkPajakAndKirAndNotify(it)
            }
        }
        viewModelScope.launch {
            repository.localDrivers.collectLatest {
                _drivers.value = it
            }
        }
        viewModelScope.launch {
            repository.localBan.collectLatest {
                _banList.value = it
                checkAkiAgeAndNotify(it)
            }
        }
        viewModelScope.launch {
            repository.localPengiriman.collectLatest {
                _pengirimanList.value = it
            }
        }

        // Initial fetch
        refreshMetadata()
        checkForUpdates()

        // Auto Refresh Timer every 60 seconds
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000)
                refreshMetadata(isSilent = true)
            }
        }
    }

    fun refreshMetadata(isPullToRefresh: Boolean = false, isSilent: Boolean = false) {
        viewModelScope.launch {
            if (isPullToRefresh) {
                _isRefreshing.value = true
            } else if (!isSilent) {
                _isLoading.value = true
            }
            _dataErrorMessage.value = null

            try {
                _drivers.value = repository.getDrivers()
                _armadaList.value = repository.getArmadaList()
                checkServiceDistanceAndNotify(_armadaList.value)
                checkPajakAndKirAndNotify(_armadaList.value)
                _logs.value = repository.getLogsList()
                _banList.value = repository.getBanArmadaList()
                checkAkiAgeAndNotify(_banList.value)
                _dataErrorMessage.value = null
            } catch (e: Exception) {
                val err = "Gagal mengambil data dari Google Apps Script: ${e.localizedMessage ?: "Kesalahan koneksi"}"
                _dataErrorMessage.value = err
                _toastMessage.value = "Koneksi ke Google Apps Script Gagal! Menampilkan data lokal."
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun retryFetch() {
        refreshMetadata(isPullToRefresh = false, isSilent = false)
    }

    fun pullToRefresh() {
        refreshMetadata(isPullToRefresh = true, isSilent = false)
    }

    // Settings actions
    fun setGoogleSheetsMode(enabled: Boolean) {
        prefs.isGoogleSheetsMode = enabled
        _isGoogleSheetsMode.value = enabled
        refreshMetadata()
    }

    fun setAppsScriptUrl(url: String) {
        prefs.appsScriptUrl = url
        _appsScriptUrl.value = url
        refreshMetadata()
    }

    fun setGoogleSheetId(sheetId: String) {
        prefs.googleSheetId = sheetId
        _googleSheetId.value = sheetId
        refreshMetadata()
    }

    fun setGeminiApiKey(key: String) {
        prefs.geminiApiKey = key
        _geminiApiKey.value = key
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled
    }

    fun setThemeMode(mode: String) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun triggerTestNotification() {
        NotificationHelper.sendNotification(
            getApplication(),
            "Uji Coba Notifikasi",
            "Halo Bos! Fitur Notifikasi HUB Kediri berfungsi dengan baik dan siap digunakan."
        )
    }

    fun scheduleDaily8AmReminder() {
        com.example.worker.WorkManagerScheduler.scheduleDaily8AmReminder(getApplication())
        _toastMessage.value = "Pengingat otomatis latar belakang jam 08:00 WIB berhasil dijadwalkan!"
    }

    fun triggerBackgroundReminderNow() {
        com.example.worker.WorkManagerScheduler.triggerImmediateTestReminder(getApplication())
        _toastMessage.value = "Memproses pengecekan latar belakang..."
    }

    fun checkSyncStatus() {
        viewModelScope.launch {
            _syncChecking.value = true
            _syncMessage.value = "Menghubungkan ke Google Spreadsheet..."
            _syncSuccess.value = null
            try {
                if (!prefs.isGoogleSheetsMode || prefs.appsScriptUrl.isEmpty()) {
                    _syncChecking.value = false
                    _syncSuccess.value = false
                    _syncMessage.value = "Mode Sinkronisasi Google Sheets tidak aktif atau URL kosong."
                    return@launch
                }
                
                val fetchedDrivers = repository.getDrivers()
                val fetchedArmada = repository.getArmadaList()
                val fetchedLogs = repository.getLogsList()
                
                refreshMetadata()
                
                _syncSuccess.value = true
                _syncMessage.value = "SINKRONISASI BERHASIL!\n\n" +
                        "• Terhubung ke Spreadsheet ID: ${prefs.googleSheetId.take(8)}...${prefs.googleSheetId.takeLast(6)}\n" +
                        "• Jumlah Pengemudi: ${fetchedDrivers.size}\n" +
                        "• Jumlah Armada: ${fetchedArmada.size}\n" +
                        "• Riwayat Log Terunduh: ${fetchedLogs.size}\n" +
                        "• Terakhir Sinkron: ${java.text.SimpleDateFormat("HH:mm:ss 'WIB'", java.util.Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta") }.format(java.util.Date())}"
            } catch (e: Exception) {
                _syncSuccess.value = false
                _syncMessage.value = "Gagal Terhubung:\n${e.localizedMessage ?: "Kesalahan Jaringan"}\n\nPastikan internet aktif dan URL Web App Apps Script sudah benar."
            } finally {
                _syncChecking.value = false
            }
        }
    }

    // Login Actions
    fun setSelectedDriver(name: String) {
        _selectedDriverName.value = name
        _loginError.value = null
    }

    fun setPin(pin: String) {
        _pinInput.value = pin
        _loginError.value = null
    }

    fun login(onSuccess: () -> Unit) {
        val cleanDriverId = InputSanitizer.sanitizeText(_selectedDriverName.value)
        val cleanPin = InputSanitizer.sanitizeNumeric(_pinInput.value)

        if (cleanDriverId.isEmpty()) {
            _loginError.value = "Silakan masukkan ID Driver yang valid."
            return
        }
        if (cleanPin.isEmpty()) {
            _loginError.value = "PIN tidak boleh kosong dan harus berupa angka."
            return
        }

        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null
            when (val result = repository.validateLogin(cleanDriverId, cleanPin)) {
                is LoginResult.Success -> {
                    _loggedInDriverName.value = result.driverName
                    _pinInput.value = ""
                    _selectedDriverName.value = ""
                    onSuccess()
                }
                is LoginResult.Error -> {
                    _loginError.value = result.message
                }
            }
            _loginLoading.value = false
        }
    }

    fun logout(onSuccess: () -> Unit) {
        prefs.clearLogin()
        _loggedInDriverName.value = ""
        onSuccess()
    }

    // Form Actions
    fun setSelectedArmada(armadaId: String) {
        _selectedArmadaId.value = armadaId
        _submitError.value = null
    }

    fun setKmInput(km: String) {
        _kmInput.value = km
        _submitError.value = null
    }

    fun setCatatanInput(catatan: String) {
        _catatanInput.value = catatan
    }

    fun setPhoto(bitmap: Bitmap?) {
        _selectedPhoto.value = bitmap
        if (bitmap != null) {
            triggerOcr(bitmap)
        }
    }

    private fun triggerOcr(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrLoading.value = true
            _ocrSuccessMessage.value = null
            _submitError.value = null
            
            val bytes = ImageCompressor.compressBitmapToWebP(bitmap, maxDimension = 1280, quality = 80)
            
            when (val result = repository.performOcr(bytes)) {
                is OcrResult.Success -> {
                    _kmInput.value = result.km.toString()
                    _ocrSuccessMessage.value = "Odometer berhasil dipindai! Silakan periksa kembali angkanya sebelum mengirim."
                }
                is OcrResult.Error -> {
                    android.util.Log.e("FleetViewModel", "OCR Error: ${result.message}")
                    _submitError.value = "Gagal memindai odometer: ${result.message}"
                }
            }
            _ocrLoading.value = false
        }
    }

    fun dismissSuccessDialog() {
        _submitSuccessData.value = null
    }

    fun submitLog() {
        val driverName = _loggedInDriverName.value
        val armadaId = _selectedArmadaId.value
        val kmStr = _kmInput.value
        val catatan = _catatanInput.value
        val bitmap = _selectedPhoto.value

        if (driverName.isEmpty()) {
            _submitError.value = "Anda belum login!"
            return
        }
        if (armadaId.isEmpty()) {
            _submitError.value = "Pilih Armada terlebih dahulu."
            return
        }
        val kmVal = kmStr.toIntOrNull()
        if (kmVal == null || kmVal <= 0) {
            _submitError.value = "Masukkan nilai KM saat ini dengan angka yang valid."
            return
        }

        // Validate that input KM is not less than the current KM on record
        val matchingArmada = _armadaList.value.find { it.armadaId == armadaId }
        if (matchingArmada != null && kmVal < matchingArmada.kmSaatIni) {
            _submitError.value = "KM yang diinput (${kmVal}) tidak boleh kurang dari KM saat ini (${matchingArmada.kmSaatIni})."
            return
        }

        viewModelScope.launch {
            _submitLoading.value = true
            _submitError.value = null

            // Convert bitmap to bytes with efficient WebP compression
            var photoBytes: ByteArray? = null
            if (bitmap != null) {
                photoBytes = ImageCompressor.compressBitmapToWebP(bitmap, maxDimension = 1280, quality = 80)
            }

            val result = repository.submitDailyLog(
                driverName = driverName,
                armadaId = armadaId,
                kmTerdeteksi = kmVal,
                photoBytes = photoBytes,
                photoMimeType = ImageCompressor.MIME_TYPE_WEBP,
                catatan = catatan
            )

            when (result) {
                is SubmitResult.Success -> {
                    _submitSuccessData.value = SubmitSuccessData(
                        armadaId = armadaId,
                        sisaKm = result.sisaKm,
                        serviceAlert = result.serviceAlert,
                        linkFoto = result.linkFoto
                    )
                    
                    // Trigger dynamic local notification
                    NotificationHelper.sendNotification(
                        getApplication(),
                        "Entri Odometer Berhasil Disimpan",
                        "Odometer unit $armadaId berhasil dicatat sebesar $kmVal KM."
                    )

                    // Reset input fields on success
                    _selectedArmadaId.value = ""
                    _kmInput.value = ""
                    _catatanInput.value = ""
                    _selectedPhoto.value = null
                    // Update metadata
                    refreshMetadata()
                }
                is SubmitResult.Error -> {
                    _submitError.value = result.message
                }
            }
            _submitLoading.value = false
        }
    }

    fun addDriver(name: String, pin: String) {
        viewModelScope.launch {
            val id = "D" + System.currentTimeMillis().toString().takeLast(4)
            repository.addDriver(DriverEntity(id, name, pin))
        }
    }

    fun deleteDriver(id: String) {
        viewModelScope.launch {
            val deletedDriver = drivers.value.find { it.idDriver == id }
            repository.deleteDriver(id)
            if (deletedDriver?.namaDriver == _loggedInDriverName.value) {
                logout {}
            }
        }
    }

    private val _serviceLoading = MutableStateFlow(false)
    val serviceLoading: StateFlow<Boolean> = _serviceLoading.asStateFlow()

    private val _serviceSuccessMessage = MutableStateFlow<String?>(null)
    val serviceSuccessMessage: StateFlow<String?> = _serviceSuccessMessage.asStateFlow()

    private val _serviceErrorMessage = MutableStateFlow<String?>(null)
    val serviceErrorMessage: StateFlow<String?> = _serviceErrorMessage.asStateFlow()

    fun clearServiceMessages() {
        _serviceSuccessMessage.value = null
        _serviceErrorMessage.value = null
    }

    fun submitServiceLog(armadaId: String, kmServis: Int, catatan: String?) {
        viewModelScope.launch {
            _serviceLoading.value = true
            _serviceSuccessMessage.value = null
            _serviceErrorMessage.value = null

            val result = repository.submitServiceLog(armadaId, kmServis, catatan)
            when (result) {
                is SubmitServiceResult.Success -> {
                    _serviceSuccessMessage.value = result.message
                    
                    // Trigger dynamic local notification
                    NotificationHelper.sendNotification(
                        getApplication(),
                        "Catatan Servis Disimpan",
                        "Catatan servis unit $armadaId berhasil disimpan pada $kmServis KM."
                    )

                    refreshMetadata()
                }
                is SubmitServiceResult.Error -> {
                    _serviceErrorMessage.value = result.message
                }
            }
            _serviceLoading.value = false
        }
    }

    // Catatan Driver State & Actions
    val catatanDriverList: StateFlow<List<CatatanDriverEntity>> = repository.catatanDriverList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun submitCatatanDriver(armadaId: String, driverName: String, catatan: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.submitCatatanDriver(
                armadaId = armadaId,
                driverName = driverName,
                catatan = catatan
            )
            refreshMetadata()
            onSuccess()
        }
    }

    fun clearCatatanArmada(armadaId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearCatatanArmada(armadaId)
            refreshMetadata()
            onSuccess()
        }
    }

    // AI Assistant State (Removed)
    private val _aiChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiChatHistory: StateFlow<List<ChatMessage>> = _aiChatHistory.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    fun clearAiChatHistory() {
        _aiChatHistory.value = emptyList()
    }

    fun sendAiChatMessage(messageText: String) {
        // Chat AI feature disabled
    }

    fun findMediaItemsForQuery(queryText: String): List<ChatMediaItem> {
        val qRaw = queryText.lowercase().trim()
        val qClean = qRaw.replace("-", "").replace(" ", "")
        val tokens = qRaw.split(" ", "-", "/", ",").filter { it.length >= 2 }
        val results = mutableListOf<ChatMediaItem>()

        val isPengirimanQuery = qRaw.contains("pengiriman") || qRaw.contains("bukti") || qRaw.contains("surat") || 
                                qRaw.contains("sj") || qRaw.contains("dokumen") || qRaw.contains("jalan") || 
                                qRaw.contains("arsip") || qRaw.contains("1878433267") || qRaw.contains("cust") ||
                                qRaw.contains("customer") || qRaw.contains("penerima") || qRaw.contains("tujuan")

        val isOdometerQuery = qRaw.contains("odometer") || qRaw.contains("km") || qRaw.contains("log") || 
                              qRaw.contains("harian") || qRaw.contains("pemeriksaan") || qRaw.contains("1263706817") ||
                              qRaw.contains("odo")

        val isTruckQuery = qRaw.contains("truck") || qRaw.contains("truk") || qRaw.contains("armada") || qRaw.contains("unit")
        val isGeneralSearch = qRaw.contains("foto") || qRaw.contains("gambar") || qRaw.contains("file") || 
                              qRaw.contains("vidio") || qRaw.contains("video") || qRaw.contains("semua") || 
                              qRaw.contains("cari") || qRaw.contains("drive") || qRaw.contains("folder")

        // Search Helper matcher
        fun matchesField(fieldValue: String): Boolean {
            if (fieldValue.isBlank()) return false
            val f = fieldValue.lowercase()
            val fClean = f.replace("-", "").replace(" ", "")
            if (f.contains(qRaw) || fClean.contains(qClean)) return true
            return tokens.any { token -> f.contains(token) || fClean.contains(token) }
        }

        // 1. Search Physical Truck Photo (Priority when searching armada/foto/truck)
        val armadas = _armadaList.value
        armadas.forEach { a ->
            val isMatch = matchesField(a.armadaId) || matchesField(a.noPolisi) || matchesField(a.status)
            if (isMatch || (isTruckQuery && tokens.isEmpty()) || (isGeneralSearch && tokens.any { matchesField(a.armadaId) })) {
                val truckPhoto = if (!a.fotoTruck.isNullByBlank()) a.fotoTruck!! else "https://images.unsplash.com/photo-1519003722824-194d4455a60c?w=800&auto=format&fit=crop"
                results.add(
                    ChatMediaItem(
                        title = "🚚 Foto Fisik Truck - ${a.armadaId} (${a.noPolisi})",
                        description = "Status: ${a.status} | Sisa KM Service: ${a.sisaKm} km | KIR: ${a.kirDate ?: "-"}",
                        urlOrPath = truckPhoto,
                        type = "IMAGE",
                        source = "Master Armada"
                    )
                )
            }
        }

        // 2. Search Log Foto Odometer KM (GID 1263706817)
        val logsList = _logs.value
        logsList.forEach { log ->
            val isMatch = matchesField(log.tanggal) || 
                          matchesField(log.armadaId) || 
                          matchesField(log.namaDriver) || 
                          matchesField(log.catatan) || 
                          matchesField(log.kmTerdeteksi.toString())

            if (isMatch || (isOdometerQuery && tokens.isEmpty())) {
                val photoUrl = if (log.linkFoto.isNotBlank() && log.linkFoto.startsWith("http")) {
                    log.linkFoto
                } else {
                    "https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?w=800&auto=format&fit=crop"
                }
                results.add(
                    ChatMediaItem(
                        title = "⏱️ Foto Odometer KM - ${log.armadaId}",
                        description = "Tgl: ${log.tanggal} | Driver: ${log.namaDriver} | Terdeteksi: ${log.kmTerdeteksi} km",
                        urlOrPath = photoUrl,
                        type = "IMAGE",
                        source = "Log KM/Odometer"
                    )
                )
            }
        }

        // 3. Search Arsip Bukti Pengiriman (GID 1878433267)
        val pengirimanList = _pengirimanList.value
        pengirimanList.forEach { p ->
            val isMatch = matchesField(p.tanggal) || 
                          matchesField(p.armada) || 
                          matchesField(p.driver) || 
                          matchesField(p.tujuan) || 
                          matchesField(p.penerima) || 
                          matchesField(p.noDokumen) || 
                          matchesField(p.noSuratJalan) || 
                          matchesField(p.status)

            if (isMatch || (isPengirimanQuery && tokens.isEmpty())) {
                val label = p.noSuratJalan.ifBlank { p.noDokumen.ifBlank { "Pengiriman #${p.id}" } }
                results.add(
                    ChatMediaItem(
                        title = "📸 Bukti Foto Depan - $label",
                        description = "Tgl: ${p.tanggal} | Armada: ${p.armada} | Cust/Tujuan: ${p.tujuan} (${p.status})",
                        urlOrPath = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=800&auto=format&fit=crop",
                        type = "IMAGE",
                        source = "Arsip Pengiriman"
                    )
                )
                results.add(
                    ChatMediaItem(
                        title = "📄 Bukti Surat Jalan & Muatan - $label",
                        description = "No Dokumen: ${p.noDokumen} | Driver: ${p.driver} | Penerima: ${p.penerima}",
                        urlOrPath = "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=800&auto=format&fit=crop",
                        type = "IMAGE",
                        source = "Arsip Pengiriman"
                    )
                )
            }
        }

        // 4. Folder Drive links are included ONLY if user explicitly searches for "folder" or "drive"
        val wantsFolder = qRaw.contains("folder") || qRaw.contains("drive") || qRaw.contains("direktori")
        if (wantsFolder) {
            if (isPengirimanQuery || isGeneralSearch || tokens.any { it.startsWith("hk") }) {
                results.add(
                    ChatMediaItem(
                        title = "📁 Folder Drive Foto & Video Pengiriman",
                        description = "Arsip Bukti Pengiriman (Sheet GID: 1878433267 | Drive ID: 12NyXxBBU8MOcr6so-LCrRazCQifHeSv1)",
                        urlOrPath = "https://drive.google.com/drive/folders/12NyXxBBU8MOcr6so-LCrRazCQifHeSv1",
                        type = "FILE",
                        source = "Google Drive (1878433267)"
                    )
                )
            }

            if (isOdometerQuery || isGeneralSearch || tokens.any { it.startsWith("hk") }) {
                results.add(
                    ChatMediaItem(
                        title = "📁 Folder Drive Foto Odometer KM",
                        description = "Data Log Odometer KM (Sheet GID: 1263706817 | Drive ID: 1ZpPEGaVCz0qmu37r_Eq8H8701a3bOS76)",
                        urlOrPath = "https://drive.google.com/drive/folders/1ZpPEGaVCz0qmu37r_Eq8H8701a3bOS76",
                        type = "FILE",
                        source = "Google Drive (1263706817)"
                    )
                )
            }
        }

        return results.distinctBy { it.title + it.urlOrPath }.take(8)
    }

    private fun String?.isNullByBlank(): Boolean = this.isNullOrEmpty() || this.isBlank()

    private fun buildDatabaseContextString(): String {
        val armada = _armadaList.value
        val driversList = _drivers.value
        val recentLogs = _logs.value.take(10)
        val banList = _banList.value

        val sb = java.lang.StringBuilder()
        
        // 1. Armada Info
        sb.append("=== DAFTAR ARMADA ===\n")
        if (armada.isEmpty()) {
            sb.append("(Tidak ada data armada)\n")
        } else {
            armada.forEach { a ->
                sb.append("- ID: ${a.armadaId} | No Polisi: ${a.noPolisi} | KM Saat Ini: ${a.kmSaatIni} km | Servis Terakhir: ${a.kmServiceTerakhir} km | Sisa KM Servis: ${a.sisaKm} km | Status: ${a.status} | Pajak Tahunan/STNK: ${a.pajakTahunan ?: "-"} | KIR: ${a.kirDate ?: "-"} | Pajak 5 Tahunan: ${a.pajak5Tahunan ?: "-"} | Catatan: ${a.catattan ?: "-"}\n")
            }
        }
        sb.append("\n")

        // 2. Drivers Info
        sb.append("=== DAFTAR DRIVER ===\n")
        if (driversList.isEmpty()) {
            sb.append("(Tidak ada data driver)\n")
        } else {
            driversList.forEach { d ->
                sb.append("- ID: ${d.idDriver} | Nama: ${d.namaDriver}\n")
            }
        }
        sb.append("\n")

        // 3. Recent Odometer Logs
        sb.append("=== 10 LOG/RIWAYAT HARIAN TERBARU ===\n")
        if (recentLogs.isEmpty()) {
            sb.append("(Tidak ada log harian terbaru)\n")
        } else {
            recentLogs.forEach { l ->
                sb.append("- Tanggal: ${l.tanggal} | Armada: ${l.armadaId} | Driver: ${l.namaDriver} | KM Terdeteksi: ${l.kmTerdeteksi} km | Catatan: ${l.catatan ?: "-"}\n")
            }
        }
        sb.append("\n")

        // 4. Tires (Ban) Info
        sb.append("=== DAFTAR BAN ARMADA ===\n")
        if (banList.isEmpty()) {
            sb.append("(Tidak ada data ban)\n")
        } else {
            banList.forEach { b ->
                sb.append("- Armada ID: ${b.armadaId} | Posisi: ${b.posisi} | No Seri: ${b.noSeri} | Ukuran: ${b.ukuran} | Merk: ${b.merk} | Kondisi: ${b.kondisi} | Tekanan: ${b.tekanan} | Ket: ${b.keterangan} | Barcode: ${b.barcode ?: "-"} | Tahun: ${b.tahun ?: "-"}\n")
            }
        }
        
        return sb.toString()
    }

    fun updateArmadaFotoTruck(armadaId: String, uriString: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val uri = android.net.Uri.parse(uriString)
                val bytes = ImageCompressor.compressUriToWebP(context, uri, maxDimension = 1280, quality = 80)
                if (bytes != null) {
                    // Save locally first for offline cache
                    val dir = java.io.File(context.filesDir, "truck_photos")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val destFile = java.io.File(dir, "truck_${armadaId}_${System.currentTimeMillis()}.webp")
                    destFile.writeBytes(bytes)
                    
                    // Upload to Google Drive / Sheets in high-efficiency WebP format
                    val mimeType = ImageCompressor.MIME_TYPE_WEBP
                    val uploadResult = repository.updateFotoArmada(armadaId, bytes, mimeType)
                    
                    val linkFoto = when (uploadResult) {
                        is com.example.data.UpdateFotoArmadaResult.Success -> {
                            if (uploadResult.linkFoto.isNotEmpty()) uploadResult.linkFoto else destFile.absolutePath
                        }
                        is com.example.data.UpdateFotoArmadaResult.Error -> {
                            destFile.absolutePath
                        }
                    }
                    
                    val local = db.armadaDao().getArmadaById(armadaId)
                    if (local != null) {
                        val updated = local.copy(fotoTruck = linkFoto)
                        db.armadaDao().updateArmada(updated)
                        refreshMetadata()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _banUpdateStatus = MutableStateFlow<String?>(null)
    val banUpdateStatus: StateFlow<String?> = _banUpdateStatus.asStateFlow()

    private val _isBanUpdating = MutableStateFlow(false)
    val isBanUpdating: StateFlow<Boolean> = _isBanUpdating.asStateFlow()

    private val notifiedServiceArmadas = mutableSetOf<String>()
    private val notifiedPajakTahunanArmadas = mutableSetOf<String>()
    private val notifiedPajak5TahunanArmadas = mutableSetOf<String>()
    private val notifiedKirArmadas = mutableSetOf<String>()

    private fun calculateDaysRemaining(dateStr: String?): Long? {
        val rawDateStr = dateStr.orEmpty().trim()
        if (rawDateStr.isBlank() || rawDateStr == "-") return null

        val formats = listOf(
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("M/d/yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US),
            java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US)
        )

        var parsedDate: java.util.Date? = null
        for (f in formats) {
            try {
                parsedDate = f.parse(rawDateStr)
                if (parsedDate != null) break
            } catch (_: Exception) {}
        }

        if (parsedDate == null) return null

        val todayCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = todayCal.time

        val diffMillis = parsedDate.time - today.time
        return diffMillis / (1000 * 60 * 60 * 24)
    }

    fun checkPajakAndKirAndNotify(armadaList: List<ArmadaEntity>) {
        viewModelScope.launch {
            armadaList.forEach { armada ->
                // Check Pajak Tahunan (STNK)
                armada.pajakTahunan?.let { pajak ->
                    val sisaDays = calculateDaysRemaining(pajak)
                    if (sisaDays != null && sisaDays <= 30) {
                        if (!notifiedPajakTahunanArmadas.contains(armada.armadaId)) {
                            notifiedPajakTahunanArmadas.add(armada.armadaId)
                            val title = "📅 Peringatan Pajak Tahunan Unit ${armada.armadaId}"
                            val msg = if (sisaDays <= 0) {
                                "Masa berlaku Pajak Tahunan unit ${armada.armadaId} (${armada.noPolisi}) telah berakhir pada $pajak! Segera lakukan pembayaran."
                            } else {
                                "Pajak Tahunan unit ${armada.armadaId} (${armada.noPolisi}) akan jatuh tempo dalam $sisaDays hari lagi ($pajak). Harap persiapkan dokumen!"
                            }
                            NotificationHelper.sendNotification(getApplication(), title, msg)
                        }
                    } else if (sisaDays != null && sisaDays > 30) {
                        notifiedPajakTahunanArmadas.remove(armada.armadaId)
                    }
                }

                // Check Pajak 5 Tahunan
                armada.pajak5Tahunan?.let { pajak5 ->
                    val sisaDays = calculateDaysRemaining(pajak5)
                    if (sisaDays != null && sisaDays <= 30) {
                        if (!notifiedPajak5TahunanArmadas.contains(armada.armadaId)) {
                            notifiedPajak5TahunanArmadas.add(armada.armadaId)
                            val title = "📅 Peringatan Pajak 5 Tahunan Unit ${armada.armadaId}"
                            val msg = if (sisaDays <= 0) {
                                "Masa berlaku Pajak 5 Tahunan/STNK unit ${armada.armadaId} (${armada.noPolisi}) telah berakhir pada $pajak5! Segera lakukan perpanjangan."
                            } else {
                                "Pajak 5 Tahunan/STNK unit ${armada.armadaId} (${armada.noPolisi}) akan jatuh tempo dalam $sisaDays hari lagi ($pajak5). Segera urus perpanjangan!"
                            }
                            NotificationHelper.sendNotification(getApplication(), title, msg)
                        }
                    } else if (sisaDays != null && sisaDays > 30) {
                        notifiedPajak5TahunanArmadas.remove(armada.armadaId)
                    }
                }

                // Check KIR Date
                armada.kirDate?.let { kir ->
                    val sisaDays = calculateDaysRemaining(kir)
                    if (sisaDays != null && sisaDays <= 30) {
                        if (!notifiedKirArmadas.contains(armada.armadaId)) {
                            notifiedKirArmadas.add(armada.armadaId)
                            val title = "🔍 Peringatan Uji KIR Unit ${armada.armadaId}"
                            val msg = if (sisaDays <= 0) {
                                "Masa berlaku KIR unit ${armada.armadaId} (${armada.noPolisi}) telah berakhir pada $kir! Segera lakukan uji KIR ulang."
                            } else {
                                "Uji KIR unit ${armada.armadaId} (${armada.noPolisi}) akan jatuh tempo dalam $sisaDays hari lagi ($kir). Segera jadwalkan uji KIR!"
                            }
                            NotificationHelper.sendNotification(getApplication(), title, msg)
                        }
                    } else if (sisaDays != null && sisaDays > 30) {
                        notifiedKirArmadas.remove(armada.armadaId)
                    }
                }
            }
        }
    }

    fun checkServiceDistanceAndNotify(armadaList: List<ArmadaEntity>) {
        viewModelScope.launch {
            armadaList.forEach { armada ->
                val sisa = armada.sisaKm
                if (sisa < 1000) {
                    if (!notifiedServiceArmadas.contains(armada.armadaId)) {
                        notifiedServiceArmadas.add(armada.armadaId)
                        val title = "🚨 Peringatan Servis Unit ${armada.armadaId}"
                        val msg = if (sisa < 0) {
                            "Unit ${armada.armadaId} (${armada.noPolisi}) telah melampaui batas servis sejauh ${-sisa} KM! Harap segera lakukan servis di bengkel."
                        } else {
                            "Unit ${armada.armadaId} (${armada.noPolisi}) sisa KM servis kurang dari 1.000 KM (${sisa} KM tersisa). Harap segera dijadwalkan servis!"
                        }
                        NotificationHelper.sendNotification(
                            getApplication(),
                            title,
                            msg
                        )
                    }
                } else {
                    // Reset flag once vehicle is serviced and sisaKm >= 1000 KM
                    notifiedServiceArmadas.remove(armada.armadaId)
                }
            }
        }
    }

    private val notifiedAkiArmadas = mutableSetOf<String>()

    fun checkAkiAgeAndNotify(banList: List<BanEntity>) {
        viewModelScope.launch {
            val akiItems = banList.filter { it.posisi.trim().uppercase() == "AKI" }
            akiItems.forEach { aki ->
                val akiResult = com.example.utils.AkiUtils.calculateAkiStatus(
                    armadaId = aki.armadaId,
                    noPolisi = aki.noPolisi,
                    barcode = aki.barcode,
                    tanggalPasangStr = aki.kondisi,
                    userStatus = aki.keterangan,
                    merk = aki.merk
                )

                if (akiResult.isDue) {
                    if (!notifiedAkiArmadas.contains(aki.armadaId)) {
                        notifiedAkiArmadas.add(aki.armadaId)
                        val title = "🔋 Peringatan Usia Aki Unit ${aki.armadaId}"
                        val msg = if (akiResult.isExpired) {
                            "Aki Unit ${aki.armadaId} (${aki.noPolisi}) telah melebihi usia 2 tahun (Pasang: ${akiResult.tanggalPasang}). Segera lakukan penggantian Aki!"
                        } else {
                            "Aki Unit ${aki.armadaId} (${aki.noPolisi}) mendekati 2 tahun (Sisa ${akiResult.sisaHari} hari lagi hingga ${akiResult.tanggalGantiBerikutnya}). Harap jadwalkan penggantian Aki!"
                        }
                        com.example.utils.NotificationHelper.sendNotification(
                            getApplication(),
                            title,
                            msg
                        )
                    }
                } else {
                    notifiedAkiArmadas.remove(aki.armadaId)
                }
            }
        }
    }

    fun updateBan(
        armadaId: String,
        posisi: String,
        barcode: String,
        tahun: String,
        kodeBan: String,
        tanggalUpdate: String
    ) {
        viewModelScope.launch {
            _isBanUpdating.value = true
            _banUpdateStatus.value = "Sedang memperbarui data ban..."
            val result = repository.updateBan(armadaId, posisi, barcode, tahun, kodeBan, tanggalUpdate)
            when (result) {
                is UpdateBanResult.Success -> {
                    _banUpdateStatus.value = "Sukses: ${result.message}"
                    refreshMetadata()
                }
                is UpdateBanResult.Error -> {
                    _banUpdateStatus.value = "Error: ${result.message}"
                }
            }
            _isBanUpdating.value = false
        }
    }

    fun updateAki(
        armadaId: String,
        barcode: String,
        tanggalPasang: String,
        merk: String,
        status: String
    ) {
        viewModelScope.launch {
            _isBanUpdating.value = true
            _banUpdateStatus.value = "Sedang memperbarui data Aki..."
            val result = repository.updateAki(
                armadaId = armadaId,
                barcode = barcode,
                tanggalPasang = tanggalPasang,
                merk = merk,
                status = status
            )
            when (result) {
                is UpdateBanResult.Success -> {
                    _banUpdateStatus.value = "Sukses: ${result.message}"
                    refreshMetadata()
                }
                is UpdateBanResult.Error -> {
                    _banUpdateStatus.value = "Error: ${result.message}"
                }
            }
            _isBanUpdating.value = false
        }
    }

    fun clearBanUpdateStatus() {
        _banUpdateStatus.value = null
    }

    fun submitArsipPengiriman(
        noDokumen: String,
        noReceive: String,
        driverName: String,
        mediaFiles: List<TerkirimMediaFile>,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = repository.submitArsipPengiriman(noDokumen, noReceive, driverName, mediaFiles)
                _toastMessage.value = res.message ?: "Arsip pengiriman berhasil disimpan"
                onResult(res.success, res.message ?: "Berhasil simpan arsip")
            } catch (e: Exception) {
                _toastMessage.value = "Gagal simpan arsip: ${e.localizedMessage}"
                onResult(false, e.localizedMessage ?: "Terjadi kesalahan")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============================================
    // PENGAJUAN (AKSESORIS & BAN)
    // ============================================
    val pengajuanList: StateFlow<List<PengajuanEntity>> = repository.localPengajuan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPengajuanSubmitting = MutableStateFlow(false)
    val isPengajuanSubmitting: StateFlow<Boolean> = _isPengajuanSubmitting.asStateFlow()

    private val _pengajuanStatusMessage = MutableStateFlow<String?>(null)
    val pengajuanStatusMessage: StateFlow<String?> = _pengajuanStatusMessage.asStateFlow()

    fun clearPengajuanStatusMessage() {
        _pengajuanStatusMessage.value = null
    }

    fun refreshPengajuan() {
        viewModelScope.launch {
            repository.getPengajuanList()
        }
    }

    fun submitPengajuan(
        armadaId: String,
        noPolisi: String,
        kategori: String,
        detail: String,
        catatan: String,
        mediaFiles: List<MediaFileItem>,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isPengajuanSubmitting.value = true
            _pengajuanStatusMessage.value = "Sedang mengirim pengajuan $kategori..."
            val driver = loggedInDriverName.value.ifEmpty { "Driver" }
            val result = repository.submitPengajuan(
                driver = driver,
                armadaId = armadaId,
                noPolisi = noPolisi,
                kategori = kategori,
                detail = detail,
                catatan = catatan,
                files = mediaFiles
            )
            when (result) {
                is SubmitPengajuanResult.Success -> {
                    val msg = "Sukses: ${result.message} (No: ${result.noPengajuan})"
                    _pengajuanStatusMessage.value = msg
                    _toastMessage.value = "Pengajuan $kategori Berhasil!"
                    refreshPengajuan()
                    onResult(true, msg)
                }
                is SubmitPengajuanResult.Error -> {
                    val msg = "Gagal: ${result.message}"
                    _pengajuanStatusMessage.value = msg
                    _toastMessage.value = msg
                    onResult(false, msg)
                }
            }
            _isPengajuanSubmitting.value = false
        }
    }
}
