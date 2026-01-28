package com.kv.kiosklauncher.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for killing background processes of non-whitelisted apps
 */
@Singleton
class ProcessKiller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * Kill all non-whitelisted apps
     */
    fun killNonWhitelistedApps(whitelistedPackages: Set<String>) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Get running app processes
                val runningProcesses = activityManager.runningAppProcesses ?: return
                
                for (processInfo in runningProcesses) {
                    val packageName = processInfo.processName
                    
                    // Skip our own package, system apps, whitelisted apps, and settings
                    if (packageName == context.packageName ||
                        packageName.startsWith("com.android") ||
                        packageName.startsWith("android") ||
                        packageName == "com.android.systemui" ||
                        whitelistedPackages.contains(packageName)) {
                        continue
                    }
                    
                    // Kill background process
                    activityManager.killBackgroundProcesses(packageName)
                }
            }
        } catch (e: Exception) {
            // Silently fail if we don't have permission
        }
    }
    
    /**
     * Kill a specific app
     */
    fun killApp(packageName: String) {
        try {
            activityManager.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            // Silently fail if we don't have permission
        }
    }
    
    /**
     * Clear app data (requires system permissions)
     * Note: This method requires device owner or system permissions
     */
    fun clearAppData(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // This requires device owner permissions
                // activityManager.clearApplicationUserData() only works for own package
                // For other packages, would need device owner DPM.clearApplicationUserData()
                false // Not implemented without device owner
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
