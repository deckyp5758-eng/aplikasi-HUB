package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {
    @Query("SELECT * FROM driver")
    fun getAllDrivers(): Flow<List<DriverEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<DriverEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverEntity)

    @Query("DELETE FROM driver WHERE idDriver = :id")
    suspend fun deleteDriverById(id: String)

    @Query("DELETE FROM driver")
    suspend fun deleteAllDrivers()

    @Query("SELECT * FROM driver WHERE idDriver = :id LIMIT 1")
    suspend fun getDriverById(id: String): DriverEntity?

    @Query("SELECT * FROM driver WHERE namaDriver = :name LIMIT 1")
    suspend fun getDriverByName(name: String): DriverEntity?
}

@Dao
interface ArmadaDao {
    @Query("SELECT * FROM armada")
    fun getAllArmada(): Flow<List<ArmadaEntity>>

    @Query("SELECT * FROM armada")
    suspend fun getAllArmadaList(): List<ArmadaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArmada(armada: List<ArmadaEntity>)

    @Query("SELECT * FROM armada WHERE armadaId = :id LIMIT 1")
    suspend fun getArmadaById(id: String): ArmadaEntity?

    @Update
    suspend fun updateArmada(armada: ArmadaEntity)
}

@Dao
interface LogHarianDao {
    @Query("SELECT * FROM log_harian ORDER BY id DESC")
    fun getAllLogs(): Flow<List<LogHarianEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogHarianEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<LogHarianEntity>)

    @Query("DELETE FROM log_harian")
    suspend fun clearLogs()
}

@Dao
interface BanDao {
    @Query("SELECT * FROM ban_armada")
    fun getAllBan(): Flow<List<BanEntity>>

    @Query("SELECT * FROM ban_armada WHERE armadaId = :armadaId")
    fun getBanByArmadaId(armadaId: String): Flow<List<BanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBan(banList: List<BanEntity>)

    @Query("UPDATE ban_armada SET barcode = :barcode, noSeri = :barcode, tahun = :tahun, kodeBan = :kodeBan, tanggalUpdate = :tanggalUpdate WHERE (armadaId = :armadaId OR noPolisi = :armadaId OR UPPER(REPLACE(REPLACE(armadaId, ' ', ''), '-', '')) = UPPER(REPLACE(REPLACE(:armadaId, ' ', ''), '-', '')) OR UPPER(REPLACE(REPLACE(noPolisi, ' ', ''), '-', '')) = UPPER(REPLACE(REPLACE(:armadaId, ' ', ''), '-', ''))) AND UPPER(REPLACE(REPLACE(posisi, ' ', ''), '-', '')) = UPPER(REPLACE(REPLACE(:posisi, ' ', ''), '-', '')) AND UPPER(posisi) != 'AKI'")
    suspend fun updateTireInfo(armadaId: String, posisi: String, barcode: String, tahun: String, kodeBan: String?, tanggalUpdate: String?): Int

    @Query("UPDATE ban_armada SET barcode = :barcode, noSeri = :barcode, kondisi = :kondisi, merk = :merk, tekanan = :merk, keterangan = :keterangan, tahun = :tahun, kodeBan = :kodeBan, tanggalUpdate = :tanggalUpdate WHERE (armadaId = :armadaId OR noPolisi = :armadaId OR UPPER(REPLACE(REPLACE(armadaId, ' ', ''), '-', '')) = UPPER(REPLACE(REPLACE(:armadaId, ' ', ''), '-', '')) OR UPPER(REPLACE(REPLACE(noPolisi, ' ', ''), '-', '')) = UPPER(REPLACE(REPLACE(:armadaId, ' ', ''), '-', ''))) AND UPPER(REPLACE(REPLACE(posisi, ' ', ''), '-', '')) = 'AKI'")
    suspend fun updateAkiInfo(armadaId: String, barcode: String, kondisi: String, merk: String, keterangan: String, tahun: String = "2025", kodeBan: String?, tanggalUpdate: String?): Int

    @Query("DELETE FROM ban_armada")
    suspend fun clearAllBan()
}

@Dao
interface PengirimanDao {
    @Query("SELECT * FROM pengiriman ORDER BY id DESC")
    fun getAllPengiriman(): Flow<List<PengirimanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengiriman(pengiriman: PengirimanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPengiriman(list: List<PengirimanEntity>)

    @Update
    suspend fun updatePengiriman(pengiriman: PengirimanEntity)

    @Query("DELETE FROM pengiriman WHERE id = :id")
    suspend fun deletePengirimanById(id: Int)

    @Query("DELETE FROM pengiriman")
    suspend fun clearAllPengiriman()
}

@Dao
interface CatatanDriverDao {
    @Query("SELECT * FROM catatan_driver ORDER BY id DESC")
    fun getAllCatatanDriver(): Flow<List<CatatanDriverEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatatanDriver(item: CatatanDriverEntity)

    @Query("DELETE FROM catatan_driver WHERE armadaId = :armadaId")
    suspend fun clearCatatanByArmadaId(armadaId: String)

    @Query("DELETE FROM catatan_driver")
    suspend fun clearAll()
}

@Dao
interface PengajuanDao {
    @Query("SELECT * FROM pengajuan ORDER BY id DESC")
    fun getAllPengajuan(): Flow<List<PengajuanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPengajuan(item: PengajuanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPengajuan(list: List<PengajuanEntity>)

    @Query("DELETE FROM pengajuan")
    suspend fun clearAll()
}
