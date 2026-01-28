package com.kv.kiosklauncher.util

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.kv.kiosklauncher.receiver.KioskDeviceAdmin
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Lock Task Mode (Kiosk Mode)
 */
@Singleton
class LockTaskManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    
    private val adminComponent = ComponentName(context, KioskDeviceAdmin::class.java)
    
    /**
     * Check if device admin is enabled
     */
    fun isDeviceAdminEnabled(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    /**
     * Check if app is device owner (required for Lock Task Mode on Android 9+)
     */
    fun isDeviceOwner(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            devicePolicyManager.isDeviceOwnerApp(context.packageName)
        } else {
            false
        }
    }
    
    /**
     * Check if Lock Task Mode is supported
     */
    fun isLockTaskModeSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }
    
    /**
     * Check if currently in Lock Task Mode
     * Note: There's no direct API to check this, would need ActivityManager
     */
    fun isLockTaskModeActive(): Boolean {
        // This would require ActivityManager.getLockTaskModeState() which needs Activity context
        // For now, return false as this is mainly informational
        return false
    }
    
    /**
     * Set Lock Task packages (requires device owner)
     */
    fun setLockTaskPackages(packages: Array<String>): Boolean {
        return try {
            if (isDeviceOwner() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                devicePolicyManager.setLockTaskPackages(adminComponent, packages)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Start Lock Task Mode for an activity
     */
    fun startLockTask(activity: Activity): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Set this app as allowed for Lock Task Mode
                if (isDeviceOwner()) {
                    setLockTaskPackages(arrayOf(context.packageName))
                }
                activity.startLockTask()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Stop Lock Task Mode
     */
    fun stopLockTask(activity: Activity): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.stopLockTask()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get intent to enable device admin
     */
    fun getEnableDeviceAdminIntent(): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable device admin to use kiosk mode features"
        )
        return intent
    }
    
    /**
     * Get ADB command to set device owner
     */
    fun getDeviceOwnerAdbCommand(): String {
        return "adb shell dpm set-device-owner ${context.packageName}/${KioskDeviceAdmin::class.java.name}"
    }
    
    /**
     * Check if device is in developer mode
     */
    fun isDeveloperModeEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }
    
    /**
     * Get setup instructions for Lock Task Mode
     */
    fun getSetupInstructions(): String {
        return when {
            isDeviceOwner() -> "Device owner is set. Lock Task Mode is ready."
            isDeviceAdminEnabled() -> "Device admin is enabled, but device owner is not set. Run ADB command:\n${getDeviceOwnerAdbCommand()}"
            else -> "Device admin is not enabled. Enable it first, then run ADB command to set device owner."
        }
    }
}
