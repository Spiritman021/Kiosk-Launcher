package com.kv.kiosklauncher.data.repository

import com.kv.kiosklauncher.data.dao.KioskSettingsDao
import com.kv.kiosklauncher.data.model.BlockingMode
import com.kv.kiosklauncher.data.model.KioskSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing kiosk settings.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val kioskSettingsDao: KioskSettingsDao
) {
    
    /**
     * Get current settings, or create default if none exist
     */
    suspend fun getSettings(): KioskSettings {
        return kioskSettingsDao.getSettings() ?: createDefaultSettings()
    }
    
    /**
     * Observe settings changes
     */
    fun observeSettings(): Flow<KioskSettings?> {
        return kioskSettingsDao.observeSettings()
    }
    
    /**
     * Update settings
     */
    suspend fun updateSettings(settings: KioskSettings) {
        kioskSettingsDao.updateSettings(settings.copy(lastModified = System.currentTimeMillis()))
    }
    
    /**
     * Create and save default settings
     */
    private suspend fun createDefaultSettings(): KioskSettings {
        val defaultSettings = KioskSettings(
            blockingMode = BlockingMode.BOTH,
            enableScreenOff = true,
            autoWhitelistDialer = true,
            monitoringIntervalMs = 100,
            showNotificationDuringSession = true,
            vibrateOnBlock = true,
            showBlockOverlay = true,
            screenOffRedirectDelayMs = 500
        )
        kioskSettingsDao.insertSettings(defaultSettings)
        return defaultSettings
    }
    
    /**
     * Reset settings to default
     */
    suspend fun resetToDefaults() {
        kioskSettingsDao.deleteSettings()
        createDefaultSettings()
    }
}
