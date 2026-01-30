package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Admin credentials for accessing kiosk settings.
 * Stores hashed PIN/password for security.
 */
@Entity(tableName = "admin_credentials")
data class AdminCredentials(
    @PrimaryKey
    val id: Long = 1, // Single row table
    
    /** Hashed PIN/password (never store plain text) */
    val hashedPassword: String,
    
    /** Salt used for hashing */
    val salt: String,
    
    /** Whether admin authentication is enabled */
    val isEnabled: Boolean = true,
    
    /** Last modified timestamp */
    val lastModified: Long = System.currentTimeMillis()
)
