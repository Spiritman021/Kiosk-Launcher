package com.kv.kiosklauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kv.kiosklauncher.R
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import com.kv.kiosklauncher.util.ForegroundAppDetector
import com.kv.kiosklauncher.util.TaskKiller
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Continuous monitoring service that checks foreground app every 50ms
 * This is the CORE of the robust kiosk mode implementation
 */
@AndroidEntryPoint
class ContinuousMonitorService : Service() {
    
    @Inject
    lateinit var configurationRepository: ConfigurationRepository
    
    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    @Inject
    lateinit var foregroundAppDetector: ForegroundAppDetector
    
    @Inject
    lateinit var taskKiller: TaskKiller
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var whitelistedPackages: Set<String> = emptySet()
    private var lastBlockedApp: String? = null
    private var lastBlockTime: Long = 0
    
    companion object {
        private const val TAG = "ContinuousMonitor"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "continuous_monitor_channel"
        private const val CHANNEL_NAME = "Continuous Monitor"
        private const val MONITORING_INTERVAL_MS = 50L // 50ms = 20 checks per second
        private const val BLOCK_COOLDOWN_MS = 200L // Prevent repeated blocks of same app
        
        // System phone/dialer packages that should always be allowed
        private val SYSTEM_PHONE_PACKAGES = setOf(
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.incallui",
            "com.android.server.telecom"
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Load whitelist
        serviceScope.launch {
            whitelistRepository.getAllApps().collect { apps ->
                whitelistedPackages = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
                Log.d(TAG, "Whitelist updated: ${whitelistedPackages.size} apps")
            }
        }
        
        // Start continuous monitoring
        serviceScope.launch {
            startContinuousMonitoring()
        }
        
        Log.d(TAG, "Continuous monitor service started")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Continuous monitor service stopped")
    }
    
    /**
     * Continuously monitor foreground app every 50ms
     */
    private suspend fun startContinuousMonitoring() {
        Log.d(TAG, "Starting continuous monitoring (50ms interval)")
        
        while (serviceScope.isActive) {
            try {
                val config = configurationRepository.configuration.first()
                
                if (config.isKioskModeEnabled) {
                    checkAndBlockForegroundApp()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring loop", e)
            }
            
            delay(MONITORING_INTERVAL_MS)
        }
    }
    
    /**
     * Check foreground app and block if not whitelisted
     */
    private fun checkAndBlockForegroundApp() {
        val foregroundApp = foregroundAppDetector.getForegroundApp() ?: return
        
        // ALWAYS allow our own package
        if (foregroundApp == packageName) {
            return
        }
        
        // Allow system UI and Android system
        if (foregroundApp == "com.android.systemui" ||
            foregroundApp == "android" ||
            foregroundApp.startsWith("com.android.settings")) {
            return
        }
        
        // ALWAYS allow phone/dialer apps for emergency calls
        if (isPhoneOrDialer(foregroundApp)) {
            return
        }
        
        // Check if app is whitelisted
        if (whitelistedPackages.contains(foregroundApp)) {
            return
        }
        
        // Check cooldown to avoid repeated blocks
        val currentTime = System.currentTimeMillis()
        if (foregroundApp == lastBlockedApp && currentTime - lastBlockTime < BLOCK_COOLDOWN_MS) {
            return
        }
        
        // BLOCK THE APP
        Log.w(TAG, "🚫 BLOCKING non-whitelisted app: $foregroundApp")
        blockApp(foregroundApp)
        
        lastBlockedApp = foregroundApp
        lastBlockTime = currentTime
    }
    
    /**
     * Check if package is a phone or dialer app
     */
    private fun isPhoneOrDialer(packageName: String): Boolean {
        return SYSTEM_PHONE_PACKAGES.contains(packageName) ||
               packageName.contains("dialer", ignoreCase = true) ||
               packageName.contains("phone", ignoreCase = true) ||
               packageName.contains("call", ignoreCase = true)
    }
    
    /**
     * Block app using triple-layer approach
     */
    private fun blockApp(packageName: String) {
        // LAYER 1: Show overlay blocker (instant visual blocking)
        BlockerOverlayService.blockApp(this, packageName)
        
        // LAYER 2: Kill the app task
        taskKiller.aggressiveKill(packageName)
        
        // LAYER 3: Launch launcher (redundant, but ensures launcher is visible)
        launchKioskHome()
        
        Log.d(TAG, "✓ Blocked app with triple-layer protection: $packageName")
    }
    
    /**
     * Launch kiosk home
     */
    private fun launchKioskHome() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
    
    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuously monitors and blocks non-whitelisted apps"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground service notification
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, LauncherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kiosk Mode Active")
            .setContentText("Monitoring apps every 50ms")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
