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
        
        // CRITICAL: Initialize default whitelist on every app start
        // This ensures kiosk launcher itself is always whitelisted
        applicationScope.launch {
            // Ensure default settings exist
            settingsRepository.getSettings()
            
            // Initialize default whitelist (kiosk launcher + phone app)
            whitelistRepository.initializeDefaultWhitelist()
        }
    }
}

