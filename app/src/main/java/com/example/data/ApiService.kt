package com.example.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("exec")
    suspend fun getDrivers(
        @Query("action") action: String = "getDrivers",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): DriversApiResponse

    @GET("exec")
    suspend fun getArmada(
        @Query("action") action: String = "getArmada",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): ArmadaApiResponse

    @POST("exec")
    suspend fun login(
        @Body request: LoginApiRequest,
        @Query("action") action: String = "login",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): LoginApiResponse

    @POST("exec")
    suspend fun submitLog(
        @Body request: SubmitLogApiRequest,
        @Query("action") action: String = "submitLog",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): SubmitLogApiResponse

    @POST("exec")
    suspend fun submitServiceLog(
        @Body request: ServiceLogApiRequest,
        @Query("action") action: String = "submitservicelog",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): ServiceLogApiResponse

    @POST("exec")
    suspend fun performOcr(
        @Body request: OcrApiRequest,
        @Query("action") action: String = "performOcr",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): OcrApiResponse

    @POST("exec")
    suspend fun callAsistenAi(
        @Body request: AsistenAiApiRequest,
        @Query("action") action: String = "asisten_ai",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): AsistenAiApiResponse

    @GET("exec")
    suspend fun getLogs(
        @Query("action") action: String = "getLogs",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): LogsApiResponse

    @GET("exec")
    suspend fun getBanArmada(
        @Query("action") action: String = "getBanArmada",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): BanArmadaApiResponse

    @POST("exec")
    suspend fun updateBan(
        @Body request: UpdateBanApiRequest,
        @Query("action") action: String = "update_ban",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): UpdateBanApiResponse

    @POST("exec")
    suspend fun updateAki(
        @Body request: UpdateAkiApiRequest,
        @Query("action") action: String = "update_aki",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = "1886867333"
    ): UpdateBanApiResponse

    @POST("exec")
    suspend fun updateFotoArmada(
        @Body request: UpdateFotoArmadaApiRequest,
        @Query("action") action: String = "updateFotoArmada",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): UpdateFotoArmadaApiResponse

    @POST("exec")
    suspend fun addPengiriman(
        @Body request: AddPengirimanApiRequest,
        @Query("action") action: String = "addPengiriman",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): CommonWriteApiResponse

    @POST("exec")
    suspend fun updatePengiriman(
        @Body request: UpdatePengirimanApiRequest,
        @Query("action") action: String = "updatePengiriman",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): CommonWriteApiResponse

    @POST("exec")
    suspend fun deletePengiriman(
        @Body request: DeletePengirimanApiRequest,
        @Query("action") action: String = "deletePengiriman",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): CommonWriteApiResponse

    @POST("exec")
    suspend fun submitTerkirim(
        @Body request: SubmitTerkirimApiRequest,
        @Query("action") action: String = "submitterkirim",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): CommonWriteApiResponse

    @POST("exec")
    suspend fun submitCatatanDriver(
        @Body request: SubmitCatatanDriverApiRequest,
        @Query("action") action: String = "submit_catatan_driver",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): CommonWriteApiResponse

    @POST("exec")
    suspend fun clearCatatanDriver(
        @Body request: ClearCatatanDriverApiRequest,
        @Query("action") action: String = "clear_catatan_driver",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): CommonWriteApiResponse

    @GET("exec")
    suspend fun getAiKnowledge(
        @Query("action") action: String = "getAiKnowledge",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = null
    ): AiKnowledgeApiResponse

    @POST("exec")
    suspend fun submitPengajuan(
        @Body request: SubmitPengajuanApiRequest,
        @Query("action") action: String = "submitPengajuan",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = "1517362778"
    ): CommonWriteApiResponse

    @GET("exec")
    suspend fun getPengajuan(
        @Query("action") action: String = "getPengajuan",
        @Query("spreadsheetId") spreadsheetId: String? = null,
        @Query("sheetId") sheetId: String? = "1517362778"
    ): GetPengajuanApiResponse

    @GET("exec")
    suspend fun checkUpdate(
        @Query("action") action: String = "checkUpdate"
    ): AppUpdateResponse
}

data class AiKnowledgeApiResponse(
    val success: Boolean,
    val data: List<AiKnowledgeApiItem>?
)

data class AiKnowledgeApiItem(
    val id: String,
    val kategori: String,
    val pertanyaan: String,
    val jawaban: String
)

data class TerkirimMediaFile(
    val base64: String,
    val fileName: String,
    val mimeType: String
)

data class SubmitTerkirimApiRequest(
    val action: String = "submitterkirim",
    val id: Int? = null,
    val noDokumen: String,
    val noSuratJalan: String,
    val tanggal: String,
    val driver: String,
    val armada: String,
    val alamat: String,
    val penerima: String,
    val noTelpCustomer: String,
    val volumeCbm: Double,
    val catatan: String,
    val files: List<TerkirimMediaFile>,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class AddPengirimanApiRequest(
    val noSuratJalan: String,
    val tanggal: String,
    val driver: String,
    val armada: String,
    val gudangAsal: String,
    val tujuan: String,
    val jumlahKoli: Int,
    val volumeCbm: Double,
    val status: String,
    val catatan: String
)

data class UpdatePengirimanApiRequest(
    val id: Int,
    val noSuratJalan: String,
    val tanggal: String,
    val driver: String,
    val armada: String,
    val gudangAsal: String,
    val tujuan: String,
    val jumlahKoli: Int,
    val volumeCbm: Double,
    val status: String,
    val catatan: String
)

data class DeletePengirimanApiRequest(
    val id: Int
)

data class CommonWriteApiResponse(
    val success: Boolean,
    val message: String?
)


data class BanArmadaApiResponse(
    val success: Boolean,
    val banArmada: List<BanApiItem>?
)

data class BanApiItem(
    val armadaId: String,
    val noPolisi: String,
    val posisi: String? = "",
    val noSeri: String? = "",
    val ukuran: String? = "",
    val merk: String? = "",
    val kondisi: String? = "",
    val tekanan: String? = "",
    val keterangan: String? = "",
    val barcode: String? = null,
    val tahun: String? = null,
    val codeBan: String? = null,
    val tanggalUpdate: String? = null,
    val tanggalPasangAki: String? = null,
    val barcodeAki: String? = null
)

data class LogsApiResponse(
    val success: Boolean,
    val logs: List<LogApiItem>?
)

data class LogApiItem(
    val tanggal: String,
    val armadaId: String,
    val kmTerdeteksi: Int,
    val linkFoto: String,
    val catatan: String,
    val namaDriver: String
)

data class DriversApiResponse(
    val success: Boolean,
    val drivers: List<DriverApiItem>?
)

data class DriverApiItem(
    val id: String,
    val name: String
)

data class ArmadaApiResponse(
    val success: Boolean,
    val armada: List<ArmadaApiItem>?
)

data class ArmadaApiItem(
    val armadaId: String,
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
    val kir: String? = null,
    val pajak5Tahunan: String? = null,
    val fotoTruck: String? = null
)

data class LoginApiRequest(
    val action: String = "login",
    val driverName: String,
    val pin: String,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class LoginApiResponse(
    val success: Boolean,
    val driverId: String?,
    val driverName: String?,
    val message: String?
)

data class SubmitLogApiRequest(
    val action: String = "submitLog",
    val logData: LogDataApiItem,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class LogDataApiItem(
    val driverName: String,
    val armadaId: String,
    val kmTerdeteksi: Int,
    val base64Photo: String?,
    val photoName: String?,
    val photoMimeType: String?,
    val catatan: String?
)

data class SubmitLogApiResponse(
    val success: Boolean,
    val linkFoto: String?,
    val sisaKm: Int?,
    val serviceAlert: Boolean?,
    val message: String?
)

data class ServiceLogApiRequest(
    val action: String = "submit_service",
    val spreadsheetId: String? = null,
    val armadaId: String,
    val kmServis: Int,
    val catatan: String?
)

data class ServiceLogApiResponse(
    val success: Boolean,
    val message: String?
)

data class OcrApiRequest(
    val action: String = "ocr",
    val base64Photo: String,
    val apiKey: String? = null,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class OcrApiResponse(
    val success: Boolean,
    val km: Int?,
    val message: String?
)

data class AsistenAiApiRequest(
    val action: String = "asisten_ai",
    val base64Data: String? = null,
    val mimeType: String? = null,
    val chatMessage: String,
    val apiKey: String? = null,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class AsistenAiApiResponse(
    val success: Boolean,
    val message: String?
)

data class UpdateBanApiRequest(
    val action: String = "update_ban",
    val banData: UpdateBanApiData,
    val spreadsheetId: String? = null,
    val sheetId: String? = null,
    val sheetName: String? = null
)

data class UpdateAkiApiRequest(
    val action: String = "update_aki",
    val akiData: UpdateAkiApiData,
    val spreadsheetId: String? = null,
    val sheetId: String? = "1886867333",
    val sheetName: String? = "AKI ARMADA"
)

data class UpdateAkiApiData(
    val armadaId: String,
    val noPolisi: String? = null,
    val barcode: String,
    val tanggalPasangAki: String,
    val merk: String,
    val status: String,
    val kondisi: String? = null,
    val tekanan: String? = null,
    val keterangan: String? = null,
    val sheetId: String? = "1886867333",
    val sheetName: String? = "AKI ARMADA"
)

data class UpdateBanApiData(
    val armadaId: String,
    val noPolisi: String? = null,
    val posisi: String,
    val barcode: String,
    val tahun: String,
    val codeBan: String? = null,
    val noSeri: String? = null,
    val tanggalUpdate: String? = null,
    val kondisi: String? = null,
    val tekanan: String? = null,
    val keterangan: String? = null,
    val tanggalPasangAki: String? = null,
    val barcodeAki: String? = null,
    val sheetName: String? = null,
    val sheetId: String? = null
)

data class UpdateBanApiResponse(
    val success: Boolean,
    val message: String?,
    val updatedData: UpdateBanApiData? = null
)

data class UpdateFotoArmadaApiRequest(
    val action: String = "update_foto_armada",
    val armadaId: String,
    val base64Photo: String,
    val photoMimeType: String = "image/jpeg",
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class UpdateFotoArmadaApiResponse(
    val success: Boolean,
    val linkFoto: String?,
    val message: String?
)

data class SubmitCatatanDriverApiRequest(
    val action: String = "submit_catatan_driver",
    val armadaId: String,
    val driverName: String,
    val tanggal: String,
    val catatan: String,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class ClearCatatanDriverApiRequest(
    val action: String = "clear_catatan_driver",
    val armadaId: String,
    val spreadsheetId: String? = null,
    val sheetId: String? = null
)

data class MediaFileItem(
    val base64: String,
    val fileName: String,
    val fileTag: String = "",
    val mimeType: String = "image/jpeg"
)

data class SubmitPengajuanApiRequest(
    val action: String = "submitPengajuan",
    val driver: String,
    val armadaId: String,
    val noPolisi: String,
    val kategori: String,
    val detail: String,
    val catatan: String,
    val files: List<MediaFileItem>,
    val spreadsheetId: String? = null,
    val sheetId: String? = "1517362778"
)

data class PengajuanApiItem(
    val id: Int? = null,
    val noPengajuan: String? = null,
    val tanggal: String? = null,
    val driver: String? = null,
    val armadaId: String? = null,
    val noPolisi: String? = null,
    val kategori: String? = null,
    val detail: String? = null,
    val catatan: String? = null,
    val foto1Url: String? = null,
    val foto2Url: String? = null,
    val foto3Url: String? = null,
    val foto4Url: String? = null,
    val fotoLainnyaUrls: String? = null,
    val status: String? = null
)

data class GetPengajuanApiResponse(
    val success: Boolean,
    val data: List<PengajuanApiItem>?
)

data class AppUpdateResponse(
    val success: Boolean,
    val latestVersionCode: Int? = null,
    val latestVersionName: String? = null,
    val apkDownloadUrl: String? = null,
    val forceUpdate: Boolean? = false,
    val changelog: String? = null
)


