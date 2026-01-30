package com.kv.kiosklauncher.data.database

import androidx.room.TypeConverter
import com.kv.kiosklauncher.data.model.BlockingMode

/**
 * Type converters for Room database.
 * Converts custom types to/from database-compatible types.
 */
class Converters {
    
    /**
     * Convert BlockingMode enum to String for database storage
     */
    @TypeConverter
    fun fromBlockingMode(mode: BlockingMode): String {
        return mode.name
    }
    
    /**
     * Convert String from database to BlockingMode enum
     */
    @TypeConverter
    fun toBlockingMode(value: String): BlockingMode {
        return try {
            BlockingMode.valueOf(value)
        } catch (e: IllegalArgumentException) {
            BlockingMode.BOTH // Default fallback
        }
    }
}
