package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.MenuItemId
import com.example.ui.RolePermissionMatrix
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
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
class UsersLoginMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var prefs: PreferenceManager
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Data aktual tab Users dari Google Sheets
    data class UserRow(
        val userId: String,
        val nama: String,
        val jabatan: String,
        val pin: String,
        val noHp: String,
        val status: String
    )

    private val actualUsersTable = listOf(
        UserRow("094723", "BEBE", "STAFF", "094723", "", "AKTIF"),
        UserRow("098594", "BONO", "STAFF", "098594", "", "AKTIF"),
        UserRow("189924", "DECKY", "STAFF", "1", "", "AKTIF"),
        UserRow("E01762", "HAMIK", "DRIVER", "01762", "", "AKTIF"),
        UserRow("E05222", "ADAM", "DRIVER", "05222", "", "AKTIF"),
        UserRow("E02459", "BILLY", "DRIVER", "02459", "", "AKTIF"),
        UserRow("E02242", "RIVALDI", "DRIVER", "02242", "", "AKTIF"),
        UserRow("E05777", "ABRILIAN", "DRIVER", "05777", "", "AKTIF"),
        UserRow("E05198", "RIZAL", "KENEK", "05198", "", "AKTIF"),
        UserRow("E02667", "ADI", "KENEK", "02667", "", "AKTIF"),
        UserRow("E02462", "WAHYU", "KENEK", "02462", "", "AKTIF"),
        UserRow("E04668", "REZZA", "KENEK", "04668", "", "AKTIF"),
        UserRow("E03862", "AVRINO", "KENEK", "03862", "", "AKTIF"),
        UserRow("E04975", "AGUS", "KENEK", "04975", "", "AKTIF"),
        UserRow("E02464", "NICO ", "LP", "02464", "", "AKTIF"),
        UserRow("E06628", "DEO ", "LP", "06628", "", "AKTIF"),
        UserRow("E06634", "RIO ", "LP", "06634", "", "AKTIF")
    )

    // Simulasi logika validateLogin Apps Script pada tab Users
    private fun simulateAppsScriptValidateLogin(
        inputIdentifier: String,
        inputPin: String,
        usersTable: List<UserRow> = actualUsersTable
    ): LoginApiResponse {
        val targets = listOf(inputIdentifier.trim())
        val pin = inputPin.trim()

        if (targets.isEmpty() || pin.isEmpty()) {
            return LoginApiResponse(
                success = false,
                driverId = null,
                driverName = null,
                role = null,
                jabatan = null,
                message = "ID Driver dan PIN wajib diisi."
            )
        }

        for (user in usersTable) {
            val idVal = user.userId.trim()
            val nameVal = user.nama.trim()
            val jabatanVal = user.jabatan.trim()
            val statusVal = user.status.trim().uppercase()
            val pinVal = user.pin.trim()

            val cleanId = idVal.lowercase().replace(" ", "").replace("-", "")
            val cleanName = nameVal.lowercase().replace(" ", "").replace("-", "")

            var isMatch = false
            for (tgt in targets) {
                val cleanTgt = tgt.lowercase().replace(" ", "").replace("-", "")
                if (cleanId.isNotEmpty() && cleanId == cleanTgt) {
                    isMatch = true
                    break
                }
                if (cleanName.isNotEmpty() && cleanName == cleanTgt) {
                    isMatch = true
                    break
                }
                if (idVal.equals(tgt, ignoreCase = true)) {
                    isMatch = true
                    break
                }
                if (nameVal.equals(tgt, ignoreCase = true)) {
                    isMatch = true
                    break
                }
            }

            if (isMatch) {
                if (statusVal != "AKTIF") {
                    return LoginApiResponse(
                        success = false,
                        driverId = idVal,
                        driverName = nameVal,
                        role = null,
                        jabatan = null,
                        message = "Akun tidak aktif."
                    )
                }

                if (pinVal != pin) {
                    return LoginApiResponse(
                        success = false,
                        driverId = idVal,
                        driverName = nameVal,
                        role = null,
                        jabatan = null,
                        message = "PIN Keamanan salah."
                    )
                }

                val roleOutput = jabatanVal.uppercase()
                return LoginApiResponse(
                    success = true,
                    driverId = idVal,
                    driverName = nameVal,
                    role = roleOutput,
                    jabatan = roleOutput,
                    message = "Login Berhasil"
                )
            }
        }

        return LoginApiResponse(
            success = false,
            driverId = null,
            driverName = null,
            role = null,
            jabatan = null,
            message = "ID Driver atau Nama '${targets[0]}' tidak terdaftar di sheet."
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = PreferenceManager(context)
        prefs.clearLogin()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    // ========================================================
    // 1. UJI 17 AKUN AKTUAL TAB USERS
    // ========================================================

    @Test
    fun testActualUsers_01_Bebe_Staff() {
        val res = simulateAppsScriptValidateLogin("094723", "094723")
        assertTrue(res.success)
        assertEquals("094723", res.driverId)
        assertEquals("BEBE", res.driverName)
        assertEquals("STAFF", res.role)
        assertEquals("STAFF", res.jabatan)
        assertEquals("Login Berhasil", res.message)
    }

    @Test
    fun testActualUsers_02_Bono_Staff() {
        val res = simulateAppsScriptValidateLogin("098594", "098594")
        assertTrue(res.success)
        assertEquals("098594", res.driverId)
        assertEquals("BONO", res.driverName)
        assertEquals("STAFF", res.role)
        assertEquals("STAFF", res.jabatan)
    }

    @Test
    fun testActualUsers_03_Decky_Staff() {
        val res = simulateAppsScriptValidateLogin("189924", "1")
        assertTrue(res.success)
        assertEquals("189924", res.driverId)
        assertEquals("DECKY", res.driverName)
        assertEquals("STAFF", res.role)
        assertEquals("STAFF", res.jabatan)
    }

    @Test
    fun testActualUsers_04_Hamik_Driver_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E01762", "01762")
        assertTrue(res.success)
        assertEquals("E01762", res.driverId)
        assertEquals("HAMIK", res.driverName)
        assertEquals("DRIVER", res.role)
        assertEquals("DRIVER", res.jabatan)
    }

    @Test
    fun testActualUsers_05_Adam_Driver_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E05222", "05222")
        assertTrue(res.success)
        assertEquals("E05222", res.driverId)
        assertEquals("ADAM", res.driverName)
        assertEquals("DRIVER", res.role)
        assertEquals("DRIVER", res.jabatan)
    }

    @Test
    fun testActualUsers_06_Billy_Driver_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E02459", "02459")
        assertTrue(res.success)
        assertEquals("E02459", res.driverId)
        assertEquals("BILLY", res.driverName)
        assertEquals("DRIVER", res.role)
        assertEquals("DRIVER", res.jabatan)
    }

    @Test
    fun testActualUsers_07_Rivaldi_Driver_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E02242", "02242")
        assertTrue(res.success)
        assertEquals("E02242", res.driverId)
        assertEquals("RIVALDI", res.driverName)
        assertEquals("DRIVER", res.role)
        assertEquals("DRIVER", res.jabatan)
    }

    @Test
    fun testActualUsers_08_Abrilian_Driver_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E05777", "05777")
        assertTrue(res.success)
        assertEquals("E05777", res.driverId)
        assertEquals("ABRILIAN", res.driverName)
        assertEquals("DRIVER", res.role)
        assertEquals("DRIVER", res.jabatan)
    }

    @Test
    fun testActualUsers_09_Rizal_Kenek_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E05198", "05198")
        assertTrue(res.success)
        assertEquals("E05198", res.driverId)
        assertEquals("RIZAL", res.driverName)
        assertEquals("KENEK", res.role)
        assertEquals("KENEK", res.jabatan)
    }

    @Test
    fun testActualUsers_10_Adi_Kenek_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E02667", "02667")
        assertTrue(res.success)
        assertEquals("E02667", res.driverId)
        assertEquals("ADI", res.driverName)
        assertEquals("KENEK", res.role)
        assertEquals("KENEK", res.jabatan)
    }

    @Test
    fun testActualUsers_11_Wahyu_Kenek_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E02462", "02462")
        assertTrue(res.success)
        assertEquals("E02462", res.driverId)
        assertEquals("WAHYU", res.driverName)
        assertEquals("KENEK", res.role)
        assertEquals("KENEK", res.jabatan)
    }

    @Test
    fun testActualUsers_12_Rezza_Kenek_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E04668", "04668")
        assertTrue(res.success)
        assertEquals("E04668", res.driverId)
        assertEquals("REZZA", res.driverName)
        assertEquals("KENEK", res.role)
        assertEquals("KENEK", res.jabatan)
    }

    @Test
    fun testActualUsers_13_Avrino_Kenek_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E03862", "03862")
        assertTrue(res.success)
        assertEquals("E03862", res.driverId)
        assertEquals("AVRINO", res.driverName)
        assertEquals("KENEK", res.role)
        assertEquals("KENEK", res.jabatan)
    }

    @Test
    fun testActualUsers_14_Agus_Kenek_LeadingZeroPin() {
        val res = simulateAppsScriptValidateLogin("E04975", "04975")
        assertTrue(res.success)
        assertEquals("E04975", res.driverId)
        assertEquals("AGUS", res.driverName)
        assertEquals("KENEK", res.role)
        assertEquals("KENEK", res.jabatan)
    }

    @Test
    fun testActualUsers_15_Nico_LP_LeadingZeroPin_TrailingSpaceTolerant() {
        val res = simulateAppsScriptValidateLogin("E02464", "02464")
        assertTrue(res.success)
        assertEquals("E02464", res.driverId)
        assertEquals("NICO", res.driverName)
        assertEquals("LP", res.role)
        assertEquals("LP", res.jabatan)
    }

    @Test
    fun testActualUsers_16_Deo_LP_LeadingZeroPin_TrailingSpaceTolerant() {
        val res = simulateAppsScriptValidateLogin("E06628", "06628")
        assertTrue(res.success)
        assertEquals("E06628", res.driverId)
        assertEquals("DEO", res.driverName)
        assertEquals("LP", res.role)
        assertEquals("LP", res.jabatan)
    }

    @Test
    fun testActualUsers_17_Rio_LP_LeadingZeroPin_TrailingSpaceTolerant() {
        val res = simulateAppsScriptValidateLogin("E06634", "06634")
        assertTrue(res.success)
        assertEquals("E06634", res.driverId)
        assertEquals("RIO", res.driverName)
        assertEquals("LP", res.role)
        assertEquals("LP", res.jabatan)
    }

    // ========================================================
    // 2. UJI KASUS VALIDASI & KEAMANAN
    // ========================================================

    @Test
    fun testPinSalahDitolak() {
        val res = simulateAppsScriptValidateLogin("E01762", "99999")
        assertFalse(res.success)
        assertEquals("E01762", res.driverId)
        assertEquals("HAMIK", res.driverName)
        assertNull(res.role)
        assertNull(res.jabatan)
        assertEquals("PIN Keamanan salah.", res.message)
    }

    @Test
    fun testUserIdTidakTerdaftarDitolak() {
        val res = simulateAppsScriptValidateLogin("UNKNOWN_USER", "1234")
        assertFalse(res.success)
        assertNull(res.driverId)
        assertNull(res.driverName)
        assertNull(res.role)
        assertNull(res.jabatan)
        assertTrue(res.message!!.contains("tidak terdaftar di sheet"))
    }

    @Test
    fun testAkunTidakAktifDitolak() {
        val inactiveTable = listOf(
            UserRow("E99999", "NONAKTIF_USER", "DRIVER", "12345", "", "NONAKTIF")
        )
        val res = simulateAppsScriptValidateLogin("E99999", "12345", inactiveTable)
        assertFalse(res.success)
        assertEquals("E99999", res.driverId)
        assertEquals("NONAKTIF_USER", res.driverName)
        assertNull(res.role)
        assertNull(res.jabatan)
        assertEquals("Akun tidak aktif.", res.message)
    }

    @Test
    fun testLoginMenggunakanNama() {
        // Hamik login dengan nama HAMIK
        val resHamik = simulateAppsScriptValidateLogin("HAMIK", "01762")
        assertTrue(resHamik.success)
        assertEquals("E01762", resHamik.driverId)
        assertEquals("HAMIK", resHamik.driverName)
        assertEquals("DRIVER", resHamik.role)

        // Decky login dengan nama DECKY
        val resDecky = simulateAppsScriptValidateLogin("DECKY", "1")
        assertTrue(resDecky.success)
        assertEquals("189924", resDecky.driverId)
        assertEquals("DECKY", resDecky.driverName)
        assertEquals("STAFF", resDecky.role)

        // Nico login dengan nama NICO (tanpa spasi di input)
        val resNico = simulateAppsScriptValidateLogin("NICO", "02464")
        assertTrue(resNico.success)
        assertEquals("E02464", resNico.driverId)
        assertEquals("NICO", resNico.driverName)
        assertEquals("LP", resNico.role)
    }

    @Test
    fun testLeadingZeroPinPreservedAsString() {
        // Memastikan PIN 01762 bukan 1762
        val resWrongLeadingZero = simulateAppsScriptValidateLogin("E01762", "1762")
        assertFalse(resWrongLeadingZero.success)
        assertEquals("PIN Keamanan salah.", resWrongLeadingZero.message)

        val resCorrect = simulateAppsScriptValidateLogin("E01762", "01762")
        assertTrue(resCorrect.success)
    }

    // ========================================================
    // 3. UJI PREFERENCEMANAGER & SESSION
    // ========================================================

    @Test
    fun testPreferenceManagerSaveAndClearRoleAndJabatan() {
        prefs.loggedInDriverName = "HAMIK"
        prefs.loggedInDriverId = "E01762"
        prefs.loggedInRole = "DRIVER"
        prefs.loggedInJabatan = "DRIVER"

        assertEquals("HAMIK", prefs.loggedInDriverName)
        assertEquals("E01762", prefs.loggedInDriverId)
        assertEquals("DRIVER", prefs.loggedInRole)
        assertEquals("DRIVER", prefs.loggedInJabatan)

        // Test clearLogin
        prefs.clearLogin()
        assertEquals("", prefs.loggedInDriverName)
        assertEquals("", prefs.loggedInDriverId)
        assertEquals("", prefs.loggedInRole)
        assertEquals("", prefs.loggedInJabatan)
    }

    // ========================================================
    // 4. UJI KOMPATIBILITAS KONTRAK JSON (APK LAMA & BARU)
    // ========================================================

    @Test
    fun testJsonSerialization_LegacyResponseWithoutRole() {
        // Respons lama dari backend tanpa role/jabatan
        val legacyJson = """
            {
                "success": true,
                "driverId": "E01762",
                "driverName": "HAMIK",
                "message": "Login Berhasil"
            }
        """.trimIndent()

        val adapter = moshi.adapter(LoginApiResponse::class.java)
        val parsed = adapter.fromJson(legacyJson)

        assertNotNull(parsed)
        assertTrue(parsed!!.success)
        assertEquals("E01762", parsed.driverId)
        assertEquals("HAMIK", parsed.driverName)
        assertNull(parsed.role)
        assertNull(parsed.jabatan)
        assertEquals("Login Berhasil", parsed.message)
    }

    @Test
    fun testJsonSerialization_NewResponseWithRoleAndJabatan() {
        val newJson = """
            {
                "success": true,
                "driverId": "094723",
                "driverName": "BEBE",
                "role": "STAFF",
                "jabatan": "STAFF",
                "message": "Login Berhasil"
            }
        """.trimIndent()

        val adapter = moshi.adapter(LoginApiResponse::class.java)
        val parsed = adapter.fromJson(newJson)

        assertNotNull(parsed)
        assertTrue(parsed!!.success)
        assertEquals("094723", parsed.driverId)
        assertEquals("BEBE", parsed.driverName)
        assertEquals("STAFF", parsed.role)
        assertEquals("STAFF", parsed.jabatan)
    }

    // ========================================================
    // 5. UJI INTEGRITAS FITUR OPERASIONAL (LOCAL PERSISTENCE)
    // ========================================================

    @Test
    fun testOperationalArmadaAndLogHarianIntact() = runBlocking {
        val armada = ArmadaEntity(
            armadaId = "HK01",
            noPolisi = "W8795PV",
            kmSaatIni = 50000,
            kmServiceTerakhir = 45000,
            intervalService = 5000,
            kmServiceBerikutnya = 50000,
            sisaKm = 0,
            status = "SERVIS SEKARANG",
            flag = "MERAH",
            fotoKm = "",
            catattan = ""
        )
        db.armadaDao().insertArmada(listOf(armada))

        val loadedArmada = db.armadaDao().getAllArmada().first()
        assertEquals(1, loadedArmada.size)
        assertEquals("HK01", loadedArmada[0].armadaId)

        val log = LogHarianEntity(
            tanggal = "03/10/2026 10:00:00",
            armadaId = "HK01",
            kmTerdeteksi = 50020,
            linkFoto = "https://example.com/foto.jpg",
            catatan = "Rute Kediri-Surabaya",
            namaDriver = "HAMIK",
            notaBbmUrl = "https://example.com/nota.jpg"
        )
        db.logHarianDao().insertLogs(listOf(log))

        val loadedLogs = db.logHarianDao().getAllLogs().first()
        assertEquals(1, loadedLogs.size)
        assertEquals("HAMIK", loadedLogs[0].namaDriver)
        assertEquals("https://example.com/nota.jpg", loadedLogs[0].notaBbmUrl)
    }

    // ========================================================
    // 6. UJI MATRIKS HAK AKSES ROLE (RBAC VERIFICATION)
    // ========================================================

    @Test
    fun testRolePermissionMatrix_Staff() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("STAFF")
        val menuIds = menuItems.map { it.id }

        // Sesuai matriks: Status armada, Catat service, laporan (+ pengaturan & keluar)
        assertTrue(menuIds.contains(MenuItemId.STATUS_ARMADA))
        assertTrue(menuIds.contains(MenuItemId.CATAT_SERVICE))
        assertTrue(menuIds.contains(MenuItemId.LAPORAN))
        assertTrue(menuIds.contains(MenuItemId.PENGATURAN))
        assertTrue(menuIds.contains(MenuItemId.KELUAR))

        // Tidak boleh memiliki menu khusus driver/kenek
        assertFalse(menuIds.contains(MenuItemId.LOG_HARIAN))
        assertFalse(menuIds.contains(MenuItemId.CATATAN_DRIVER))
        assertFalse(menuIds.contains(MenuItemId.PENGAJUAN_BAN))
        assertFalse(menuIds.contains(MenuItemId.SERVICE_AC))
        assertFalse(menuIds.contains(MenuItemId.ARSIP_PENGIRIMAN))

        // Screen guard check
        assertTrue(RolePermissionMatrix.isScreenAllowed("STAFF", "armada"))
        assertTrue(RolePermissionMatrix.isScreenAllowed("STAFF", "service"))
        assertTrue(RolePermissionMatrix.isScreenAllowed("STAFF", "settings"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("STAFF", "service_ac"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("STAFF", "form"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("STAFF", "pengajuan"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("STAFF", "arsip_pengiriman"))
    }

    @Test
    fun testRolePermissionMatrix_Driver() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("DRIVER")
        val menuIds = menuItems.map { it.id }

        // Sesuai matriks: Status Armada, Catatan driver, Pengajuan Ban/aks, Service AC (+ keluar)
        assertTrue(menuIds.contains(MenuItemId.STATUS_ARMADA))
        assertTrue(menuIds.contains(MenuItemId.CATATAN_DRIVER))
        assertTrue(menuIds.contains(MenuItemId.PENGAJUAN_BAN))
        assertTrue(menuIds.contains(MenuItemId.SERVICE_AC))
        assertTrue(menuIds.contains(MenuItemId.KELUAR))

        // Tidak boleh memiliki menu staff/kenek
        assertFalse(menuIds.contains(MenuItemId.CATAT_SERVICE))
        assertFalse(menuIds.contains(MenuItemId.LAPORAN))
        assertFalse(menuIds.contains(MenuItemId.LOG_HARIAN))
        assertFalse(menuIds.contains(MenuItemId.ARSIP_PENGIRIMAN))
        assertFalse(menuIds.contains(MenuItemId.PENGATURAN))

        // Screen guard check
        assertTrue(RolePermissionMatrix.isScreenAllowed("DRIVER", "armada"))
        assertTrue(RolePermissionMatrix.isScreenAllowed("DRIVER", "pengajuan"))
        assertTrue(RolePermissionMatrix.isScreenAllowed("DRIVER", "service_ac"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("DRIVER", "service"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("DRIVER", "form"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("DRIVER", "arsip_pengiriman"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("DRIVER", "settings"))
    }

    @Test
    fun testRolePermissionMatrix_Kenek() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("KENEK")
        val menuIds = menuItems.map { it.id }

        // Sesuai matriks: Status Armada, Log Harian, Arsip Pengiriman (+ keluar)
        assertTrue(menuIds.contains(MenuItemId.STATUS_ARMADA))
        assertTrue(menuIds.contains(MenuItemId.LOG_HARIAN))
        assertTrue(menuIds.contains(MenuItemId.ARSIP_PENGIRIMAN))
        assertTrue(menuIds.contains(MenuItemId.KELUAR))

        // Tidak boleh memiliki menu staff/driver
        assertFalse(menuIds.contains(MenuItemId.CATAT_SERVICE))
        assertFalse(menuIds.contains(MenuItemId.CATATAN_DRIVER))
        assertFalse(menuIds.contains(MenuItemId.PENGAJUAN_BAN))
        assertFalse(menuIds.contains(MenuItemId.SERVICE_AC))
        assertFalse(menuIds.contains(MenuItemId.LAPORAN))
        assertFalse(menuIds.contains(MenuItemId.PENGATURAN))

        // Screen guard check
        assertTrue(RolePermissionMatrix.isScreenAllowed("KENEK", "armada"))
        assertTrue(RolePermissionMatrix.isScreenAllowed("KENEK", "form"))
        assertTrue(RolePermissionMatrix.isScreenAllowed("KENEK", "arsip_pengiriman"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("KENEK", "service_ac"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("KENEK", "service"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("KENEK", "pengajuan"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("KENEK", "settings"))
    }

    @Test
    fun testRolePermissionMatrix_LP() {
        val menuItems = RolePermissionMatrix.getAllowedMenuItems("LP")
        val menuIds = menuItems.map { it.id }

        // Sesuai matriks: Status Armada (+ keluar)
        assertTrue(menuIds.contains(MenuItemId.STATUS_ARMADA))
        assertTrue(menuIds.contains(MenuItemId.KELUAR))
        assertEquals(2, menuIds.size)

        // Tidak boleh memiliki menu lain
        assertFalse(menuIds.contains(MenuItemId.CATAT_SERVICE))
        assertFalse(menuIds.contains(MenuItemId.CATATAN_DRIVER))
        assertFalse(menuIds.contains(MenuItemId.PENGAJUAN_BAN))
        assertFalse(menuIds.contains(MenuItemId.SERVICE_AC))
        assertFalse(menuIds.contains(MenuItemId.LOG_HARIAN))
        assertFalse(menuIds.contains(MenuItemId.ARSIP_PENGIRIMAN))
        assertFalse(menuIds.contains(MenuItemId.LAPORAN))
        assertFalse(menuIds.contains(MenuItemId.PENGATURAN))

        // Screen guard check
        assertTrue(RolePermissionMatrix.isScreenAllowed("LP", "armada"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("LP", "service_ac"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("LP", "service"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("LP", "form"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("LP", "pengajuan"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("LP", "arsip_pengiriman"))
        assertFalse(RolePermissionMatrix.isScreenAllowed("LP", "settings"))
    }
}
