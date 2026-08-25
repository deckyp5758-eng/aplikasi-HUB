package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DriverEntity::class, ArmadaEntity::class, LogHarianEntity::class, BanEntity::class, PengirimanEntity::class, CatatanDriverEntity::class, PengajuanEntity::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun driverDao(): DriverDao
    abstract fun armadaDao(): ArmadaDao
    abstract fun logHarianDao(): LogHarianDao
    abstract fun banDao(): BanDao
    abstract fun pengirimanDao(): PengirimanDao
    abstract fun catatanDriverDao(): CatatanDriverDao
    abstract fun pengajuanDao(): PengajuanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fleet_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // Populate Drivers
            val drivers = listOf(
                DriverEntity("D01", "Driver HUB 1", "1234"),
                DriverEntity("D02", "Driver HUB 2", "5678")
            )
            db.driverDao().insertDrivers(drivers)

            // Populate Armada
            val armada = listOf(
                ArmadaEntity(
                    armadaId = "HK01",
                    noPolisi = "W8795PV",
                    kmSaatIni = 48000,
                    kmServiceTerakhir = 45000,
                    intervalService = 5000,
                    kmServiceBerikutnya = 50000,
                    sisaKm = 2000,
                    status = "AMAN",
                    flag = "",
                    fotoKm = "",
                    catattan = "",
                    pajakTahunan = "01/12/2026",
                    kirDate = "05/09/2026",
                    pajak5Tahunan = "01/12/2028"
                ),
                ArmadaEntity(
                    armadaId = "HK02",
                    noPolisi = "A8653ZU",
                    kmSaatIni = 59100,
                    kmServiceTerakhir = 55000,
                    intervalService = 5000,
                    kmServiceBerikutnya = 60000,
                    sisaKm = 900,
                    status = "⚠️ SERVICE <1000 KM",
                    flag = "",
                    fotoKm = "",
                    catattan = "",
                    pajakTahunan = "19/07/2027",
                    kirDate = "11/09/2026",
                    pajak5Tahunan = "19/07/2031"
                ),
                ArmadaEntity(
                    armadaId = "HK03",
                    noPolisi = "W8649QK",
                    kmSaatIni = 72000,
                    kmServiceTerakhir = 70000,
                    intervalService = 5000,
                    kmServiceBerikutnya = 75000,
                    sisaKm = 3000,
                    status = "AMAN",
                    flag = "",
                    fotoKm = "",
                    catattan = "",
                    pajakTahunan = "22/06/2027",
                    kirDate = "11/12/2026",
                    pajak5Tahunan = "22/06/2031"
                ),
                ArmadaEntity(
                    armadaId = "HK04",
                    noPolisi = "A8190ZV",
                    kmSaatIni = 81000,
                    kmServiceTerakhir = 80000,
                    intervalService = 5000,
                    kmServiceBerikutnya = 85000,
                    sisaKm = 4000,
                    status = "AMAN",
                    flag = "",
                    fotoKm = "",
                    catattan = "",
                    pajakTahunan = "08/06/2027",
                    kirDate = "30/09/2026",
                    pajak5Tahunan = "08/06/2030"
                )
            )
            db.armadaDao().insertArmada(armada)

            // Populate Pengiriman
            val pengirimanList = listOf(
                PengirimanEntity(
                    noSuratJalan = "SJ/20260720/001",
                    tanggal = "20 Juli 2026",
                    driver = "Driver HUB 1",
                    armada = "HK01",
                    gudangAsal = "Gudang Kediri Utama",
                    tujuan = "Surabaya (Margomulyo)",
                    jumlahKoli = 35,
                    volumeCbm = 3.5,
                    status = "Selesai",
                    catatan = "Barang sparepart otomotif aman"
                ),
                PengirimanEntity(
                    noSuratJalan = "SJ/20260720/002",
                    tanggal = "20 Juli 2026",
                    driver = "Driver HUB 2",
                    armada = "HK02",
                    gudangAsal = "Gudang Kediri Utama",
                    tujuan = "Malang (Klojen)",
                    jumlahKoli = 50,
                    volumeCbm = 6.2,
                    status = "Dalam Perjalanan",
                    catatan = "Paket sembako logistik"
                ),
                PengirimanEntity(
                    noSuratJalan = "SJ/20260720/003",
                    tanggal = "20 Juli 2026",
                    driver = "Driver HUB 1",
                    armada = "HK03",
                    gudangAsal = "Gudang Kediri Cabang",
                    tujuan = "Blitar (Kanigoro)",
                    jumlahKoli = 20,
                    volumeCbm = 1.8,
                    status = "Belum Berangkat",
                    catatan = "Muatan bahan pokok curah"
                ),
                PengirimanEntity(
                    noSuratJalan = "SJ/20260719/004",
                    tanggal = "19 Juli 2026",
                    driver = "Driver HUB 2",
                    armada = "HK01",
                    gudangAsal = "Gudang Kediri Utama",
                    tujuan = "Tulungagung",
                    jumlahKoli = 15,
                    volumeCbm = 1.2,
                    status = "Selesai",
                    catatan = "Elektronik pecah belah"
                )
            )
            db.pengirimanDao().insertAllPengiriman(pengirimanList)
        }
    }
}
