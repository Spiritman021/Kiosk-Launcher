package com.kv.kiosklauncher.data.dao

import androidx.room.*
import com.kv.kiosklauncher.data.model.AdminCredentials
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AdminCredentials entity.
 * Provides methods to manage admin authentication.
 */
@Dao
interface AdminCredentialsDao {
    
    /**
     * Insert or update admin credentials (single row table)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: AdminCredentials)
    
    /**
     * Update credentials
     */
    @Update
    suspend fun updateCredentials(credentials: AdminCredentials)
    
    /**
     * Get current admin credentials
     */
    @Query("SELECT * FROM admin_credentials WHERE id = 1 LIMIT 1")
    suspend fun getCredentials(): AdminCredentials?
    
    /**
     * Observe credentials changes
     */
    @Query("SELECT * FROM admin_credentials WHERE id = 1 LIMIT 1")
    fun observeCredentials(): Flow<AdminCredentials?>
    
    /**
     * Check if admin authentication is set up
     */
    @Query("SELECT EXISTS(SELECT 1 FROM admin_credentials WHERE id = 1 AND isEnabled = 1 LIMIT 1)")
    suspend fun isAdminAuthEnabled(): Boolean
    
    /**
     * Delete credentials (reset admin)
     */
    @Query("DELETE FROM admin_credentials")
    suspend fun deleteCredentials()
}
