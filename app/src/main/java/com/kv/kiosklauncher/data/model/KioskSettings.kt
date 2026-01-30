package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Blocking mode determines what happens when user tries to access non-whitelisted app
 */
enum class BlockingMode {
    /** Redirect user back to launcher home screen */
    REDIRECT,
    
    /** Turn off the screen using Device Admin */
    SCREEN_OFF,
    
    /** Turn off screen first, then redirect to launcher */
    BOTH
}

/**
 * Configurable settings for kiosk behavior.
 * These settings control how the kiosk mode operates.
 */
@Entity(tableName = "kiosk_settings")
data class KioskSettings(
    @PrimaryKey
    val id: Long = 1, // Single row table
    
    /** How to block unauthorized app access */
    val blockingMode: BlockingMode = BlockingMode.BOTH,
    
    /** Whether to enable screen-off functionality (requires Device Admin) */
    val enableScreenOff: Boolean = true,
    
    /** Automatically whitelist the phone/dialer app */
    val autoWhitelistDialer: Boolean = true,
    
    /** How frequently to check for foreground app changes (milliseconds) */
    val monitoringIntervalMs: Long = 50,
    
    /** Show persistent notification during active session */
    val showNotificationDuringSession: Boolean = true,
    
    /** Vibrate when blocking an app */
    val vibrateOnBlock: Boolean = true,
    
    /** Show overlay message when blocking */
    val showBlockOverlay: Boolean = true,
    
    /** Delay before redirecting after screen-off (milliseconds) */
    val screenOffRedirectDelayMs: Long = 500,
    
    /** Last modified timestamp */
    val lastModified: Long = System.currentTimeMillis()
)
