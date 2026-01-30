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
import com.kv.kiosklauncher.util.AppDetector
import com.kv.kiosklauncher.util.WhitelistChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * High-frequency foreground service that monitors for unauthorized app access.
 * Polls every 50-100ms to detect app changes and trigger blocking actions.
 */
@AndroidEntryPoint
class AppMonitorService : Service() {
    
    @Inject
    lateinit var appDetector: AppDetector
    
    @Inject
    lateinit var whitelistChecker: WhitelistChecker
    
    @Inject
    lateinit var blockingActionService: BlockingActionService
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null
    
    private var lastCheckedPackage: String? = null
    
    companion object {
        private const val TAG = "AppMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kiosk_monitor_channel"
        private const val MONITORING_INTERVAL_MS = 100L // 100ms polling
        
        const val ACTION_START_MONITORING = "com.kv.kiosklauncher.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.kv.kiosklauncher.STOP_MONITORING"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startMonitoring() {
        Log.d(TAG, "Starting app monitoring")
        
        // Start foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start monitoring loop
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkForegroundApp()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                }
            }
        }
    }
    
    private fun stopMonitoring() {
        Log.d(TAG, "Stopping app monitoring")
        monitoringJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private suspend fun checkForegroundApp() {
        // Check if session is still active
        if (!sessionManager.isSessionActive.value) {
            Log.d(TAG, "Session not active, stopping monitoring")
            stopMonitoring()
            return
        }
        
        // Check if session has expired
        if (sessionManager.isSessionExpired()) {
            Log.d(TAG, "Session expired, stopping monitoring")
            sessionManager.stopSession()
            stopMonitoring()
            return
        }
        
        // Get foreground app
        val foregroundPackage = appDetector.getForegroundAppPackageName()
        
        if (foregroundPackage != null && foregroundPackage != lastCheckedPackage) {
            lastCheckedPackage = foregroundPackage
            Log.d(TAG, "Detected app: $foregroundPackage")
            
            // Check if app should be blocked
            val shouldBlock = whitelistChecker.shouldBlock(foregroundPackage)
            Log.d(TAG, "Should block $foregroundPackage? $shouldBlock (whitelisted: ${!shouldBlock})")
            
            if (shouldBlock) {
                Log.w(TAG, "🚫 BLOCKING unauthorized app: $foregroundPackage")
                blockingActionService.blockApp(foregroundPackage)
            } else {
                Log.d(TAG, "✅ Allowing whitelisted app: $foregroundPackage")
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kiosk Mode Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage during kiosk mode"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val remainingTime = sessionManager.getRemainingTimeMs()
        val remainingMinutes = (remainingTime / 1000 / 60).toInt()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kiosk Mode Active")
            .setContentText("$remainingMinutes minutes remaining")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
