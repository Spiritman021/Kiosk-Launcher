package com.kv.kiosklauncher.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.kv.kiosklauncher.util.WhitelistChecker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility Service for instant window change detection.
 * Provides faster app detection than polling alone.
 * Works in conjunction with AppMonitorService for maximum reliability.
 */
@AndroidEntryPoint
class KioskAccessibilityService : AccessibilityService() {
    
    @Inject
    lateinit var whitelistChecker: WhitelistChecker
    
    @Inject
    lateinit var blockingActionService: BlockingActionService
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    companion object {
        private const val TAG = "KioskAccessibility"
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Only process window state changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        // Check if session is active
        if (!sessionManager.isSessionActive.value) {
            return
        }
        
        // Get package name of the app that triggered the event
        val packageName = event.packageName?.toString() ?: return
        
        // Ignore if it's our own package or system UI
        if (packageName == this.packageName || packageName == "com.android.systemui") {
            return
        }
        
        // Check if app should be blocked
        serviceScope.launch {
            if (whitelistChecker.shouldBlock(packageName)) {
                Log.d(TAG, "Accessibility detected unauthorized app: $packageName")
                blockingActionService.blockApp(packageName)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Accessibility service destroyed")
    }
}
