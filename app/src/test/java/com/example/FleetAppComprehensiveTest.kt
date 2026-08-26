package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.utils.AkiUtils
import com.example.utils.CommonUtils
import com.example.utils.InputSanitizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FleetAppComprehensiveTest {

    private lateinit var db: AppDatabase
    private lateinit var driverDao: DriverDao
    private lateinit var armadaDao: ArmadaDao
    private lateinit var logHarianDao: LogHarianDao
    private lateinit var banDao: BanDao
    private lateinit var pengirimanDao: PengirimanDao
    private lateinit var catatanDriverDao: CatatanDriverDao
    private lateinit var pengajuanDao: PengajuanDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        driverDao = db.driverDao()
        armadaDao = db.armadaDao()
        logHarianDao = db.logHarianDao()
        banDao = db.banDao()
        pengirimanDao = db.pengirimanDao()
        catatanDriverDao = db.catatanDriverDao()
        pengajuanDao = db.pengajuanDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // ==========================================
    // 1. UJI UTILITY & SANITASI
    // ==========================================

    @Test
    fun testInputSanitization() {
        // Test XSS stripping & dangerous tags
        val maliciousInput = "<script>alert('hack');</script>  Unit HK01  "
        val clean = InputSanitizer.sanitizeText(maliciousInput)
        assertFalse(clean.contains("<script>"))
        assertTrue(clean.contains("Unit HK01"))

        // Test Numeric sanitizer
        val rawOdo = "KM 125.000 ABC"
        val numericOnly = InputSanitizer.sanitizeNumeric(rawOdo)
        assertEquals("125000", numericOnly)

        // Test Alphanumeric sanitizer
        val rawArmada = "HK-01 @#! Kediri"
        val alphaOnly = InputSanitizer.sanitizeAlphanumeric(rawArmada)
        assertEquals("HK01Kediri", alphaOnly)
    }

    @Test
    fun testCommonUtilsDriveUrlConversion() {
        // Google Drive sharing URL with ?id=
        val driveUrl1 = "https://drive.google.com/open?id=1AbCdEfGhIjKlMnOpQrStUv"
        val directUrl1 = CommonUtils.getDirectDriveImageUrl(driveUrl1)
        assertEquals("https://lh3.googleusercontent.com/d/1AbCdEfGhIjKlMnOpQrStUv", directUrl1)

        // Google Drive sharing URL with /d/
        val driveUrl2 = "https://drive.google.com/file/d/1XyZ987654321/view?usp=sharing"
        val directUrl2 = CommonUtils.getDirectDriveImageUrl(driveUrl2)
        assertEquals("https://lh3.googleusercontent.com/d/1XyZ987654321", directUrl2)

        // Normal non-drive URL remains intact
        val regularUrl = "https://example.com/images/truck.jpg"
        assertEquals(regularUrl, CommonUtils.getDirectDriveImageUrl(regularUrl))
    }

    @Test
    fun testCommonUtilsFormatKm() {
        val formatted = CommonUtils.formatKm(154200)
        assertTrue(formatted.contains("154") && formatted.contains("200"))
    }

    // ==========================================
    // 2. UJI LOGIKA STATUS AKI (AkiUtils)
    // ==========================================

    @Test
    fun testAkiStatusCalculation() {
        // Case 1: Fresh Aki (Installed today/recently)
        val resFresh = AkiUtils.calculateAkiStatus(
            armadaId = "HK01",
            noPolisi = "W8795PV",
            barcode = "0255KDR",
            tanggalPasangStr = "01/01/2026",
            userStatus = "AMAN"
        )
        assertNotNull(resFresh)
        assertEquals("HK01", resFresh.armadaId)
        assertFalse(resFresh.isExpired)

        // Case 2: Expired Aki (>2 Years Old, e.g., 2020)
        val resOld = AkiUtils.calculateAkiStatus(
            armadaId = "HK02",
            noPolisi = "A8653ZU",
            barcode = "0255KDR",
            tanggalPasangStr = "01/01/2020",
            userStatus = "AMAN"
        )
        assertTrue(resOld.isExpired)
        assertTrue(resOld.isDue)
        assertTrue(resOld.statusLabel.contains("GANTI") || resOld.statusLabel.contains("2 TAHUN"))

        // Case 3: Empty / Missing Date Fallback
        val resEmpty = AkiUtils.calculateAkiStatus(
            armadaId = "HK03",
            noPolisi = "W8649QK",
            barcode = "0255KDR",
            tanggalPasangStr = "-",
            userStatus = "AMAN"
        )
        assertEquals("-", resEmpty.tanggalPasang)
        assertFalse(resEmpty.isExpired)
    }

    // ==========================================
    // 3. UJI DATABASE LOKAL (ROOM DAOS)
    // ==========================================

    @Test
    fun testDriverDaoOperations() = runBlocking {
        val testDrivers = listOf(
            DriverEntity("D01", "Driver HUB 1", "1234"),
            DriverEntity("D02", "Driver HUB 2", "5678")
        )
        driverDao.insertDrivers(testDrivers)

        val retrieved = driverDao.getAllDrivers().first()
        assertEquals(2, retrieved.size)
        assertEquals("Driver HUB 1", retrieved[0].namaDriver)

        // Test delete by ID
        driverDao.deleteDriverById("D01")
        val remaining = driverDao.getAllDrivers().first()
        assertEquals(1, remaining.size)
        assertEquals("D02", remaining[0].idDriver)
    }

    @Test
    fun testArmadaDaoOperations() = runBlocking {
        val testArmada = listOf(
            ArmadaEntity(
                armadaId = "HK01",
                noPolisi = "W 8795 PV",
                kmSaatIni = 125000,
                kmServiceTerakhir = 120000,
                intervalService = 10000,
                kmServiceBerikutnya = 130000,
                sisaKm = 5000,
                status = "Aman",
                flag = "NORMAL",
                fotoKm = "",
                catattan = ""
            )
        )
        armadaDao.insertArmada(testArmada)

        val retrieved = armadaDao.getAllArmada().first()
        assertEquals(1, retrieved.size)
        assertEquals("HK01", retrieved[0].armadaId)
        assertEquals(5000, retrieved[0].sisaKm)

        // Test Single fetch by ID
        val single = armadaDao.getArmadaById("HK01")
        assertNotNull(single)
        assertEquals("W 8795 PV", single?.noPolisi)
    }

    @Test
    fun testLogHarianDaoOperations() = runBlocking {
        val testLog = LogHarianEntity(
            id = 0,
            tanggal = "19/08/2026",
            armadaId = "HK01",
            kmTerdeteksi = 125500,
            linkFoto = "",
            catatan = "Rute Kediri - Surabaya lancar",
            namaDriver = "Driver HUB 1"
        )
        logHarianDao.insertLog(testLog)

        val logs = logHarianDao.getAllLogs().first()
        assertEquals(1, logs.size)
        assertEquals("HK01", logs[0].armadaId)
        assertEquals(125500, logs[0].kmTerdeteksi)
    }

    @Test
    fun testBanDaoOperations() = runBlocking {
        val testBanList = listOf(
            BanEntity(
                id = 0,
                armadaId = "HK01",
                noPolisi = "W 8795 PV",
                posisi = "Depan Kiri (FL)",
                noSeri = "BS-FL-9281",
                ukuran = "7.50-16",
                merk = "Bridgestone",
                kondisi = "Tebal",
                tekanan = "105 Psi",
                keterangan = "Aman",
                barcode = "BS-FL-9281",
                tahun = "2024",
                kodeBan = "BS-FL-9281"
            ),
            BanEntity(
                id = 0,
                armadaId = "HK01",
                noPolisi = "W 8795 PV",
                posisi = "AKI",
                noSeri = "0255KDR",
                ukuran = "12V",
                merk = "GS Astra 12V",
                kondisi = "01/01/2026",
                tekanan = "GS Astra 12V",
                keterangan = "AMAN",
                barcode = "0255KDR",
                tahun = "2026"
            )
        )
        banDao.insertAllBan(testBanList)

        val allBan = banDao.getAllBan().first()
        assertEquals(2, allBan.size)

        // Test update tire info query
        val updated = banDao.updateTireInfo(
            armadaId = "HK01",
            posisi = "Depan Kiri (FL)",
            barcode = "BS-FL-9999",
            tahun = "2025",
            kodeBan = "BS-FL-9999",
            tanggalUpdate = "19/08/2026"
        )
        assertEquals(1, updated)

        val updatedList = banDao.getAllBan().first()
        val flItem = updatedList.find { it.posisi.contains("FL") }
        assertEquals("BS-FL-9999", flItem?.barcode)
    }

    @Test
    fun testPengirimanDaoOperations() = runBlocking {
        val testPengiriman = PengirimanEntity(
            id = 0,
            noDokumen = "DOK-TEST-001",
            noSuratJalan = "SJ/2026/001",
            tanggal = "19/08/2026",
            driver = "Driver HUB 1",
            armada = "HK01",
            gudangAsal = "Gudang Kediri",
            tujuan = "Surabaya",
            penerima = "PT Sejahtera",
            status = "Belum Berangkat"
        )
        pengirimanDao.insertPengiriman(testPengiriman)

        val list = pengirimanDao.getAllPengiriman().first()
        assertEquals(1, list.size)
        assertEquals("DOK-TEST-001", list[0].noDokumen)

        // Update status to Terkirim
        val toUpdate = list[0].copy(status = "TERKIRIM", catatan = "Barang diterima dengan baik")
        pengirimanDao.updatePengiriman(toUpdate)

        val updatedList = pengirimanDao.getAllPengiriman().first()
        assertEquals("TERKIRIM", updatedList[0].status)
        assertEquals("Barang diterima dengan baik", updatedList[0].catatan)
    }

    @Test
    fun testCatatanDriverAndPengajuanDao() = runBlocking {
        val catatan = CatatanDriverEntity(
            id = 0,
            tanggal = "19/08/2026",
            armadaId = "HK01",
            driverName = "Driver HUB 1",
            catatan = "Pengereman normal, AC dingin."
        )
        catatanDriverDao.insertCatatanDriver(catatan)
        val catatans = catatanDriverDao.getAllCatatanDriver().first()
        assertEquals(1, catatans.size)

        val pengajuan = PengajuanEntity(
            id = 0,
            noPengajuan = "PJ-001",
            tanggal = "19/08/2026",
            driver = "Driver HUB 1",
            armadaId = "HK01",
            noPolisi = "W 8795 PV",
            kategori = "Ban",
            detail = "Pengajuan ganti ban belakang",
            status = "PENDING"
        )
        pengajuanDao.insertPengajuan(pengajuan)
        val pengajuans = pengajuanDao.getAllPengajuan().first()
        assertEquals(1, pengajuans.size)
        assertEquals("PENDING", pengajuans[0].status)
    }

    // ==========================================
    // 3. UJI LOG MASUK DRIVER ID DEMI ID (1 PER 1)
    // ==========================================

    @Test
    fun testDriverLogin1By1() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferenceManager(context)
        prefs.isGoogleSheetsMode = false
        val repository = FleetRepository(context, db, prefs)

        // Reset database drivers to default set (D01 & D02)
        driverDao.deleteAllDrivers()
        driverDao.insertDrivers(
            listOf(
                DriverEntity("D01", "Driver HUB 1", "1234"),
                DriverEntity("D02", "Driver HUB 2", "5678")
            )
        )

        // --- ID 1: D01 ---
        // 1a. Test ID D01 with correct PIN (1234)
        val resD01Success = repository.validateLogin("D01", "1234")
        assertTrue(resD01Success is LoginResult.Success)
        assertEquals("Driver HUB 1", (resD01Success as LoginResult.Success).driverName)
        assertEquals("D01", resD01Success.driverId)

        // 1b. Test ID D01 with wrong PIN (9999)
        val resD01Fail = repository.validateLogin("D01", "9999")
        assertTrue(resD01Fail is LoginResult.Error)
        assertEquals("PIN Keamanan salah.", (resD01Fail as LoginResult.Error).message)

        // 1c. Test lowercase id "d01" with correct PIN (1234)
        val resD01Lower = repository.validateLogin("d01", "1234")
        assertTrue(resD01Lower is LoginResult.Success)

        // 1d. Test Full Name "Driver HUB 1" with correct PIN (1234)
        val resName01Success = repository.validateLogin("Driver HUB 1", "1234")
        assertTrue(resName01Success is LoginResult.Success)

        // --- ID 2: D02 ---
        // 2a. Test ID D02 with correct PIN (5678)
        val resD02Success = repository.validateLogin("D02", "5678")
        assertTrue(resD02Success is LoginResult.Success)
        assertEquals("Driver HUB 2", (resD02Success as LoginResult.Success).driverName)
        assertEquals("D02", resD02Success.driverId)

        // 2b. Test ID D02 with wrong PIN (1234)
        val resD02Fail = repository.validateLogin("D02", "1234")
        assertTrue(resD02Fail is LoginResult.Error)
        assertEquals("PIN Keamanan salah.", (resD02Fail as LoginResult.Error).message)

        // 2c. Test Full Name "Driver HUB 2" with correct PIN (5678)
        val resName02Success = repository.validateLogin("Driver HUB 2", "5678")
        assertTrue(resName02Success is LoginResult.Success)

        // --- ID 3: Unregistered ID (D99 / Random) ---
        val resUnregistered = repository.validateLogin("D99", "1234")
        assertTrue(resUnregistered is LoginResult.Error)
        assertEquals("ID Driver atau Nama tidak terdaftar di sistem.", (resUnregistered as LoginResult.Error).message)
    }
}
