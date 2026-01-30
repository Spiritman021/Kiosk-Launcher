package com.kv.kiosklauncher.di

import android.content.Context
import androidx.room.Room
import com.kv.kiosklauncher.data.dao.*
import com.kv.kiosklauncher.data.database.KioskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the Room database instance
     */
    @Provides
    @Singleton
    fun provideKioskDatabase(
        @ApplicationContext context: Context
    ): KioskDatabase {
        return Room.databaseBuilder(
            context,
            KioskDatabase::class.java,
            "kiosk_launcher.db"
        )
            .fallbackToDestructiveMigration() // Allow destructive migration for development
            .build()
    }
    
    /**
     * Provides SessionDao
     */
    @Provides
    @Singleton
    fun provideSessionDao(database: KioskDatabase): SessionDao {
        return database.sessionDao()
    }
    
    /**
     * Provides WhitelistedAppDao
     */
    @Provides
    @Singleton
    fun provideWhitelistedAppDao(database: KioskDatabase): WhitelistedAppDao {
        return database.whitelistedAppDao()
    }
    
    /**
     * Provides KioskSettingsDao
     */
    @Provides
    @Singleton
    fun provideKioskSettingsDao(database: KioskDatabase): KioskSettingsDao {
        return database.kioskSettingsDao()
    }
    
    /**
     * Provides BlockLogDao
     */
    @Provides
    @Singleton
    fun provideBlockLogDao(database: KioskDatabase): BlockLogDao {
        return database.blockLogDao()
    }
    
    /**
     * Provides AdminCredentialsDao
     */
    @Provides
    @Singleton
    fun provideAdminCredentialsDao(database: KioskDatabase): AdminCredentialsDao {
        return database.adminCredentialsDao()
    }
}
