package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a kiosk session.
 * Can be time-limited or indefinite (runs until manually stopped).
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Timestamp when the session was started (milliseconds since epoch) */
    val startTime: Long,
    
    /** Duration of the session in minutes (0 for indefinite) */
    val durationMinutes: Int = 0,
    
    /** Calculated end time (startTime + duration, or Long.MAX_VALUE for indefinite) */
    val endTime: Long,
    
    /** Whether this session is currently active */
    val isActive: Boolean,
    
    /** Whether this is an indefinite session (no time limit) */
    val isIndefinite: Boolean = true,
    
    /** Timestamp when this session record was created */
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate remaining time in milliseconds
     */
    fun getRemainingTimeMs(): Long {
        if (isIndefinite) return Long.MAX_VALUE
        
        val now = System.currentTimeMillis()
        return if (isActive && now < endTime) {
            endTime - now
        } else {
            0L
        }
    }
    
    /**
     * Check if session has expired
     */
    fun isExpired(): Boolean {
        if (isIndefinite) return false
        return System.currentTimeMillis() >= endTime
    }
    
    /**
     * Get progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        if (isIndefinite) return 0
        
        val totalDuration = endTime - startTime
        val elapsed = System.currentTimeMillis() - startTime
        return ((elapsed.toFloat() / totalDuration.toFloat()) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Get elapsed time in milliseconds
     */
    fun getElapsedTimeMs(): Long {
        return System.currentTimeMillis() - startTime
    }
}
