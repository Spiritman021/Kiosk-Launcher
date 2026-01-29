package com.kv.kiosklauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import com.kv.kiosklauncher.util.ScreenManager
import com.kv.kiosklauncher.util.TaskKiller
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
    
    @Inject
    lateinit var screenManager: ScreenManager
    
    @Inject
    lateinit var taskKiller: TaskKiller
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var whitelistedPackages: Set<String> = emptySet()
    private var isKioskModeEnabled = false
    
    companion object {
        private const val TAG = "KioskAccessibility"
        
        // System phone/dialer packages that should always be allowed for emergency calls
        private val SYSTEM_PHONE_PACKAGES = setOf(
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.incallui",
            "com.android.server.telecom"
        )
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
            
            // ALWAYS allow phone/dialer apps for emergency calls
            if (isPhoneOrDialer(packageName)) {
                Log.d(TAG, "Allowing phone/dialer app: $packageName")
                return
            }
            
            // Check if app is whitelisted
            if (whitelistedPackages.contains(packageName)) {
                Log.d(TAG, "Allowing whitelisted app: $packageName")
                return
            }
            
            // Block non-whitelisted app with aggressive methods
            Log.w(TAG, "BLOCKING non-whitelisted app: $packageName")
            blockApp(packageName)
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
     * Check if package is a phone or dialer app (for emergency calls)
     */
    private fun isPhoneOrDialer(packageName: String): Boolean {
        return SYSTEM_PHONE_PACKAGES.contains(packageName) ||
               packageName.contains("dialer", ignoreCase = true) ||
               packageName.contains("phone", ignoreCase = true) ||
               packageName.contains("call", ignoreCase = true)
    }
    
    /**
     * Block current app and return to launcher
     * Uses multiple aggressive methods for instant blocking
     */
    private fun blockApp(packageName: String) {
        Log.d(TAG, "Initiating aggressive block for: $packageName")
        
        // METHOD 1: Turn screen off (most effective - like BlockIt)
        if (screenManager.turnScreenOff()) {
            Log.d(TAG, "✓ Screen turned off for blocked app: $packageName")
            // Also kill the task so it doesn't resume when screen turns back on
            taskKiller.aggressiveKill(packageName)
            return
        }
        
        // METHOD 2: Aggressive task killing
        Log.d(TAG, "Screen-off unavailable, using task killer")
        taskKiller.aggressiveKill(packageName)
        
        // METHOD 3: Multiple global actions to force exit
        performGlobalAction(GLOBAL_ACTION_HOME)
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        // METHOD 4: Launch launcher activity
        val intent = Intent(this, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        
        Log.d(TAG, "✓ Blocked app using fallback methods: $packageName")
    }
}
