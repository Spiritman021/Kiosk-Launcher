package com.kv.kiosklauncher.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.kv.kiosklauncher.R
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Full-screen overlay service that instantly blocks non-whitelisted apps
 * This is the KEY component for BlockIt-style instant blocking
 */
@AndroidEntryPoint
class BlockerOverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isOverlayVisible = false
    
    companion object {
        private const val TAG = "BlockerOverlay"
        private const val ACTION_BLOCK_APP = "com.kv.kiosklauncher.BLOCK_APP"
        private const val EXTRA_PACKAGE_NAME = "package_name"
        
        fun start(context: Context) {
            val intent = Intent(context, BlockerOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, BlockerOverlayService::class.java)
            context.stopService(intent)
        }
        
        fun blockApp(context: Context, packageName: String) {
            val intent = Intent(context, BlockerOverlayService::class.java).apply {
                action = ACTION_BLOCK_APP
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlay()
        Log.d(TAG, "Blocker overlay service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_BLOCK_APP) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            Log.w(TAG, "Blocking app: $packageName")
            showOverlay(packageName)
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        serviceScope.cancel()
    }
    
    /**
     * Create the full-screen overlay
     */
    private fun createOverlay() {
        try {
            val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )
            }
            
            overlayView = LayoutInflater.from(this).inflate(R.layout.blocker_overlay, null)
            overlayView?.alpha = 0f // Start transparent
            overlayView?.visibility = View.GONE
            
            windowManager.addView(overlayView, layoutParams)
            Log.d(TAG, "Overlay created and added to window")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create overlay", e)
        }
    }
    
    /**
     * Show the overlay instantly to block an app
     */
    private fun showOverlay(packageName: String?) {
        serviceScope.launch {
            try {
                overlayView?.let { view ->
                    // Make overlay visible INSTANTLY (0ms animation)
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                    isOverlayVisible = true
                    
                    Log.d(TAG, "✓ Overlay shown instantly for: $packageName")
                    
                    // Launch launcher activity
                    launchKioskHome()
                    
                    // Hide overlay after launcher is visible (500ms delay)
                    delay(500)
                    hideOverlay()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
            }
        }
    }
    
    /**
     * Hide the overlay
     */
    private fun hideOverlay() {
        try {
            overlayView?.let { view ->
                view.alpha = 0f
                view.visibility = View.GONE
                isOverlayVisible = false
                Log.d(TAG, "Overlay hidden")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay", e)
        }
    }
    
    /**
     * Remove the overlay from window
     */
    private fun removeOverlay() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
                Log.d(TAG, "Overlay removed from window")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay", e)
        }
    }
    
    /**
     * Launch kiosk home (launcher activity)
     */
    private fun launchKioskHome() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        Log.d(TAG, "Launched kiosk home")
    }
}
