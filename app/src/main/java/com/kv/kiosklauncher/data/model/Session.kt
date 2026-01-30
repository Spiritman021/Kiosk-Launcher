package com.kv.kiosklauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a kiosk session with timer information.
 * A session is active when the user starts the timer and restricts app access.
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Timestamp when the session was started (milliseconds since epoch) */
    val startTime: Long,
    
    /** Duration of the session in minutes */
    val durationMinutes: Int,
    
    /** Calculated end time (startTime + duration) */
    val endTime: Long,
    
    /** Whether this session is currently active */
    val isActive: Boolean,
    
    /** Timestamp when this session record was created */
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate remaining time in milliseconds
     */
    fun getRemainingTimeMs(): Long {
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
        return System.currentTimeMillis() >= endTime
    }
    
    /**
     * Get progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        val totalDuration = endTime - startTime
        val elapsed = System.currentTimeMillis() - startTime
        return ((elapsed.toFloat() / totalDuration.toFloat()) * 100).toInt().coerceIn(0, 100)
    }
}
