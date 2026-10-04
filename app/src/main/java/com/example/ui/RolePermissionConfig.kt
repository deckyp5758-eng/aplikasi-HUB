package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Konfigurasi Role dan Matriks Hak Akses Aplikasi HUB KEDIRI
 * Terpusat, bersih, dan mudah dirawat sewaktu-waktu ada perubahan kebijakan hak akses.
 */
enum class AppRole {
    STAFF,
    DRIVER,
    KENEK,
    LP,
    UNKNOWN;

    companion object {
        fun fromString(role: String?): AppRole {
            return when (role?.trim()?.uppercase()) {
                "STAFF" -> STAFF
                "DRIVER" -> DRIVER
                "KENEK" -> KENEK
                "LP" -> LP
                else -> UNKNOWN
            }
        }

        /**
         * Resolves role from saved role/jabatan string from Users tab login result.
         * Role must not be determined by hardcoded driver names or IDs.
         * Empty or unknown roles resolve to UNKNOWN.
         */
        fun resolveRole(role: String?, driverNameOrId: String? = null): AppRole {
            val fromRole = fromString(role)
            if (fromRole != UNKNOWN) return fromRole
            return UNKNOWN
        }
    }
}

enum class MenuItemId {
    STATUS_ARMADA,
    CATAT_SERVICE,
    LAPORAN,
    CATATAN_DRIVER,
    PENGAJUAN_BAN,
    LOG_HARIAN,
    ARSIP_PENGIRIMAN,
    PENGATURAN,
    SERVICE_AC,
    KELUAR
}

data class DashboardMenuItem(
    val id: MenuItemId,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val circleBgColor: Color,
    val destinationScreen: String? = null
)

object RolePermissionMatrix {

    /**
     * MATRIKS HAK AKSES PER-ROLE:
     * Staff  : Status armada, Catat service, Laporan, Pengaturan, Keluar
     * Driver : Status armada, Catatan driver, Pengajuan Ban/aks, Service AC, Keluar
     * Kenek  : Status armada, Log Harian, Arsip Pengiriman, Keluar
     * LP     : Status armada, Keluar
     */
    val ROLE_MENU_MAP: Map<AppRole, List<MenuItemId>> = mapOf(
        AppRole.STAFF to listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.CATAT_SERVICE,
            MenuItemId.LAPORAN,
            MenuItemId.PENGATURAN,
            MenuItemId.KELUAR
        ),
        AppRole.DRIVER to listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.CATATAN_DRIVER,
            MenuItemId.PENGAJUAN_BAN,
            MenuItemId.SERVICE_AC,
            MenuItemId.KELUAR
        ),
        AppRole.KENEK to listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.LOG_HARIAN,
            MenuItemId.ARSIP_PENGIRIMAN,
            MenuItemId.KELUAR
        ),
        AppRole.LP to listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.KELUAR
        ),
        AppRole.UNKNOWN to listOf(
            MenuItemId.STATUS_ARMADA,
            MenuItemId.KELUAR
        )
    )

    val ALL_MENU_ITEMS: List<DashboardMenuItem> = listOf(
        DashboardMenuItem(
            id = MenuItemId.STATUS_ARMADA,
            title = "Status Armada",
            description = "Pantau status & kilometer armada",
            icon = Icons.Default.LocalShipping,
            circleBgColor = Color(0xFF0054A6),
            destinationScreen = "armada"
        ),
        DashboardMenuItem(
            id = MenuItemId.LOG_HARIAN,
            title = "Log Harian",
            description = "Catat KM & aktivitas harian armada",
            icon = Icons.Default.Speed,
            circleBgColor = Color(0xFF059669),
            destinationScreen = "form"
        ),
        DashboardMenuItem(
            id = MenuItemId.CATAT_SERVICE,
            title = "Catat Servis",
            description = "Input servis & penggantian part",
            icon = Icons.Default.Build,
            circleBgColor = Color(0xFFDC2626),
            destinationScreen = "service"
        ),
        DashboardMenuItem(
            id = MenuItemId.CATATAN_DRIVER,
            title = "Catatan Driver",
            description = "Input & simpan keluhan driver",
            icon = Icons.Default.RateReview,
            circleBgColor = Color(0xFFD97706),
            destinationScreen = null
        ),
        DashboardMenuItem(
            id = MenuItemId.LAPORAN,
            title = "Laporan",
            description = "Lihat laporan & rekap data",
            icon = Icons.Default.BarChart,
            circleBgColor = Color(0xFF7C3AED),
            destinationScreen = null
        ),
        DashboardMenuItem(
            id = MenuItemId.PENGATURAN,
            title = "Pengaturan",
            description = "Kelola Google Sheet & API Key",
            icon = Icons.Default.Settings,
            circleBgColor = Color(0xFF475569),
            destinationScreen = "settings"
        ),
        DashboardMenuItem(
            id = MenuItemId.PENGAJUAN_BAN,
            title = "Pengajuan Ban/Aks",
            description = "Pengajuan ban & aksesoris armada",
            icon = Icons.Default.ShoppingCart,
            circleBgColor = Color(0xFFE11D48),
            destinationScreen = "pengajuan"
        ),
        DashboardMenuItem(
            id = MenuItemId.SERVICE_AC,
            title = "Service AC",
            description = "Catat KM service AC armada",
            icon = Icons.Default.AcUnit,
            circleBgColor = Color(0xFF0284C7),
            destinationScreen = "service_ac"
        ),
        DashboardMenuItem(
            id = MenuItemId.ARSIP_PENGIRIMAN,
            title = "Arsip Pengiriman",
            description = "Simpan arsip bukti kirim",
            icon = Icons.Default.CloudUpload,
            circleBgColor = Color(0xFF0891B2),
            destinationScreen = "arsip_pengiriman"
        ),
        DashboardMenuItem(
            id = MenuItemId.KELUAR,
            title = "Keluar",
            description = "Keluar dari sesi akun",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            circleBgColor = Color(0xFF991B1B),
            destinationScreen = null
        )
    )

    private val ALL_MENU_ITEMS_MAP: Map<MenuItemId, DashboardMenuItem> = ALL_MENU_ITEMS.associateBy { it.id }

    fun getAllowedMenuItems(roleString: String?, driverNameOrId: String? = null): List<DashboardMenuItem> {
        val role = AppRole.resolveRole(roleString, driverNameOrId)
        val allowedIds = ROLE_MENU_MAP[role] ?: ROLE_MENU_MAP[AppRole.UNKNOWN]!!
        return allowedIds.mapNotNull { ALL_MENU_ITEMS_MAP[it] }
    }

    fun isScreenAllowed(roleString: String?, screenRoute: String, driverNameOrId: String? = null): Boolean {
        if (screenRoute == "dashboard") return true
        val role = AppRole.resolveRole(roleString, driverNameOrId)
        if (screenRoute == "service_ac") {
            return role == AppRole.DRIVER
        }
        val allowedMenuIds = ROLE_MENU_MAP[role] ?: ROLE_MENU_MAP[AppRole.UNKNOWN]!!
        val targetMenuItem = ALL_MENU_ITEMS.find { it.destinationScreen == screenRoute }
        return targetMenuItem == null || allowedMenuIds.contains(targetMenuItem.id)
    }
}
