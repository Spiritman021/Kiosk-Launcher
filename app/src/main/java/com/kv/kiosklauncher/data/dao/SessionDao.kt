package com.kv.kiosklauncher.data.dao

import androidx.room.*
import com.kv.kiosklauncher.data.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Session entity.
 * Provides methods to interact with session data in the database.
 */
@Dao
interface SessionDao {
    
    /**
     * Insert a new session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session): Long
    
    /**
     * Update an existing session
     */
    @Update
    suspend fun updateSession(session: Session)
    
    /**
     * Delete a session
     */
    @Delete
    suspend fun deleteSession(session: Session)
    
    /**
     * Get the currently active session (if any)
     */
    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): Session?
    
    /**
     * Observe the currently active session
     */
    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    fun observeActiveSession(): Flow<Session?>
    
    /**
     * Get all sessions ordered by creation time (newest first)
     */
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<Session>>
    
    /**
     * Get session by ID
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): Session?
    
    /**
     * Deactivate all sessions (useful when starting a new session)
     */
    @Query("UPDATE sessions SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllSessions()
    
    /**
     * Delete all sessions
     */
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()
    
    /**
     * Get session history (last N sessions)
     */
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getSessionHistory(limit: Int = 10): List<Session>
}
