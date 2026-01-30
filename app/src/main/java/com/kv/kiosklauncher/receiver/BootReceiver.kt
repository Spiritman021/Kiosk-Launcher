package com.kv.kiosklauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kv.kiosklauncher.service.AppMonitorService
import com.kv.kiosklauncher.service.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Boot receiver to restart kiosk mode monitoring after device reboot.
 * Ensures session continues if device is restarted during active session.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, checking for active session")
            
            scope.launch {
                // Load active session from database
                sessionManager.loadActiveSession()
                
                // If there's an active session, restart monitoring
                if (sessionManager.isSessionActive.value) {
                    Log.d(TAG, "Active session found, restarting monitoring")
                    val monitorIntent = Intent(context, AppMonitorService::class.java).apply {
                        action = AppMonitorService.ACTION_START_MONITORING
                    }
                    context.startForegroundService(monitorIntent)
                }
            }
        }
    }
}
