package com.kv.kiosklauncher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Boot receiver to restart kiosk mode after device reboot
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var configurationRepository: ConfigurationRepository
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking kiosk mode status")
            
            // Check if kiosk mode was enabled before reboot
            CoroutineScope(Dispatchers.IO).launch {
                val config = configurationRepository.configuration.first()
                
                if (config.isKioskModeEnabled) {
                    Log.d(TAG, "Kiosk mode was enabled, restarting launcher")
                    
                    // Start launcher activity
                    val launchIntent = Intent(context, LauncherActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(launchIntent)
                }
            }
        }
    }
}
