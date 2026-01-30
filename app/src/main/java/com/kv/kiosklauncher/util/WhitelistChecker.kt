package com.kv.kiosklauncher.util

import com.kv.kiosklauncher.data.dao.WhitelistedAppDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fast whitelist checker with in-memory caching for O(1) lookup performance.
 * Critical for high-frequency app monitoring (50-100ms polling).
 */
@Singleton
class WhitelistChecker @Inject constructor(
    private val whitelistedAppDao: WhitelistedAppDao
) {
    
    // In-memory cache of whitelisted package names for fast lookup
    private val _whitelistedPackages = MutableStateFlow<Set<String>>(emptySet())
    val whitelistedPackages: StateFlow<Set<String>> = _whitelistedPackages.asStateFlow()
    
    // Always allowed packages (system critical apps)
    private val alwaysAllowedPackages = setOf(
        "com.android.systemui",           // System UI
        "com.kv.kiosklauncher",           // This app itself
        "android",                         // Android system
        "com.android.settings"             // Settings (for emergency access)
    )
    
    /**
     * Refresh the whitelist cache from database.
     * Should be called when whitelist changes.
     */
    suspend fun refreshCache() {
        val packageNames = whitelistedAppDao.getAllWhitelistedPackageNames()
        _whitelistedPackages.value = packageNames.toSet()
    }
    
    /**
     * Check if a package is whitelisted.
     * O(1) lookup using in-memory set.
     */
    fun isWhitelisted(packageName: String): Boolean {
        // Always allow critical system packages
        if (alwaysAllowedPackages.contains(packageName)) {
            return true
        }
        
        // Check in-memory cache
        return _whitelistedPackages.value.contains(packageName)
    }
    
    /**
     * Check if a package is a phone/dialer app.
     * These should always be accessible for emergency calls.
     */
    fun isPhoneApp(packageName: String): Boolean {
        return packageName in setOf(
            "com.android.dialer",
            "com.android.phone",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui"
        )
    }
    
    /**
     * Check if an app should be blocked.
     * Returns true if the app is NOT whitelisted and NOT a critical system app.
     */
    fun shouldBlock(packageName: String): Boolean {
        // Never block phone apps (emergency calls)
        if (isPhoneApp(packageName)) {
            return false
        }
        
        // Block if not whitelisted
        return !isWhitelisted(packageName)
    }
    
    /**
     * Get count of whitelisted apps
     */
    fun getWhitelistCount(): Int {
        return _whitelistedPackages.value.size
    }
}
