package com.kv.kiosklauncher.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.kv.kiosklauncher.receiver.KioskDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for screen control and device locking
 * Used to turn screen off when non-whitelisted apps are accessed
 */
@Singleton
class ScreenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    
    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val adminComponent: ComponentName =
        ComponentName(context, KioskDeviceAdmin::class.java)
    
    companion object {
        private const val TAG = "ScreenManager"
    }
    
    /**
     * Check if device admin is active
     */
    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    /**
     * Turn screen off immediately using device admin
     * This is the most effective blocking method
     */
    fun turnScreenOff(): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                // Lock the device immediately - this turns off the screen
                devicePolicyManager.lockNow()
                Log.d(TAG, "Screen turned off via device admin")
                true
            } else {
                Log.w(TAG, "Cannot turn screen off - device admin not active")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn screen off", e)
            false
        }
    }
    
    /**
     * Lock device (same as turnScreenOff, kept for clarity)
     */
    fun lockDevice(): Boolean {
        return turnScreenOff()
    }
    
    /**
     * Check if screen is currently on
     */
    fun isScreenOn(): Boolean {
        return powerManager.isInteractive
    }
    
    /**
     * Set screen timeout (requires device admin)
     */
    fun setScreenTimeout(timeoutMs: Long): Boolean {
        return try {
            if (isDeviceAdminActive()) {
                devicePolicyManager.setMaximumTimeToLock(adminComponent, timeoutMs)
                Log.d(TAG, "Screen timeout set to ${timeoutMs}ms")
                true
            } else {
                Log.w(TAG, "Cannot set screen timeout - device admin not active")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set screen timeout", e)
            false
        }
    }
    
    /**
     * Get current screen timeout
     */
    fun getScreenTimeout(): Long {
        return try {
            if (isDeviceAdminActive()) {
                devicePolicyManager.getMaximumTimeToLock(adminComponent)
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get screen timeout", e)
            0L
        }
    }
}
