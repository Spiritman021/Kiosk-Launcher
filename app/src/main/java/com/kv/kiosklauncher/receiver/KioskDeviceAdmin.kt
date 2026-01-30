package com.kv.kiosklauncher.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Admin Receiver for screen lock capability.
 * Required for turning off the screen when blocking apps.
 */
class KioskDeviceAdmin : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "KioskDeviceAdmin"
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling Device Admin will prevent kiosk mode from locking the screen when blocking apps."
    }
}
