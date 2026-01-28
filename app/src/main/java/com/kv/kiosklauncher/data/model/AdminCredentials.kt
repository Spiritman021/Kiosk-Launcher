package com.kv.kiosklauncher.data.model

/**
 * Represents admin credentials with security metadata
 */
data class AdminCredentials(
    val username: String = "admin",
    val passwordHash: String,
    val salt: String,
    val isDefaultPassword: Boolean = true,
    val lastPasswordChange: Long = System.currentTimeMillis(),
    val emergencyExitCode: String? = null
)
