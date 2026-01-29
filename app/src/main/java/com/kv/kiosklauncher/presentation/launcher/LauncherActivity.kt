package com.kv.kiosklauncher.presentation.launcher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.presentation.theme.KioskLauncherTheme
import com.kv.kiosklauncher.service.KioskService
import com.kv.kiosklauncher.util.AppLaunchMonitor
import com.kv.kiosklauncher.util.LockTaskManager
import com.kv.kiosklauncher.util.SystemUIManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main launcher activity for kiosk mode
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {
    
    @Inject
    lateinit var lockTaskManager: LockTaskManager
    
    @Inject
    lateinit var systemUIManager: SystemUIManager
    
    @Inject
    lateinit var appLaunchMonitor: AppLaunchMonitor
    
    @Inject
    lateinit var configurationRepository: ConfigurationRepository
    
    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply kiosk mode window flags BEFORE setting content
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        enableEdgeToEdge()
        
        setContent {
            KioskLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen()
                }
            }
        }
        
        // Initialize kiosk mode
        initializeKioskMode()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Apply kiosk mode settings every time we resume
        lifecycleScope.launch {
            applyKioskModeSettings()
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        
        // Reapply system UI hiding when window regains focus
        if (hasFocus) {
            lifecycleScope.launch {
                val config = configurationRepository.configuration.first()
                if (config.isKioskModeEnabled) {
                    systemUIManager.hideSystemUI(window)
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Don't force launcher to front - let accessibility service and app monitor handle blocking
        // This allows AdminSettingsActivity and whitelisted apps to work properly
    }
    
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Disable back button in kiosk mode
        lifecycleScope.launch {
            val config = configurationRepository.configuration.first()
            if (!config.isKioskModeEnabled) {
                super.onBackPressed()
            }
            // Otherwise, do nothing (back button disabled)
        }
    }
    
    /**
     * Initialize kiosk mode features
     */
    private fun initializeKioskMode() {
        lifecycleScope.launch {
            val config = configurationRepository.configuration.first()
            
            if (config.isKioskModeEnabled) {
                // Start kiosk service
                val serviceIntent = Intent(this@LauncherActivity, KioskService::class.java)
                startForegroundService(serviceIntent)
                
                // Start app launch monitoring
                val whitelistedPackages = whitelistRepository.getAllApps().first()
                    .filter { it.isEnabled }
                    .map { it.packageName }
                    .toSet()
                
                if (appLaunchMonitor.hasUsageStatsPermission()) {
                    appLaunchMonitor.startMonitoring(whitelistedPackages)
                }
            }
        }
    }
    
    /**
     * Apply kiosk mode settings
     */
    private suspend fun applyKioskModeSettings() {
        val config = configurationRepository.configuration.first()
        
        if (!config.isKioskModeEnabled) {
            // Remove kiosk restrictions if disabled
            systemUIManager.removeKioskMode(window)
            return
        }
        
        // Try Lock Task Mode first (Android 5.0+)
        if (config.useLockTaskMode && lockTaskManager.isLockTaskModeSupported()) {
            if (lockTaskManager.isDeviceOwner()) {
                lockTaskManager.startLockTask(this)
                // Lock Task Mode handles status bar blocking
                systemUIManager.hideSystemUI(window)
                systemUIManager.keepScreenOn(window)
            } else {
                // No device owner - use system UI hiding only
                // StatusBarBlockerService handles the overlay
                systemUIManager.hideSystemUI(window)
                systemUIManager.disableStatusBarExpansion(window)
                systemUIManager.keepScreenOn(window)
            }
        } else {
            // Fallback to system UI hiding
            // StatusBarBlockerService handles the overlay
            systemUIManager.hideSystemUI(window)
            systemUIManager.disableStatusBarExpansion(window)
            systemUIManager.keepScreenOn(window)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop monitoring when activity is destroyed
        appLaunchMonitor.stopMonitoring()
        
        // StatusBarBlockerService handles overlay cleanup
    }
}
