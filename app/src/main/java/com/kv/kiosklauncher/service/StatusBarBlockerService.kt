package com.kv.kiosklauncher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.kv.kiosklauncher.R
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
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
 * Foreground service that maintains a system-wide overlay to block status bar
 * This overlay persists across all apps, preventing notification bar access
 * Similar to BlockIt app functionality
 */
@AndroidEntryPoint
class StatusBarBlockerService : Service() {
    
    @Inject
    lateinit var configurationRepository: ConfigurationRepository
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var statusBarBlockerView: View? = null
    private lateinit var windowManager: WindowManager
    
    companion object {
        private const val TAG = "StatusBarBlocker"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "status_bar_blocker_channel"
        private const val CHANNEL_NAME = "Status Bar Blocker"
        
        fun start(context: Context) {
            val intent = Intent(context, StatusBarBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, StatusBarBlockerService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        Log.d(TAG, "StatusBarBlockerService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val config = configurationRepository.configuration.first()
            
            if (config.isKioskModeEnabled) {
                addStatusBarBlocker()
            } else {
                // Kiosk mode disabled, stop service
                stopSelf()
            }
        }
        
        // Monitor configuration changes
        serviceScope.launch {
            configurationRepository.configuration.collect { config ->
                if (config.isKioskModeEnabled) {
                    if (statusBarBlockerView == null) {
                        addStatusBarBlocker()
                    }
                } else {
                    removeStatusBarBlocker()
                    stopSelf()
                }
            }
        }
        
        return START_STICKY // Auto-restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        removeStatusBarBlocker()
        serviceScope.cancel()
        Log.d(TAG, "StatusBarBlockerService destroyed")
    }
    
    /**
     * Add system-wide overlay to block status bar
     */
    private fun addStatusBarBlocker() {
        if (statusBarBlockerView != null) {
            Log.d(TAG, "Status bar blocker already active")
            return
        }
        
        try {
            // Create transparent blocking view
            val blockerView = View(this).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                // Consume all touch events
                setOnTouchListener { _, event ->
                    Log.d(TAG, "Status bar touch blocked: ${event.action}")
                    true // Block the touch
                }
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                getStatusBarHeight() + 100, // Cover status bar + extra
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = 0
            }
            
            windowManager.addView(blockerView, params)
            statusBarBlockerView = blockerView
            
            Log.d(TAG, "Status bar blocker overlay added (system-wide)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add status bar blocker", e)
            // May fail if SYSTEM_ALERT_WINDOW permission not granted
        }
    }
    
    /**
     * Remove status bar blocker overlay
     */
    private fun removeStatusBarBlocker() {
        statusBarBlockerView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Status bar blocker overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove status bar blocker", e)
            }
            statusBarBlockerView = null
        }
    }
    
    /**
     * Get status bar height
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return if (result > 0) result else 75 // Default to 75px if not found
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
                description = "Blocks status bar access in kiosk mode"
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
            .setContentTitle("Status Bar Blocked")
            .setContentText("Kiosk mode: Status bar access restricted")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
