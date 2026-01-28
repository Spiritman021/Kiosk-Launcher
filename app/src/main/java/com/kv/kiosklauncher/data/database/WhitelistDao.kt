package com.kv.kiosklauncher.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kv.kiosklauncher.data.model.WhitelistEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for whitelist operations
 */
@Dao
interface WhitelistDao {
    
    @Query("SELECT * FROM whitelist WHERE isEnabled = 1 ORDER BY appName ASC")
    fun getAllWhitelistedApps(): Flow<List<WhitelistEntry>>
    
    @Query("SELECT * FROM whitelist ORDER BY appName ASC")
    fun getAllApps(): Flow<List<WhitelistEntry>>
    
    @Query("SELECT * FROM whitelist WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): WhitelistEntry?
    
    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE packageName = :packageName AND isEnabled = 1)")
    suspend fun isAppWhitelisted(packageName: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: WhitelistEntry)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<WhitelistEntry>)
    
    @Update
    suspend fun updateApp(app: WhitelistEntry)
    
    @Delete
    suspend fun deleteApp(app: WhitelistEntry)
    
    @Query("DELETE FROM whitelist WHERE packageName = :packageName")
    suspend fun deleteAppByPackage(packageName: String)
    
    @Query("DELETE FROM whitelist")
    suspend fun clearWhitelist()
    
    @Query("SELECT COUNT(*) FROM whitelist WHERE isEnabled = 1")
    suspend fun getWhitelistCount(): Int
}
