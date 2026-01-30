package com.kv.kiosklauncher.data.dao

import androidx.room.*
import com.kv.kiosklauncher.data.model.KioskSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for KioskSettings entity.
 * Provides methods to manage kiosk configuration settings.
 */
@Dao
interface KioskSettingsDao {
    
    /**
     * Insert or update settings (single row table)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: KioskSettings)
    
    /**
     * Update settings
     */
    @Update
    suspend fun updateSettings(settings: KioskSettings)
    
    /**
     * Get current settings
     */
    @Query("SELECT * FROM kiosk_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): KioskSettings?
    
    /**
     * Observe settings changes
     */
    @Query("SELECT * FROM kiosk_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<KioskSettings?>
    
    /**
     * Delete all settings (reset to default)
     */
    @Query("DELETE FROM kiosk_settings")
    suspend fun deleteSettings()
}
