package com.kv.kiosklauncher.util

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for hiding system UI and blocking status bar
 */
@Singleton
class SystemUIManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var statusBarBlockerView: View? = null
    private val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    /**
     * Hide system UI (status bar and navigation bar)
     */
    fun hideSystemUI(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.apply {
                hide(WindowInsetsCompat.Type.statusBars())
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    /**
     * Disable status bar expansion (notification shade)
     */
    fun disableStatusBarExpansion(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            
            // Disable status bar expansion
            try {
                @Suppress("DEPRECATION")
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Keep screen on
     */
    fun keepScreenOn(window: Window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    /**
     * Add overlay to block status bar interaction
     * This creates an invisible overlay at the top of the screen
     */
    fun addStatusBarBlocker(activity: Activity) {
        try {
            removeStatusBarBlocker() // Remove existing if any
            
            // Create a blocking view
            val blockerView = View(activity).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setOnTouchListener { _, _ -> true } // Consume all touch events
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                getStatusBarHeight() + 50, // Cover status bar + extra
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = android.view.Gravity.TOP
                y = 0
            }
            
            windowManager.addView(blockerView, params)
            statusBarBlockerView = blockerView
        } catch (e: Exception) {
            // May fail if SYSTEM_ALERT_WINDOW permission not granted
        }
    }
    
    /**
     * Remove status bar blocker overlay
     */
    fun removeStatusBarBlocker() {
        statusBarBlockerView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View may already be removed
            }
            statusBarBlockerView = null
        }
    }
    
    /**
     * Get status bar height
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return if (result > 0) result else 75 // Default to 75px if not found
    }
    
    /**
     * Disable all system UI interactions
     */
    fun applyFullKioskMode(window: Window, activity: Activity) {
        // Hide system UI
        hideSystemUI(window)
        
        // Disable status bar expansion
        disableStatusBarExpansion(window)
        
        // Keep screen on
        keepScreenOn(window)
        
        // Add status bar blocker overlay
        addStatusBarBlocker(activity)
        
        // Set window flags for maximum restriction
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        }
    }
    
    /**
     * Remove all kiosk mode restrictions
     */
    fun removeKioskMode(window: Window) {
        // Remove status bar blocker
        removeStatusBarBlocker()
        
        // Show system UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller?.apply {
                show(WindowInsetsCompat.Type.statusBars())
                show(WindowInsetsCompat.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        
        // Remove window flags
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
