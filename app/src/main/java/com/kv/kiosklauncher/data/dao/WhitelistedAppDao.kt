package com.kv.kiosklauncher.data.dao

import androidx.room.*
import com.kv.kiosklauncher.data.model.WhitelistedApp
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WhitelistedApp entity.
 * Provides methods to manage whitelisted apps.
 */
@Dao
interface WhitelistedAppDao {
    
    /**
     * Insert a new whitelisted app
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelistedApp(app: WhitelistedApp): Long
    
    /**
     * Insert multiple whitelisted apps
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelistedApps(apps: List<WhitelistedApp>)
    
    /**
     * Update a whitelisted app
     */
    @Update
    suspend fun updateWhitelistedApp(app: WhitelistedApp)
    
    /**
     * Delete a whitelisted app
     */
    @Delete
    suspend fun deleteWhitelistedApp(app: WhitelistedApp)
    
    /**
     * Delete whitelisted app by package name
     */
    @Query("DELETE FROM whitelisted_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
    
    /**
     * Get all whitelisted apps
     */
    @Query("SELECT * FROM whitelisted_apps ORDER BY appName ASC")
    fun getAllWhitelistedApps(): Flow<List<WhitelistedApp>>
    
    /**
     * Get all whitelisted apps as list (non-reactive)
     */
    @Query("SELECT * FROM whitelisted_apps ORDER BY appName ASC")
    suspend fun getAllWhitelistedAppsList(): List<WhitelistedApp>
    
    /**
     * Get all whitelisted package names (for fast lookup)
     */
    @Query("SELECT packageName FROM whitelisted_apps")
    suspend fun getAllWhitelistedPackageNames(): List<String>
    
    /**
     * Observe all whitelisted package names
     */
    @Query("SELECT packageName FROM whitelisted_apps")
    fun observeWhitelistedPackageNames(): Flow<List<String>>
    
    /**
     * Check if an app is whitelisted by package name
     */
    @Query("SELECT EXISTS(SELECT 1 FROM whitelisted_apps WHERE packageName = :packageName LIMIT 1)")
    suspend fun isAppWhitelisted(packageName: String): Boolean
    
    /**
     * Get whitelisted app by package name
     */
    @Query("SELECT * FROM whitelisted_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getWhitelistedAppByPackage(packageName: String): WhitelistedApp?
    
    /**
     * Get count of whitelisted apps
     */
    @Query("SELECT COUNT(*) FROM whitelisted_apps")
    suspend fun getWhitelistedAppCount(): Int
    
    /**
     * Observe count of whitelisted apps
     */
    @Query("SELECT COUNT(*) FROM whitelisted_apps")
    fun observeWhitelistedAppCount(): Flow<Int>
    
    /**
     * Delete all whitelisted apps
     */
    @Query("DELETE FROM whitelisted_apps")
    suspend fun deleteAllWhitelistedApps()
    
    /**
     * Delete all non-auto-whitelisted apps (keep phone app, etc.)
     */
    @Query("DELETE FROM whitelisted_apps WHERE isAutoWhitelisted = 0")
    suspend fun deleteNonAutoWhitelistedApps()
}
