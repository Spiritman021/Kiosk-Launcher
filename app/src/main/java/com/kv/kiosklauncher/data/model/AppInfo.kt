package com.kv.kiosklauncher.data.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed application with its metadata
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val versionName: String,
    val lastUpdateTime: Long
)
