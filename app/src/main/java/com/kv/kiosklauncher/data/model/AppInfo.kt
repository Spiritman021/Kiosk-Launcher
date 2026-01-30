package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an installed app on the device.
 * Used for displaying all apps in whitelist management screen.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isWhitelisted: Boolean = false,
    val versionName: String? = null,
    val versionCode: Long = 0
)

/**
 * Log entry for blocked app access attempts.
 * Useful for analytics and debugging.
 */
@Entity(tableName = "block_logs")
data class BlockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Package name of the blocked app */
    val packageName: String,
    
    /** Display name of the blocked app */
    val appName: String,
    
    /** Session ID when this block occurred */
    val sessionId: Long,
    
    /** Timestamp when the block occurred */
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Action taken (REDIRECT, SCREEN_OFF, BOTH) */
    val actionTaken: String
)
