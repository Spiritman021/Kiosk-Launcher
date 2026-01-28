package com.kv.kiosklauncher.util

/**
 * Constants used throughout the application
 */
object Constants {
    
    // Default credentials
    const val DEFAULT_USERNAME = "admin"
    const val DEFAULT_PASSWORD = "admin123"
    
    // Emergency exit
    const val EMERGENCY_EXIT_FINGER_COUNT = 5
    const val EMERGENCY_EXIT_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    
    // Password requirements
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PASSWORD_LENGTH = 32
    
    // Grid layout
    const val MIN_GRID_COLUMNS = 3
    const val MAX_GRID_COLUMNS = 6
    const val DEFAULT_GRID_COLUMNS = 4
    
    // Icon sizes (dp)
    const val MIN_ICON_SIZE = 64
    const val MAX_ICON_SIZE = 192
    const val DEFAULT_ICON_SIZE = 128
    
    // Authentication
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOGIN_LOCKOUT_DURATION_MS = 30 * 1000L // 30 seconds
    
    // Kiosk mode
    const val KIOSK_SERVICE_NOTIFICATION_ID = 1001
    const val KIOSK_SERVICE_CHANNEL_ID = "kiosk_service_channel"
    
    // Shared preferences
    const val PREFS_SECURE = "kiosk_secure_prefs"
    const val PREFS_CONFIG = "kiosk_config"
    
    // Intent extras
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_FROM_EMERGENCY_EXIT = "extra_from_emergency_exit"
    
    // Request codes
    const val REQUEST_CODE_ADMIN_LOGIN = 1001
    const val REQUEST_CODE_USAGE_STATS = 1002
    const val REQUEST_CODE_OVERLAY = 1003
    const val REQUEST_CODE_DEVICE_ADMIN = 1004
}
