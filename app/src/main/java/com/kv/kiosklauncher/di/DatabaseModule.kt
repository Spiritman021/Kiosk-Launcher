package com.kv.kiosklauncher.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.kv.kiosklauncher.data.database.KioskDatabase
import com.kv.kiosklauncher.data.database.WhitelistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and data layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideKioskDatabase(
        @ApplicationContext context: Context
    ): KioskDatabase {
        return Room.databaseBuilder(
            context,
            KioskDatabase::class.java,
            KioskDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideWhitelistDao(database: KioskDatabase): WhitelistDao {
        return database.whitelistDao()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
