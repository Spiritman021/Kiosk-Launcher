package com.kv.kiosklauncher.util

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized foreground app detector with caching
 * Provides fast, efficient detection of the currently running app
 */
@Singleton
class ForegroundAppDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    
    private var lastForegroundApp: String? = null
    private var lastCheckTime: Long = 0
    private val cacheValidityMs = 40 // Cache for 40ms (faster than 50ms polling)
    
    companion object {
        private const val TAG = "ForegroundAppDetector"
    }
    
    /**
     * Get the current foreground app package name
     * Uses caching to avoid excessive system calls
     */
    fun getForegroundApp(): String? {
        val currentTime = System.currentTimeMillis()
        
        // Return cached value if still valid
        if (currentTime - lastCheckTime < cacheValidityMs && lastForegroundApp != null) {
            return lastForegroundApp
        }
        
        // Get fresh value
        val foregroundApp = detectForegroundApp()
        lastForegroundApp = foregroundApp
        lastCheckTime = currentTime
        
        return foregroundApp
    }
    
    /**
     * Detect the foreground app using UsageStatsManager
     */
    private fun detectForegroundApp(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null
        }
        
        return try {
            val time = System.currentTimeMillis()
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10, // Last 10 seconds
                time
            )
            
            if (stats != null && stats.isNotEmpty()) {
                // Get most recently used app
                val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
                val foregroundPackage = sortedStats.firstOrNull()?.packageName
                
                if (foregroundPackage != null && foregroundPackage != lastForegroundApp) {
                    Log.d(TAG, "Foreground app changed: $foregroundPackage")
                }
                
                foregroundPackage
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting foreground app", e)
            null
        }
    }
    
    /**
     * Clear the cache (useful when kiosk mode state changes)
     */
    fun clearCache() {
        lastForegroundApp = null
        lastCheckTime = 0
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false
        }
        
        return try {
            val time = System.currentTimeMillis()
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
