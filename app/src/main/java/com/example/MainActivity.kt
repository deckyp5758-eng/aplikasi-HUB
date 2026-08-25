package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppContent
import com.example.ui.FleetViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.NotificationHelper
import com.example.worker.WorkManagerScheduler

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Inisialisasi Notification Channel
        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Gagal inisialisasi notification channel", e)
        }

        // 2. Minta izin notifikasi runtime di Android 13+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Gagal meminta izin notifikasi", e)
        }

        // 3. Jadwalkan otomatis pengingat latar belakang setiap jam 08:00 pagi
        try {
            WorkManagerScheduler.scheduleDaily8AmReminder(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Gagal menjadwalkan WorkManager", e)
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: FleetViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            
            val useDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = useDarkTheme) {
                AppContent(viewModel = viewModel)
            }
        }
    }
}

