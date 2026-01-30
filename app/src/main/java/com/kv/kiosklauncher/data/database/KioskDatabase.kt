package com.kv.kiosklauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kv.kiosklauncher.data.dao.*
import com.kv.kiosklauncher.data.model.*

/**
 * Room database for Kiosk Launcher.
 * Contains all entities and provides DAOs for data access.
 */
@Database(
    entities = [
        Session::class,
        WhitelistedApp::class,
        KioskSettings::class,
        BlockLog::class,
        AdminCredentials::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KioskDatabase : RoomDatabase() {
    
    abstract fun sessionDao(): SessionDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao
    abstract fun kioskSettingsDao(): KioskSettingsDao
    abstract fun blockLogDao(): BlockLogDao
    abstract fun adminCredentialsDao(): AdminCredentialsDao
    
    companion object {
        const val DATABASE_NAME = "kiosk_launcher.db"
    }
}
