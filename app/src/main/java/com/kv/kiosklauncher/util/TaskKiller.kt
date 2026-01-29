package com.kv.kiosklauncher.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggressive task killer for removing non-whitelisted apps from recents and memory
 */
@Singleton
class TaskKiller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    companion object {
        private const val TAG = "TaskKiller"
    }
    
    /**
     * Kill app task immediately and remove from recents
     */
    fun killAppTask(packageName: String) {
        try {
            // Method 1: Kill background processes
            activityManager.killBackgroundProcesses(packageName)
            Log.d(TAG, "Killed background processes for: $packageName")
            
            // Method 2: Remove from recents (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                removeFromRecents(packageName)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill app task: $packageName", e)
        }
    }
    
    /**
     * Remove app from recent tasks list
     */
    fun removeFromRecents(packageName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val recentTasks = activityManager.appTasks
                
                for (task in recentTasks) {
                    try {
                        val taskInfo = task.taskInfo
                        val taskPackageName = taskInfo.baseIntent?.component?.packageName
                        
                        if (taskPackageName == packageName) {
                            // Remove this task
                            task.finishAndRemoveTask()
                            Log.d(TAG, "Removed task from recents: $packageName")
                        }
                    } catch (e: Exception) {
                        // Continue to next task
                        Log.w(TAG, "Failed to check task", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove from recents: $packageName", e)
        }
    }
    
    /**
     * Force stop app (requires system permissions - will fail on non-rooted devices)
     * This is a best-effort attempt
     */
    fun forceStopApp(packageName: String): Boolean {
        return try {
            // This method exists but requires system permissions
            // On non-rooted devices, this will fail silently
            val method = activityManager.javaClass.getMethod(
                "forceStopPackage",
                String::class.java
            )
            method.invoke(activityManager, packageName)
            Log.d(TAG, "Force stopped app: $packageName")
            true
        } catch (e: Exception) {
            // Expected to fail on non-rooted devices
            Log.d(TAG, "Cannot force stop (requires system permissions): $packageName")
            false
        }
    }
    
    /**
     * Aggressive kill - try all methods
     */
    fun aggressiveKill(packageName: String) {
        Log.d(TAG, "Aggressive kill initiated for: $packageName")
        
        // Try all methods in sequence
        killAppTask(packageName)
        forceStopApp(packageName)
        
        // Kill again for good measure
        activityManager.killBackgroundProcesses(packageName)
    }
}
