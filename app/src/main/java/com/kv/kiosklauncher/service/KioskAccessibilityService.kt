package com.kv.kiosklauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility service for robust app monitoring
 * Monitors all window state changes and blocks non-whitelisted apps
 */
@AndroidEntryPoint
class KioskAccessibilityService : AccessibilityService() {
    
    @Inject
    lateinit var configurationRepository: ConfigurationRepository
    
    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var whitelistedPackages: Set<String> = emptySet()
    private var isKioskModeEnabled = false
    
    companion object {
        private const val TAG = "KioskAccessibility"
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
        
        serviceScope.launch {
            // Load configuration
            configurationRepository.configuration.collect { config ->
                isKioskModeEnabled = config.isKioskModeEnabled
                Log.d(TAG, "Kiosk mode enabled: $isKioskModeEnabled")
            }
        }
        
        serviceScope.launch {
            // Load whitelist
            whitelistRepository.getAllApps().collect { apps ->
                whitelistedPackages = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
                Log.d(TAG, "Whitelisted packages updated: ${whitelistedPackages.size} apps")
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isKioskModeEnabled) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            Log.d(TAG, "Window state changed: $packageName")
            
            // ALWAYS allow our own package (includes LauncherActivity and AdminSettingsActivity)
            if (packageName == this.packageName) {
                Log.d(TAG, "Allowing own package: $packageName")
                return
            }
            
            // Allow system UI and Android system
            if (packageName == "com.android.systemui" ||
                packageName == "android" ||
                packageName.startsWith("com.android.settings")) {
                Log.d(TAG, "Allowing system package: $packageName")
                return
            }
            
            // Check if app is whitelisted
            if (whitelistedPackages.contains(packageName)) {
                Log.d(TAG, "Allowing whitelisted app: $packageName")
                return
            }
            
            // Block non-whitelisted app
            Log.w(TAG, "BLOCKING non-whitelisted app: $packageName")
            blockApp()
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    /**
     * Block current app and return to launcher
     */
    private fun blockApp() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        
        // Perform global back action to close the non-whitelisted app
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
