package com.kv.kiosklauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kv.kiosklauncher.data.model.KioskConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiosk_config")

/**
 * Repository for managing kiosk configuration settings
 */
@Singleton
class ConfigurationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object PreferencesKeys {
        val KIOSK_MODE_ENABLED = booleanPreferencesKey("kiosk_mode_enabled")
        val USE_LOCK_TASK_MODE = booleanPreferencesKey("use_lock_task_mode")
        val EMERGENCY_EXIT_ENABLED = booleanPreferencesKey("emergency_exit_enabled")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SHOW_APP_NAMES = booleanPreferencesKey("show_app_names")
        val ICON_SIZE = intPreferencesKey("icon_size")
        val LAST_CONFIG_UPDATE = longPreferencesKey("last_config_update")
    }
    
    /**
     * Get configuration as Flow
     */
    val configuration: Flow<KioskConfiguration> = context.configDataStore.data.map { preferences ->
        KioskConfiguration(
            isKioskModeEnabled = preferences[PreferencesKeys.KIOSK_MODE_ENABLED] ?: false,
            useLockTaskMode = preferences[PreferencesKeys.USE_LOCK_TASK_MODE] ?: false,
            emergencyExitEnabled = preferences[PreferencesKeys.EMERGENCY_EXIT_ENABLED] ?: true,
            gridColumns = preferences[PreferencesKeys.GRID_COLUMNS] ?: 4,
            showAppNames = preferences[PreferencesKeys.SHOW_APP_NAMES] ?: true,
            iconSize = preferences[PreferencesKeys.ICON_SIZE] ?: 128,
            lastConfigUpdate = preferences[PreferencesKeys.LAST_CONFIG_UPDATE] ?: System.currentTimeMillis()
        )
    }
    
    /**
     * Update kiosk mode enabled status
     */
    suspend fun setKioskModeEnabled(enabled: Boolean) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.KIOSK_MODE_ENABLED] = enabled
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update Lock Task Mode preference
     */
    suspend fun setUseLockTaskMode(use: Boolean) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_LOCK_TASK_MODE] = use
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update emergency exit enabled status
     */
    suspend fun setEmergencyExitEnabled(enabled: Boolean) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.EMERGENCY_EXIT_ENABLED] = enabled
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update grid columns
     */
    suspend fun setGridColumns(columns: Int) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update show app names preference
     */
    suspend fun setShowAppNames(show: Boolean) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_APP_NAMES] = show
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update icon size
     */
    suspend fun setIconSize(size: Int) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.ICON_SIZE] = size
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
    
    /**
     * Update entire configuration
     */
    suspend fun updateConfiguration(config: KioskConfiguration) {
        context.configDataStore.edit { preferences ->
            preferences[PreferencesKeys.KIOSK_MODE_ENABLED] = config.isKioskModeEnabled
            preferences[PreferencesKeys.USE_LOCK_TASK_MODE] = config.useLockTaskMode
            preferences[PreferencesKeys.EMERGENCY_EXIT_ENABLED] = config.emergencyExitEnabled
            preferences[PreferencesKeys.GRID_COLUMNS] = config.gridColumns
            preferences[PreferencesKeys.SHOW_APP_NAMES] = config.showAppNames
            preferences[PreferencesKeys.ICON_SIZE] = config.iconSize
            preferences[PreferencesKeys.LAST_CONFIG_UPDATE] = System.currentTimeMillis()
        }
    }
}
