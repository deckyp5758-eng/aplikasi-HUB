package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AkiStatusResult(
    val armadaId: String = "",
    val noPolisi: String = "",
    val barcode: String = "0255KDR",
    val tanggalPasang: String = "-",
    val tanggalGantiBerikutnya: String = "-",
    val sisaHari: Long? = null,
    val isDue: Boolean = false, // true when sisaHari <= 30 or past 2 years (triggers notification!)
    val isExpired: Boolean = false, // true when past 2 years (sisaHari <= 0)
    val isWarning30Days: Boolean = false, // true when 1..30 days remaining
    val statusLabel: String = "AMAN",
    val merk: String = "Aki Standard"
)

object AkiUtils {

    /**
     * Menghitung tanggal ganti aki berikutnya (2 tahun dari tanggal pemasangan)
     * dan mengecek apakah usia aki sudah mencapai/mendekati 2 tahun (notifikasi 30 hari sebelum 2 tahun).
     */
    fun calculateAkiStatus(
        armadaId: String,
        noPolisi: String,
        barcode: String?,
        tanggalPasangStr: String?,
        userStatus: String? = null,
        merk: String? = null
    ): AkiStatusResult {
        val rawDateStr = tanggalPasangStr.orEmpty().trim()
        val barVal = barcode.orEmpty().ifBlank { "0255KDR" }
        val merkVal = merk.orEmpty().ifBlank { "Aki Standard" }

        if (rawDateStr.isBlank() || rawDateStr == "-") {
            val status = userStatus?.ifBlank { "AMAN" } ?: "AMAN"
            return AkiStatusResult(
                armadaId = armadaId,
                noPolisi = noPolisi,
                barcode = barVal,
                tanggalPasang = "-",
                tanggalGantiBerikutnya = "-",
                sisaHari = null,
                isDue = false,
                isExpired = false,
                isWarning30Days = false,
                statusLabel = status,
                merk = merkVal
            )
        }

        var cleanRawStr = rawDateStr
        if (cleanRawStr.contains("GMT")) {
            // Strip timezone name inside parentheses (e.g., "(Pacific Daylight Time)")
            cleanRawStr = cleanRawStr.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
        }

        val formats = listOf(
            SimpleDateFormat("M/d/yyyy", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("d/M/yyyy", Locale.US),
            SimpleDateFormat("dd MMMM yyyy", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US),
            SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US)
        )

        var parsedDate: Date? = null
        for (f in formats) {
            try {
                parsedDate = f.parse(cleanRawStr)
                if (parsedDate != null) break
            } catch (_: Exception) {}
        }

        if (parsedDate == null) {
            val isUserGanti = userStatus?.contains("GANTI", ignoreCase = true) == true
            val status = userStatus?.ifBlank { "AMAN" } ?: "AMAN"
            return AkiStatusResult(
                armadaId = armadaId,
                noPolisi = noPolisi,
                barcode = barVal,
                tanggalPasang = rawDateStr,
                tanggalGantiBerikutnya = "$rawDateStr + 2 Thn",
                sisaHari = null,
                isDue = isUserGanti,
                isExpired = isUserGanti,
                isWarning30Days = false,
                statusLabel = status,
                merk = merkVal
            )
        }

        // Calculate 2 years replacement date
        val calGanti = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.YEAR, 2)
        }
        val gantiDate = calGanti.time
        val gantiDateFormatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(gantiDate)

        // Today start of day
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = todayCal.time

        val diffMillis = gantiDate.time - today.time
        val sisaHari = diffMillis / (1000 * 60 * 60 * 24)

        val isExpired = sisaHari <= 0
        val isWarning30Days = sisaHari in 1..30
        val isUserGanti = userStatus?.contains("GANTI", ignoreCase = true) == true
        val isDue = isExpired || isWarning30Days || isUserGanti

        val statusLabel = when {
            isUserGanti -> userStatus.orEmpty()
            isExpired -> "GANTI (SUDAH >2 TAHUN)"
            isWarning30Days -> "SEGERA GANTI (H-$sisaHari HARI)"
            !userStatus.isNullOrBlank() && userStatus.trim().uppercase() != "AMAN" -> userStatus
            else -> "AMAN"
        }

        val cleanTanggalPasang = try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsedDate)
        } catch (_: Exception) {
            rawDateStr
        }

        return AkiStatusResult(
            armadaId = armadaId,
            noPolisi = noPolisi,
            barcode = barVal,
            tanggalPasang = cleanTanggalPasang,
            tanggalGantiBerikutnya = gantiDateFormatted,
            sisaHari = sisaHari,
            isDue = isDue,
            isExpired = isExpired,
            isWarning30Days = isWarning30Days,
            statusLabel = statusLabel,
            merk = merkVal
        )
    }
}
