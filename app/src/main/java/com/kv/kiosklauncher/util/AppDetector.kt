package com.kv.kiosklauncher.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for detecting the currently running foreground app.
 * Uses multiple methods for maximum compatibility and reliability.
 */
@Singleton
class AppDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }
    
    private val packageManager: PackageManager by lazy {
        context.packageManager
    }
    
    /**
     * Get the package name of the currently running foreground app.
     * Returns null if unable to detect.
     */
    fun getForegroundAppPackageName(): String? {
        // Primary method: UsageStatsManager (most reliable on modern Android)
        if (hasUsageStatsPermission()) {
            val foregroundApp = getForegroundAppFromUsageStats()
            if (foregroundApp != null) {
                return foregroundApp
            }
        }
        
        // Fallback: Return null if no method works
        // Accessibility service will be the backup detection method
        return null
    }
    
    /**
     * Get foreground app using UsageStatsManager.
     * This is the most reliable method on Android 5.0+
     */
    private fun getForegroundAppFromUsageStats(): String? {
        try {
            val now = System.currentTimeMillis()
            
            // Query usage stats for the last 1 second
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000,
                now
            )
            
            if (stats.isNullOrEmpty()) {
                return null
            }
            
            // Find the app with the most recent last time used
            val mostRecent = stats.maxByOrNull { it.lastTimeUsed }
            return mostRecent?.packageName
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Check if app has Usage Stats permission
     */
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get app name from package name
     */
    fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    /**
     * Check if an app is a system app
     */
    fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
