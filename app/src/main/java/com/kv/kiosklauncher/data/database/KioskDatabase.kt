package com.kv.kiosklauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kv.kiosklauncher.data.model.WhitelistEntry

/**
 * Room database for kiosk launcher
 */
@Database(
    entities = [WhitelistEntry::class],
    version = 1,
    exportSchema = true
)
abstract class KioskDatabase : RoomDatabase() {
    abstract fun whitelistDao(): WhitelistDao
    
    companion object {
        const val DATABASE_NAME = "kiosk_launcher.db"
    }
}
