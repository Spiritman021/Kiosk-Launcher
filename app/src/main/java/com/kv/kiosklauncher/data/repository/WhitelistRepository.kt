package com.kv.kiosklauncher.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.kv.kiosklauncher.data.dao.WhitelistedAppDao
import com.kv.kiosklauncher.data.model.AppInfo
import com.kv.kiosklauncher.data.model.WhitelistedApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing whitelisted apps.
 * Handles both database operations and package manager queries.
 */
@Singleton
class WhitelistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whitelistedAppDao: WhitelistedAppDao
) {
    
    private val packageManager: PackageManager = context.packageManager
    
    /**
     * Get all installed apps on the device
     */
    suspend fun getAllInstalledApps(): List<AppInfo> {
        val whitelistedPackages = whitelistedAppDao.getAllWhitelistedPackageNames().toSet()
        
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                // Filter out this app itself
                appInfo.packageName != context.packageName &&
                // Has a launch intent (is launchable)
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isWhitelisted = whitelistedPackages.contains(appInfo.packageName)
                )
            }
            .sortedBy { it.appName }
    }
    
    /**
     * Get all whitelisted apps
     */
    fun getWhitelistedApps(): Flow<List<WhitelistedApp>> {
        return whitelistedAppDao.getAllWhitelistedApps()
    }
    
    /**
     * Add an app to whitelist
     */
    suspend fun addToWhitelist(appInfo: AppInfo) {
        val whitelistedApp = WhitelistedApp(
            packageName = appInfo.packageName,
            appName = appInfo.appName,
            isSystemApp = appInfo.isSystemApp,
            isAutoWhitelisted = false
        )
        whitelistedAppDao.insertWhitelistedApp(whitelistedApp)
    }
    
    /**
     * Remove an app from whitelist
     */
    suspend fun removeFromWhitelist(packageName: String) {
        whitelistedAppDao.deleteByPackageName(packageName)
    }
    
    /**
     * Check if an app is whitelisted
     */
    suspend fun isWhitelisted(packageName: String): Boolean {
        return whitelistedAppDao.isAppWhitelisted(packageName)
    }
    
    /**
     * Auto-whitelist phone/dialer app
     */
    suspend fun autoWhitelistPhoneApp() {
        val dialerPackages = listOf(
            "com.android.dialer",
            "com.android.phone",
            "com.google.android.dialer",
            "com.samsung.android.dialer"
        )
        
        dialerPackages.forEach { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                val whitelistedApp = WhitelistedApp(
                    packageName = packageName,
                    appName = appName,
                    isSystemApp = true,
                    isAutoWhitelisted = true
                )
                whitelistedAppDao.insertWhitelistedApp(whitelistedApp)
            } catch (e: PackageManager.NameNotFoundException) {
                // App not installed, skip
            }
        }
    }
    
    /**
     * Auto-whitelist the kiosk launcher itself
     * CRITICAL: This must be called on app initialization
     */
    suspend fun autoWhitelistKioskLauncher() {
        try {
            val packageName = context.packageName
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            
            val whitelistedApp = WhitelistedApp(
                packageName = packageName,
                appName = appName,
                isSystemApp = false,
                isAutoWhitelisted = true
            )
            whitelistedAppDao.insertWhitelistedApp(whitelistedApp)
        } catch (e: Exception) {
            // Should never happen, but log if it does
            e.printStackTrace()
        }
    }
    
    /**
     * Initialize default whitelist (kiosk launcher + phone app)
     * Should be called on first app launch
     */
    suspend fun initializeDefaultWhitelist() {
        autoWhitelistKioskLauncher()
        autoWhitelistPhoneApp()
    }
}
