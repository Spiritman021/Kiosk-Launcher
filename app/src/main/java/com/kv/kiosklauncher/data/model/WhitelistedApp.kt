package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an app that is allowed to be accessed during kiosk mode.
 * Only whitelisted apps can be opened when a session is active.
 */
@Entity(tableName = "whitelisted_apps")
data class WhitelistedApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Package name of the whitelisted app (e.g., "com.android.dialer") */
    val packageName: String,
    
    /** Display name of the app */
    val appName: String,
    
    /** Whether this is a system app (phone, settings, etc.) */
    val isSystemApp: Boolean = false,
    
    /** Whether this app was auto-whitelisted (e.g., phone app) */
    val isAutoWhitelisted: Boolean = false,
    
    /** Timestamp when this app was added to whitelist */
    val addedAt: Long = System.currentTimeMillis()
)
