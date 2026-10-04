package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.AppRole
import com.example.ui.MenuItemId
import com.example.ui.RolePermissionMatrix
import com.example.utils.ServiceAcUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ServiceAcComprehensiveTest {

    private lateinit var db: AppDatabase
    private lateinit var serviceAcDao: ServiceAcDao
    private lateinit var armadaDao: ArmadaDao
    private lateinit var context: Context
    private lateinit var prefs: PreferenceManager
    private lateinit var repository: FleetRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        serviceAcDao = db.serviceAcDao()
        armadaDao = db.armadaDao()
        prefs = PreferenceManager(context)
        prefs.isGoogleSheetsMode = false
        repository = FleetRepository(context, db, prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // 1-7. UJI RBAC & MENU SERVICE AC (PER-ROLE & ROUTE GUARD)
    // =========================================================================

    @Test
    fun test01_DriverMelihatMenuServiceAC() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("DRIVER")
        val menuIds = menuItems.map { it.id }
        assertTrue("Role DRIVER harus memiliki menu SERVICE_AC", menuIds.contains(MenuItemId.SERVICE_AC))
        
        val serviceAcItem = menuItems.find { it.id == MenuItemId.SERVICE_AC }
        assertNotNull(serviceAcItem)
        assertEquals("Service AC", serviceAcItem?.title)
        assertEquals("Catat KM service AC armada", serviceAcItem?.description)
        assertEquals("service_ac", serviceAcItem?.destinationScreen)
    }

    @Test
    fun test02_StaffTidakMelihatServiceAC() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("STAFF")
        val menuIds = menuItems.map { it.id }
        assertFalse("Role STAFF TIDAK boleh melihat menu SERVICE_AC", menuIds.contains(MenuItemId.SERVICE_AC))
    }

    @Test
    fun test03_KenekTidakMelihatServiceAC() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("KENEK")
        val menuIds = menuItems.map { it.id }
        assertFalse("Role KENEK TIDAK boleh melihat menu SERVICE_AC", menuIds.contains(MenuItemId.SERVICE_AC))
    }

    @Test
    fun test04_LPTidakMelihatServiceAC() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("LP")
        val menuIds = menuItems.map { it.id }
        assertFalse("Role LP TIDAK boleh melihat menu SERVICE_AC", menuIds.contains(MenuItemId.SERVICE_AC))
    }

    @Test
    fun test05_UnknownRoleTidakMelihatServiceAC() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("UNKNOWN")
        val menuIds = menuItems.map { it.id }
        assertFalse("Role UNKNOWN TIDAK boleh melihat menu SERVICE_AC", menuIds.contains(MenuItemId.SERVICE_AC))
    }

    @Test
    fun test06_RoleKosongAtauNullTidakMelihatServiceAC() {
        val menuItemsEmpty = RolePermissionMatrix.getAllowedMenuItems("")
        val menuIdsEmpty = menuItemsEmpty.map { it.id }
        assertFalse("Role kosong TIDAK boleh melihat menu SERVICE_AC", menuIdsEmpty.contains(MenuItemId.SERVICE_AC))

        val menuItemsNull = RolePermissionMatrix.getAllowedMenuItems(null)
        val menuIdsNull = menuItemsNull.map { it.id }
        assertFalse("Role null TIDAK boleh melihat menu SERVICE_AC", menuIdsNull.contains(MenuItemId.SERVICE_AC))
    }

    @Test
    fun test07_RouteServiceAcHanyaDapatDibukaDriver() {
        assertTrue("Route service_ac harus diizinkan untuk DRIVER", RolePermissionMatrix.isScreenAllowed("DRIVER", "service_ac"))
        assertFalse("Route service_ac harus ditolak untuk STAFF", RolePermissionMatrix.isScreenAllowed("STAFF", "service_ac"))
        assertFalse("Route service_ac harus ditolak untuk KENEK", RolePermissionMatrix.isScreenAllowed("KENEK", "service_ac"))
        assertFalse("Route service_ac harus ditolak untuk LP", RolePermissionMatrix.isScreenAllowed("LP", "service_ac"))
        assertFalse("Route service_ac harus ditolak untuk UNKNOWN", RolePermissionMatrix.isScreenAllowed("UNKNOWN", "service_ac"))
        assertFalse("Route service_ac harus ditolak untuk role kosong", RolePermissionMatrix.isScreenAllowed("", "service_ac"))
        assertFalse("Route service_ac harus ditolak untuk role null", RolePermissionMatrix.isScreenAllowed(null, "service_ac"))
    }

    // =========================================================================
    // 8-10. UJI DATA ARMADA MASTER (ARMADA ID, NO POLISI, KM SAAT INI)
    // =========================================================================

    @Test
    fun test08_09_10_MasterArmadaLookup() = runBlocking {
        val armadaMaster = listOf(
            ArmadaEntity(
                armadaId = "HK01",
                noPolisi = "W8795PV",
                kmSaatIni = 338529,
                kmServiceTerakhir = 330000,
                intervalService = 5000,
                kmServiceBerikutnya = 335000,
                sisaKm = -3529,
                status = "🚨 HARUS SERVICE",
                flag = "",
                fotoKm = "",
                catattan = ""
            ),
            ArmadaEntity(
                armadaId = "HK02",
                noPolisi = "A8653ZU",
                kmSaatIni = 145200,
                kmServiceTerakhir = 140000,
                intervalService = 5000,
                kmServiceBerikutnya = 145000,
                sisaKm = -200,
                status = "🚨 HARUS SERVICE",
                flag = "",
                fotoKm = "",
                catattan = ""
            )
        )
        armadaDao.insertArmada(armadaMaster)

        val fetchedArmada = armadaDao.getArmadaById("HK01")
        assertNotNull("Armada HK01 harus ada di database master", fetchedArmada)
        assertEquals("HK01", fetchedArmada?.armadaId)
        assertEquals("W8795PV", fetchedArmada?.noPolisi)
        assertEquals(338529, fetchedArmada?.kmSaatIni)
    }

    // =========================================================================
    // 11-13. UJI VALIDASI INPUT KM SERVICE AC (WAJIB, POSITIF, TIDAK NOL/NEGATIF)
    // =========================================================================

    @Test
    fun test11_12_13_ValidasiKmServiceAc() = runBlocking {
        // Armada ID kosong
        val resBlankArmada = repository.submitServiceAc("", 338500)
        assertTrue(resBlankArmada.isFailure)

        // KM Nol
        val resZeroKm = repository.submitServiceAc("HK01", 0)
        assertTrue(resZeroKm.isFailure)

        // KM Negatif
        val resNegativeKm = repository.submitServiceAc("HK01", -500)
        assertTrue(resNegativeKm.isFailure)

        // KM Positif Valid
        val resValid = repository.submitServiceAc("HK01", 338500)
        assertTrue(resValid.isSuccess)
    }

    // =========================================================================
    // 14-16. UJI INSERT & UPDATE TAB SERVICE AC (SINGLE ROW PER ARMADA, NO DUPLICATION)
    // =========================================================================

    @Test
    fun test14_15_16_SingleRowPerArmadaNoDuplication() = runBlocking {
        armadaDao.insertArmada(listOf(
            ArmadaEntity(
                armadaId = "HK01",
                noPolisi = "W8795PV",
                kmSaatIni = 338529,
                kmServiceTerakhir = 330000,
                intervalService = 5000,
                kmServiceBerikutnya = 335000,
                sisaKm = 0,
                status = "AMAN",
                flag = "",
                fotoKm = "",
                catattan = ""
            )
        ))

        // First submit: Baru 1 baris
        val res1 = repository.submitServiceAc("HK01", 338500)
        assertTrue(res1.isSuccess)

        val listAfterFirst = serviceAcDao.getAllServiceAc().first()
        assertEquals(1, listAfterFirst.size)
        assertEquals("HK01", listAfterFirst[0].armadaId)
        assertEquals("W8795PV", listAfterFirst[0].noPolisi)
        assertEquals(338500, listAfterFirst[0].kmServiceTerakhir)

        // Second submit: Memperbarui baris yang sama untuk HK01 (tidak membuat baris duplikat)
        val res2 = repository.submitServiceAc("HK01", 340000)
        assertTrue(res2.isSuccess)

        val listAfterSecond = serviceAcDao.getAllServiceAc().first()
        assertEquals("Jumlah record untuk armada HK01 harus tetap 1 (tidak duplikasi)", 1, listAfterSecond.size)
        assertEquals("HK01", listAfterSecond[0].armadaId)
        assertEquals(340000, listAfterSecond[0].kmServiceTerakhir)
        assertEquals(350000, listAfterSecond[0].kmServiceBerikutnya) // 340000 + 10000
    }

    // =========================================================================
    // 17-24. UJI RUMUS MATEMATIS, TANGGAL EDATE, SISA KM & STATUS
    // =========================================================================

    @Test
    fun test17_18_19_20_21_PerhitunganKalkulasiServiceAc() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val refDate = sdf.parse("2026-10-04")!!

        val calc = ServiceAcUtils.calculateServiceAc(
            armadaId = "HK01",
            noPolisi = "W8795PV",
            kmSaatIni = 338529,
            tglServiceTerakhir = "2026-10-04",
            kmServiceTerakhir = 338500,
            intervalBulan = 6,
            intervalKm = 10000,
            referenceDate = refDate
        )

        // 17. Interval Bulan = 6
        assertEquals(6, calc.intervalBulan)
        assertEquals("6 BULAN", calc.jadwalService)

        // 18. Interval KM = 10000
        assertEquals(10000, calc.intervalKm)

        // 19. Tanggal Berikutnya = 2026-10-04 + 6 bulan = 2027-04-04
        assertEquals("2027-04-04", calc.serviceAcBerikutnya)

        // 20. KM Berikutnya = 338500 + 10000 = 348500
        assertEquals(348500, calc.kmServiceBerikutnya)

        // 21. Sisa KM = 348500 - 338529 = 9971
        assertEquals(9971, calc.sisaKm)

        // 22. Status NORMAL (sisa KM 9971 > 1000 dan sisa hari > 30)
        assertEquals("NORMAL", calc.status)
    }

    @Test
    fun test23_StatusSegeraServisBerdasarkanSisaKm() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val refDate = sdf.parse("2026-10-04")!!

        // KM Service: 100.000, Target Berikutnya: 110.000.
        // KM Saat Ini: 109.200 -> Sisa KM = 800 KM (<= 1000 KM) -> SEGERA SERVIS
        val calc = ServiceAcUtils.calculateServiceAc(
            armadaId = "HK02",
            noPolisi = "A8653ZU",
            kmSaatIni = 109200,
            tglServiceTerakhir = "2026-10-04",
            kmServiceTerakhir = 100000,
            intervalBulan = 6,
            intervalKm = 10000,
            referenceDate = refDate
        )

        assertEquals("SEGERA SERVIS", calc.status)
        assertEquals(800, calc.sisaKm)
    }

    @Test
    fun test23_StatusSegeraServisBerdasarkanTanggalKurang30Hari() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        // Tanggal Service 2026-04-04 -> Target: 2026-10-04.
        // Reference Date hari ini 2026-09-20 (14 hari sebelum jatuh tempo <= 30 hari)
        val refDate = sdf.parse("2026-09-20")!!

        val calc = ServiceAcUtils.calculateServiceAc(
            armadaId = "HK03",
            noPolisi = "W8649QK",
            kmSaatIni = 52000, // Sisa KM masih 8.000
            tglServiceTerakhir = "2026-04-04",
            kmServiceTerakhir = 50000,
            intervalBulan = 6,
            intervalKm = 10000,
            referenceDate = refDate
        )

        assertEquals("SEGERA SERVIS", calc.status)
    }

    @Test
    fun test24_StatusJatuhTempoBerdasarkanKmLewat() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val refDate = sdf.parse("2026-10-04")!!

        // KM Service: 100.000, Target Berikutnya: 110.000.
        // KM Saat Ini: 110.500 (kmSaatIni >= kmServiceBerikutnya) -> JATUH TEMPO
        val calc = ServiceAcUtils.calculateServiceAc(
            armadaId = "HK04",
            noPolisi = "A8190ZV",
            kmSaatIni = 110500,
            tglServiceTerakhir = "2026-10-04",
            kmServiceTerakhir = 100000,
            intervalBulan = 6,
            intervalKm = 10000,
            referenceDate = refDate
        )

        assertEquals("JATUH TEMPO", calc.status)
        assertTrue(calc.sisaKm <= 0)
    }

    @Test
    fun test24_StatusJatuhTempoBerdasarkanTanggalLewat() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        // Tanggal Service: 2026-01-01 -> Target Berikutnya: 2026-07-01
        // Reference Date: 2026-10-04 (sudah melewati 2026-07-01) -> JATUH TEMPO
        val refDate = sdf.parse("2026-10-04")!!

        val calc = ServiceAcUtils.calculateServiceAc(
            armadaId = "HK05",
            noPolisi = "AG 1234 XY",
            kmSaatIni = 25000, // KM masih rendah
            tglServiceTerakhir = "2026-01-01",
            kmServiceTerakhir = 20000,
            intervalBulan = 6,
            intervalKm = 10000,
            referenceDate = refDate
        )

        assertEquals("JATUH TEMPO", calc.status)
    }

    // =========================================================================
    // 25-28. UJI MENU LAMA PER-ROLE TIDAK BERUBAH
    // =========================================================================

    @Test
    fun test25_26_27_28_MenuLegacyPreserved() {
        // Driver legacy menus preserved: Status armada, Catatan driver, Pengajuan Ban/aks, Keluar + Service AC
        val driverMenu = RolePermissionMatrix.getAllowedMenuItems("DRIVER").map { it.id }
        assertTrue(driverMenu.contains(MenuItemId.STATUS_ARMADA))
        assertTrue(driverMenu.contains(MenuItemId.CATATAN_DRIVER))
        assertTrue(driverMenu.contains(MenuItemId.PENGAJUAN_BAN))
        assertTrue(driverMenu.contains(MenuItemId.SERVICE_AC))
        assertTrue(driverMenu.contains(MenuItemId.KELUAR))
        assertEquals(5, driverMenu.size)

        // Staff menu tidak berubah
        val staffMenu = RolePermissionMatrix.getAllowedMenuItems("STAFF").map { it.id }
        assertEquals(listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.CATAT_SERVICE,
            MenuItemId.LAPORAN,
            MenuItemId.PENGATURAN,
            MenuItemId.KELUAR
        ), staffMenu)

        // Kenek menu tidak berubah
        val kenekMenu = RolePermissionMatrix.getAllowedMenuItems("KENEK").map { it.id }
        assertEquals(listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.LOG_HARIAN,
            MenuItemId.ARSIP_PENGIRIMAN,
            MenuItemId.KELUAR
        ), kenekMenu)

        // LP menu tidak berubah
        val lpMenu = RolePermissionMatrix.getAllowedMenuItems("LP").map { it.id }
        assertEquals(listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.KELUAR
        ), lpMenu)
    }
}
