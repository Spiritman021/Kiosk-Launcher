package com.kv.kiosklauncher.data.model

/**
 * Configuration settings for kiosk mode
 */
data class KioskConfiguration(
    val isKioskModeEnabled: Boolean = false,
    val useLockTaskMode: Boolean = false,
    val emergencyExitEnabled: Boolean = true,
    val gridColumns: Int = 4,
    val showAppNames: Boolean = true,
    val iconSize: Int = 128, // dp
    val lastConfigUpdate: Long = System.currentTimeMillis()
)
