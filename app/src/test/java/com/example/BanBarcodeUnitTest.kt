package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
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
class BanBarcodeUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var banDao: BanDao
    private lateinit var armadaDao: ArmadaDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        banDao = db.banDao()
        armadaDao = db.armadaDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testBanDaoUpdateTireInfoSelectiveUpdate() = runBlocking {
        // Setup 2 tires for HK01
        val initialTires = listOf(
            BanEntity(
                id = 0,
                armadaId = "HK01",
                noPolisi = "W 8795 PV",
                posisi = "Depan Kiri (FL)",
                noSeri = "OLD-FL-SERI",
                ukuran = "7.50-16",
                merk = "Bridgestone",
                kondisi = "Tebal",
                tekanan = "105 Psi",
                keterangan = "Aman",
                barcode = "OLD-FL-BARCODE",
                tahun = "2023",
                kodeBan = "OLD-FL-CODE",
                tanggalUpdate = "2023-01-01"
            ),
            BanEntity(
                id = 0,
                armadaId = "HK01",
                noPolisi = "W 8795 PV",
                posisi = "Depan Kanan (FR)",
                noSeri = "OLD-FR-SERI",
                ukuran = "7.50-16",
                merk = "Bridgestone",
                kondisi = "Tebal",
                tekanan = "105 Psi",
                keterangan = "Aman",
                barcode = "OLD-FR-BARCODE",
                tahun = "2023",
                kodeBan = "OLD-FR-CODE",
                tanggalUpdate = "2023-01-01"
            )
        )
        banDao.insertAllBan(initialTires)

        // Perform updateTireInfo for Depan Kiri (FL) only
        val rowsAffected = banDao.updateTireInfo(
            armadaId = "HK01",
            posisi = "Depan Kiri (FL)",
            barcode = "NEW-BARCODE-G",
            tahun = "2026",
            kodeBan = "NEW-CODE-G",
            tanggalUpdate = "2026-08-27 12:00:00"
        )

        assertEquals(1, rowsAffected)

        val allTires = banDao.getAllBan().first()
        val flTire = allTires.find { it.posisi == "Depan Kiri (FL)" }
        val frTire = allTires.find { it.posisi == "Depan Kanan (FR)" }

        assertNotNull(flTire)
        assertNotNull(frTire)

        // Verify FL tire was updated
        assertEquals("NEW-BARCODE-G", flTire?.barcode)
        assertEquals("NEW-BARCODE-G", flTire?.noSeri)
        assertEquals("NEW-CODE-G", flTire?.kodeBan)
        assertEquals("2026", flTire?.tahun)
        assertEquals("2026-08-27 12:00:00", flTire?.tanggalUpdate)
        // Verify non-target columns (merk, ukuran, kondisi) remained intact
        assertEquals("Bridgestone", flTire?.merk)
        assertEquals("7.50-16", flTire?.ukuran)
        assertEquals("Tebal", flTire?.kondisi)

        // Verify FR tire remained completely unchanged
        assertEquals("OLD-FR-BARCODE", frTire?.barcode)
        assertEquals("2023", frTire?.tahun)
    }

    @Test
    fun testBanApiItemBarcodeResolution() {
        // Test case 1: Standard layout where Barcode is in column G (item.barcode populated)
        val itemColG = BanApiItem(
            armadaId = "HK01",
            noPolisi = "W8795PV",
            posisi = "Depan Kiri (FL)",
            noSeri = "12345",
            codeBan = "CB-001",
            barcode = "BARCODE-COL-G",
            tahun = "2026"
        )

        val resolvedBarcodeG = listOfNotNull(
            itemColG.noSeri?.takeIf { it.isNotBlank() },
            itemColG.codeBan?.takeIf { it.isNotBlank() },
            itemColG.barcode?.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: ""

        assertEquals("12345", resolvedBarcodeG)

        // Test case 2: Legacy layout where noSeri and codeBan are blank, but barcode column has data
        val itemLegacyColK = BanApiItem(
            armadaId = "HK02",
            noPolisi = "A8653ZU",
            posisi = "Belakang Kiri (RL)",
            noSeri = "",
            codeBan = "",
            barcode = "LEGACY-BARCODE-K",
            tahun = "2024"
        )

        val resolvedBarcodeK = listOfNotNull(
            itemLegacyColK.noSeri?.takeIf { it.isNotBlank() },
            itemLegacyColK.codeBan?.takeIf { it.isNotBlank() },
            itemLegacyColK.barcode?.takeIf { it.isNotBlank() }
        ).firstOrNull() ?: ""

        assertEquals("LEGACY-BARCODE-K", resolvedBarcodeK)
    }
}
