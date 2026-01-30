package com.kv.kiosklauncher.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.kv.kiosklauncher.receiver.KioskDeviceAdmin

/**
 * Helper class for checking and requesting required permissions.
 */
class PermissionHelper(private val context: Context) {
    
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val deviceAdminComponent: ComponentName by lazy {
        ComponentName(context, KioskDeviceAdmin::class.java)
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(): Boolean {
        return hasUsageStatsPermission() &&
                hasOverlayPermission() &&
                isAccessibilityServiceEnabled() &&
                isDeviceAdminEnabled() &&
                isBatteryOptimizationDisabled()
    }
    
    /**
     * Check if Usage Stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appDetector = AppDetector(context)
        return appDetector.hasUsageStatsPermission()
    }
    
    /**
     * Check if Overlay permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Check if Accessibility Service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "${context.packageName}/.service.KioskAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName) == true
    }
    
    /**
     * Check if Device Admin is enabled
     */
    fun isDeviceAdminEnabled(): Boolean {
        return devicePolicyManager.isAdminActive(deviceAdminComponent)
    }
    
    /**
     * Check if battery optimization is disabled for this app
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    
    /**
     * Open Usage Stats settings
     */
    fun openUsageStatsSettings(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
    
    /**
     * Open Overlay permission settings
     */
    fun openOverlaySettings(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }
    
    /**
     * Open Accessibility settings
     */
    fun openAccessibilitySettings(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
    
    /**
     * Open Device Admin settings to enable admin
     */
    fun openDeviceAdminSettings(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable Device Admin to allow kiosk mode to lock the screen when blocking unauthorized apps."
            )
        }
    }
    
    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }
    
    /**
     * Get list of missing permissions with user-friendly names
     */
    fun getMissingPermissions(): List<Pair<String, String>> {
        val missing = mutableListOf<Pair<String, String>>()
        
        if (!hasUsageStatsPermission()) {
            missing.add("Usage Stats" to "Required to detect which app is currently running")
        }
        if (!hasOverlayPermission()) {
            missing.add("Display over other apps" to "Required to show blocking overlay")
        }
        if (!isAccessibilityServiceEnabled()) {
            missing.add("Accessibility Service" to "Required for instant app detection")
        }
        if (!isDeviceAdminEnabled()) {
            missing.add("Device Admin" to "Required to lock screen when blocking apps")
        }
        if (!isBatteryOptimizationDisabled()) {
            missing.add("Battery Optimization" to "Required to keep monitoring service running")
        }
        
        return missing
    }
}
