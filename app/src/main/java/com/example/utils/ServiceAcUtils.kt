package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ServiceAcCalculationResult(
    val armadaId: String,
    val noPolisi: String,
    val kmSaatIni: Int,
    val tglServiceTerakhir: String,
    val kmServiceTerakhir: Int,
    val jadwalService: String = "6 BULAN",
    val intervalBulan: Int = 6,
    val intervalKm: Int = 10000,
    val serviceAcBerikutnya: String,
    val kmServiceBerikutnya: Int,
    val sisaKm: Int,
    val status: String,
    val catatan: String = ""
)

object ServiceAcUtils {

    const val DEFAULT_INTERVAL_BULAN = 6
    const val DEFAULT_INTERVAL_KM = 10000
    const val DEFAULT_JADWAL_SERVICE = "6 BULAN"

    const val STATUS_NORMAL = "NORMAL"
    const val STATUS_SEGERA_SERVIS = "SEGERA SERVIS"
    const val STATUS_JATUH_TEMPO = "JATUH TEMPO"

    private val supportedDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US)
    )

    fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val cleanStr = dateStr.trim()
        for (format in supportedDateFormats) {
            try {
                format.isLenient = false
                val parsed = format.parse(cleanStr)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    fun formatDate(date: Date, pattern: String = "yyyy-MM-dd"): String {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        return sdf.format(date)
    }

    /**
     * Hitung jadwal, sisa KM, dan status Service AC berdasarkan rumus:
     * - Tanggal Service Berikutnya: EDATE(tglService, intervalBulan)
     * - KM Service Berikutnya: kmService + intervalKm
     * - Sisa KM: KM Service Berikutnya - KM Saat Ini
     * - Status:
     *   JATUH TEMPO: jika hari ini >= tanggal berikutnya ATAU km saat ini >= km service berikutnya
     *   SEGERA SERVIS: jika sisa hari <= 30 ATAU sisa km <= 1000
     *   NORMAL: kondisi lainnya
     */
    fun calculateServiceAc(
        armadaId: String,
        noPolisi: String,
        kmSaatIni: Int,
        tglServiceTerakhir: String,
        kmServiceTerakhir: Int,
        intervalBulan: Int = DEFAULT_INTERVAL_BULAN,
        intervalKm: Int = DEFAULT_INTERVAL_KM,
        referenceDate: Date = Date(),
        catatan: String = ""
    ): ServiceAcCalculationResult {
        val parsedDate = parseDate(tglServiceTerakhir) ?: referenceDate

        // Hitung EDATE (+ intervalBulan)
        val cal = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.MONTH, intervalBulan)
        }
        val nextDate = cal.time
        val nextDateStr = formatDate(nextDate, "yyyy-MM-dd")

        val kmServiceBerikutnya = kmServiceTerakhir + intervalKm
        val sisaKm = kmServiceBerikutnya - kmSaatIni

        // Hitung selisih hari
        val diffMillis = nextDate.time - referenceDate.time
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        val isJatuhTempo = referenceDate.time >= nextDate.time || kmSaatIni >= kmServiceBerikutnya
        val isSegeraServis = !isJatuhTempo && (diffDays <= 30 || sisaKm <= 1000)

        val status = when {
            isJatuhTempo -> STATUS_JATUH_TEMPO
            isSegeraServis -> STATUS_SEGERA_SERVIS
            else -> STATUS_NORMAL
        }

        return ServiceAcCalculationResult(
            armadaId = armadaId,
            noPolisi = noPolisi,
            kmSaatIni = kmSaatIni,
            tglServiceTerakhir = formatDate(parsedDate, "yyyy-MM-dd"),
            kmServiceTerakhir = kmServiceTerakhir,
            jadwalService = "$intervalBulan BULAN",
            intervalBulan = intervalBulan,
            intervalKm = intervalKm,
            serviceAcBerikutnya = nextDateStr,
            kmServiceBerikutnya = kmServiceBerikutnya,
            sisaKm = sisaKm,
            status = status,
            catatan = catatan
        )
    }
}
