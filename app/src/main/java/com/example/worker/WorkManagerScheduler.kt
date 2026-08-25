package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    private const val DAILY_WORK_TAG = "fleet_daily_8am_reminder"
    private const val IMMEDIATE_TEST_TAG = "fleet_test_reminder"

    /**
     * Menjadwalkan pengingat otomatis background setiap pagi jam 08:00 WIB.
     * Berjalan meskipun aplikasi ditutup / setelah HP reboot.
     */
    fun scheduleDaily8AmReminder(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Jika jam sekarang sudah melewati jam 08:00 pagi, jadwalkan untuk besok jam 08:00
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        Log.d("WorkManagerScheduler", "Menjadwalkan alarm jam 08:00 pagi dalam: ${timeDiff / 1000 / 60} menit ke depan.")

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<FleetReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(DAILY_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    /**
     * Memungkinkan testing instan notifikasi latar belakang
     */
    fun triggerImmediateTestReminder(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<FleetReminderWorker>()
            .addTag(IMMEDIATE_TEST_TAG)
            .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_TEST_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }
}
