package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity for whitelisted applications
 */
@Entity(tableName = "whitelist")
data class WhitelistEntry(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)
