package com.kv.kiosklauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kv.kiosklauncher.R
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import com.kv.kiosklauncher.util.LockTaskManager
import com.kv.kiosklauncher.util.ProcessKiller
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
 * Foreground service to maintain kiosk mode with aggressive enforcement
 */
@AndroidEntryPoint
class KioskService : Service() {
    
    @Inject
    lateinit var configurationRepository: ConfigurationRepository
    
    @Inject
    lateinit var lockTaskManager: LockTaskManager
    
    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    @Inject
    lateinit var processKiller: ProcessKiller
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kiosk_service_channel"
        private const val CHANNEL_NAME = "Kiosk Service"
        private const val ENFORCEMENT_INTERVAL_MS = 2000L // Check every 2 seconds
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            monitorKioskMode()
        }
        
        serviceScope.launch {
            enforceKioskMode()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    /**
     * Monitor kiosk mode and restart launcher if needed
     */
    private suspend fun monitorKioskMode() {
        val config = configurationRepository.configuration.first()
        
        if (config.isKioskModeEnabled) {
            // Ensure launcher is running
            ensureLauncherRunning()
        }
    }
    
    /**
     * Continuously enforce kiosk mode by killing non-whitelisted apps
     */
    private suspend fun enforceKioskMode() {
        while (serviceScope.isActive) {
            try {
                val config = configurationRepository.configuration.first()
                
                if (config.isKioskModeEnabled) {
                    // Get whitelisted packages
                    val whitelistedApps = whitelistRepository.getAllApps().first()
                    val whitelistedPackages = whitelistedApps
                        .filter { it.isEnabled }
                        .map { it.packageName }
                        .toSet()
                    
                    // Kill non-whitelisted background processes
                    processKiller.killNonWhitelistedApps(whitelistedPackages)
                }
            } catch (e: Exception) {
                // Continue enforcement even if there's an error
            }
            
            delay(ENFORCEMENT_INTERVAL_MS)
        }
    }
    
    /**
     * Ensure launcher activity is running
     */
    private fun ensureLauncherRunning() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps kiosk mode active and enforces restrictions"
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
            .setContentText("Device is locked in kiosk mode")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
