package com.kv.kiosklauncher

import android.app.Application
import com.kv.kiosklauncher.data.repository.SettingsRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Kiosk Launcher.
 * Initializes Hilt dependency injection and default settings.
 */
@HiltAndroidApp
class KioskLauncherApp : Application() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize default settings and auto-whitelist phone app
        applicationScope.launch {
            // Ensure default settings exist
            settingsRepository.getSettings()
            
            // Auto-whitelist phone app if setting is enabled
            val settings = settingsRepository.getSettings()
            if (settings.autoWhitelistDialer) {
                whitelistRepository.autoWhitelistPhoneApp()
            }
        }
    }
}

