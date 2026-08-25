package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FleetReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("FleetReminderWorker", "Menjalankan pengecekan background jam 08:00...")
            val db = AppDatabase.getDatabase(appContext, this)
            val armadaList = db.armadaDao().getAllArmadaList()

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = Calendar.getInstance()

            val criticalArmada = mutableListOf<String>()
            val expiringDocs = mutableListOf<String>()

            for (armada in armadaList) {
                // 1. Check Service urgency
                if (armada.sisaKm in 0..1000) {
                    criticalArmada.add("${armada.armadaId} (${armada.noPolisi}) sisa ${armada.sisaKm} KM")
                }

                // 2. Check KIR expiry
                armada.kirDate?.let { dateStr ->
                    try {
                        val parsed = sdf.parse(dateStr)
                        if (parsed != null) {
                            val diffDays = ((parsed.time - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                            if (diffDays in 0..14) {
                                expiringDocs.add("${armada.armadaId} KIR sisa $diffDays hari ($dateStr)")
                            } else if (diffDays < 0) {
                                expiringDocs.add("⚠️ ${armada.armadaId} KIR KEDALUWARSA ($dateStr)")
                            }
                        }
                    } catch (e: Exception) {
                        // ignore parse errors
                    }
                }

                // 3. Check Pajak Tahunan expiry
                armada.pajakTahunan?.let { dateStr ->
                    try {
                        val parsed = sdf.parse(dateStr)
                        if (parsed != null) {
                            val diffDays = ((parsed.time - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                            if (diffDays in 0..14) {
                                expiringDocs.add("${armada.armadaId} Pajak sisa $diffDays hari ($dateStr)")
                            } else if (diffDays < 0) {
                                expiringDocs.add("⚠️ ${armada.armadaId} Pajak KEDALUWARSA ($dateStr)")
                            }
                        }
                    } catch (e: Exception) {
                        // ignore parse errors
                    }
                }
            }

            // Build notifications based on priority
            if (expiringDocs.isNotEmpty()) {
                val docSummary = expiringDocs.take(3).joinToString(", ")
                NotificationHelper.sendNotification(
                    context = appContext,
                    title = "⚠️ Peringatan KIR / Pajak Armada HO33",
                    message = "Perhatian: $docSummary. Segera tindak lanjuti sebelum jatuh tempo!"
                )
            } else if (criticalArmada.isNotEmpty()) {
                val srvSummary = criticalArmada.take(2).joinToString(", ")
                NotificationHelper.sendNotification(
                    context = appContext,
                    title = "🔧 Pengingat Servis Armada HO33",
                    message = "Armada mendekati batas servis: $srvSummary. Mohon jadwalkan servis bengkel."
                )
            } else {
                // Regular daily morning reminder
                val dayFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                val dateFormatted = dayFormat.format(Date())
                NotificationHelper.sendNotification(
                    context = appContext,
                    title = "🚚 Pengingat Pagi HUB Kediri (HO33)",
                    message = "Selamat pagi! Hari ini $dateFormatted. Jangan lupa foto dan catat KM awal Odometer sebelum mulai bertugas."
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("FleetReminderWorker", "Gagal menjalankan background reminder", e)
            Result.retry()
        }
    }
}
