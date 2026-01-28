package com.kv.kiosklauncher

import android.app.Application
import com.kv.kiosklauncher.data.repository.SecurePreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Kiosk Launcher
 * Initializes Hilt and default credentials
 */
@HiltAndroidApp
class KioskLauncherApp : Application() {
    
    @Inject
    lateinit var securePrefsRepository: SecurePreferencesRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize default admin credentials if not set
        securePrefsRepository.initializeDefaultCredentials()
    }
}
