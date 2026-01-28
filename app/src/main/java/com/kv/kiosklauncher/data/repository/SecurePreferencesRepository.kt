package com.kv.kiosklauncher.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.kv.kiosklauncher.data.model.AdminCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for secure storage of admin credentials and sensitive data
 */
@Singleton
class SecurePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "kiosk_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val KEY_ADMIN_CREDENTIALS = "admin_credentials"
        private const val DEFAULT_USERNAME = "admin"
        private const val DEFAULT_PASSWORD = "admin123"
    }
    
    /**
     * Get admin credentials
     */
    fun getAdminCredentials(): AdminCredentials? {
        val json = encryptedPrefs.getString(KEY_ADMIN_CREDENTIALS, null)
        return json?.let { gson.fromJson(it, AdminCredentials::class.java) }
    }
    
    /**
     * Initialize with default credentials if not set
     */
    fun initializeDefaultCredentials() {
        if (getAdminCredentials() == null) {
            val salt = generateSalt()
            val passwordHash = hashPassword(DEFAULT_PASSWORD, salt)
            val credentials = AdminCredentials(
                username = DEFAULT_USERNAME,
                passwordHash = passwordHash,
                salt = salt,
                isDefaultPassword = true
            )
            saveAdminCredentials(credentials)
        }
    }
    
    /**
     * Save admin credentials
     */
    fun saveAdminCredentials(credentials: AdminCredentials) {
        val json = gson.toJson(credentials)
        encryptedPrefs.edit().putString(KEY_ADMIN_CREDENTIALS, json).apply()
    }
    
    /**
     * Verify password
     */
    fun verifyPassword(password: String): Boolean {
        val credentials = getAdminCredentials() ?: return false
        val hashedInput = hashPassword(password, credentials.salt)
        return hashedInput == credentials.passwordHash
    }
    
    /**
     * Change password
     */
    fun changePassword(newPassword: String): Boolean {
        val credentials = getAdminCredentials() ?: return false
        val newSalt = generateSalt()
        val newHash = hashPassword(newPassword, newSalt)
        
        val updatedCredentials = credentials.copy(
            passwordHash = newHash,
            salt = newSalt,
            isDefaultPassword = false,
            lastPasswordChange = System.currentTimeMillis()
        )
        saveAdminCredentials(updatedCredentials)
        return true
    }
    
    /**
     * Set emergency exit code
     */
    fun setEmergencyExitCode(code: String) {
        val credentials = getAdminCredentials() ?: return
        val salt = generateSalt()
        val hashedCode = hashPassword(code, salt)
        
        // Store emergency code with its own salt (concatenated)
        val updatedCredentials = credentials.copy(
            emergencyExitCode = "$salt:$hashedCode"
        )
        saveAdminCredentials(updatedCredentials)
    }
    
    /**
     * Verify emergency exit code
     */
    fun verifyEmergencyExitCode(code: String): Boolean {
        val credentials = getAdminCredentials() ?: return false
        val emergencyData = credentials.emergencyExitCode ?: return false
        
        val parts = emergencyData.split(":")
        if (parts.size != 2) return false
        
        val salt = parts[0]
        val storedHash = parts[1]
        val inputHash = hashPassword(code, salt)
        
        return inputHash == storedHash
    }
    
    /**
     * Check if emergency code is set
     */
    fun hasEmergencyCode(): Boolean {
        val credentials = getAdminCredentials() ?: return false
        return !credentials.emergencyExitCode.isNullOrBlank()
    }
    
    /**
     * Set emergency code (wrapper for setEmergencyExitCode)
     */
    fun setEmergencyCode(code: String): Boolean {
        return try {
            setEmergencyExitCode(code)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verify emergency code (wrapper for verifyEmergencyExitCode)
     */
    fun verifyEmergencyCode(code: String): Boolean {
        return verifyEmergencyExitCode(code)
    }
    
    /**
     * Clear emergency code
     */
    fun clearEmergencyCode() {
        val credentials = getAdminCredentials() ?: return
        val updatedCredentials = credentials.copy(emergencyExitCode = null)
        saveAdminCredentials(updatedCredentials)
    }
    
    /**
     * Check if using default password
     */
    fun isUsingDefaultPassword(): Boolean {
        return getAdminCredentials()?.isDefaultPassword ?: true
    }
    
    /**
     * Hash password with salt using SHA-256
     */
    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = "$salt$password"
        val hash = digest.digest(saltedPassword.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
    
    /**
     * Generate random salt
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }
    
    /**
     * Clear all secure data (factory reset)
     */
    fun clearAllData() {
        encryptedPrefs.edit().clear().apply()
    }
}
