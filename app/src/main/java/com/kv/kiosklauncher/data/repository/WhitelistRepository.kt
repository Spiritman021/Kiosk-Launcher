package com.kv.kiosklauncher.data.repository

import com.kv.kiosklauncher.data.database.WhitelistDao
import com.kv.kiosklauncher.data.model.WhitelistEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing whitelisted applications
 */
@Singleton
class WhitelistRepository @Inject constructor(
    private val whitelistDao: WhitelistDao
) {
    
    /**
     * Get all whitelisted apps as Flow for reactive updates
     */
    fun getWhitelistedApps(): Flow<List<WhitelistEntry>> {
        return whitelistDao.getAllWhitelistedApps()
    }
    
    /**
     * Get all apps (including disabled) as Flow
     */
    fun getAllApps(): Flow<List<WhitelistEntry>> {
        return whitelistDao.getAllApps()
    }
    
    /**
     * Check if an app is whitelisted
     */
    suspend fun isAppWhitelisted(packageName: String): Boolean {
        return whitelistDao.isAppWhitelisted(packageName)
    }
    
    /**
     * Add an app to whitelist
     */
    suspend fun addToWhitelist(packageName: String, appName: String) {
        val entry = WhitelistEntry(
            packageName = packageName,
            appName = appName,
            addedAt = System.currentTimeMillis(),
            isEnabled = true
        )
        whitelistDao.insertApp(entry)
    }
    
    /**
     * Add multiple apps to whitelist
     */
    suspend fun addMultipleToWhitelist(apps: List<Pair<String, String>>) {
        val entries = apps.map { (packageName, appName) ->
            WhitelistEntry(
                packageName = packageName,
                appName = appName,
                addedAt = System.currentTimeMillis(),
                isEnabled = true
            )
        }
        whitelistDao.insertApps(entries)
    }
    
    /**
     * Remove an app from whitelist
     */
    suspend fun removeFromWhitelist(packageName: String) {
        whitelistDao.deleteAppByPackage(packageName)
    }
    
    /**
     * Toggle app whitelist status
     */
    suspend fun toggleAppStatus(packageName: String) {
        val app = whitelistDao.getApp(packageName)
        app?.let {
            whitelistDao.updateApp(it.copy(isEnabled = !it.isEnabled))
        }
    }
    
    /**
     * Clear entire whitelist
     */
    suspend fun clearWhitelist() {
        whitelistDao.clearWhitelist()
    }
    
    /**
     * Get count of whitelisted apps
     */
    suspend fun getWhitelistCount(): Int {
        return whitelistDao.getWhitelistCount()
    }
    
    /**
     * Import whitelist from list
     */
    suspend fun importWhitelist(entries: List<WhitelistEntry>) {
        whitelistDao.insertApps(entries)
    }
}
