package com.kv.kiosklauncher.util

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.kv.kiosklauncher.presentation.launcher.LauncherActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor for app launches and redirect to launcher if not whitelisted
 */
@Singleton
class AppLaunchMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var monitoringScope: CoroutineScope? = null
    private var whitelistedPackages: Set<String> = emptySet()
    
    companion object {
        private const val TAG = "AppLaunchMonitor"
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )
            return stats != null && stats.isNotEmpty()
        }
        return false
    }
    
    /**
     * Get intent to request usage stats permission
     */
    fun getUsageStatsPermissionIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
    
    /**
     * Start monitoring app launches
     */
    fun startMonitoring(whitelistedPackages: Set<String>) {
        this.whitelistedPackages = whitelistedPackages
        Log.d(TAG, "Starting monitoring with ${whitelistedPackages.size} whitelisted apps")
        
        if (monitoringScope?.isActive == true) {
            return // Already monitoring
        }
        
        monitoringScope = CoroutineScope(Dispatchers.Default).apply {
            launch {
                while (isActive) {
                    checkForegroundApp()
                    delay(1000) // Check every 1 second (reduced from 500ms to be less aggressive)
                }
            }
        }
    }
    
    /**
     * Stop monitoring app launches
     */
    fun stopMonitoring() {
        monitoringScope?.let { scope ->
            if (scope.isActive) {
                // Cancel the scope
                monitoringScope = null
            }
        }
    }
    
    /**
     * Check if foreground app is whitelisted
     */
    private fun checkForegroundApp() {
        val foregroundPackage = getForegroundPackage() ?: return
        
        // ALWAYS allow our own package (includes LauncherActivity and AdminSettingsActivity)
        if (foregroundPackage == context.packageName) {
            return
        }
        
        // Allow system UI and settings
        if (foregroundPackage == "com.android.systemui" ||
            foregroundPackage.startsWith("com.android.settings")) {
            return
        }
        
        // Check if app is whitelisted
        if (whitelistedPackages.contains(foregroundPackage)) {
            Log.d(TAG, "Allowing whitelisted app: $foregroundPackage")
            return
        }
        
        // If not whitelisted, bring launcher to front
        Log.w(TAG, "BLOCKING non-whitelisted app: $foregroundPackage")
        bringLauncherToFront()
    }
    
    /**
     * Get foreground app package name
     */
    private fun getForegroundPackage(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )
            
            if (stats != null && stats.isNotEmpty()) {
                // Get most recent app
                val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
                return sortedStats.firstOrNull()?.packageName
            }
        } else {
            // Fallback for older devices
            @Suppress("DEPRECATION")
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = activityManager.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                return tasks[0].topActivity?.packageName
            }
        }
        
        return null
    }
    
    /**
     * Bring launcher to front
     */
    private fun bringLauncherToFront() {
        val intent = Intent(context, LauncherActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }
}
