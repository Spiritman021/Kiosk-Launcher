package com.kv.kiosklauncher.data.dao

import androidx.room.*
import com.kv.kiosklauncher.data.model.BlockLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BlockLog entity.
 * Provides methods to log and query blocked app access attempts.
 */
@Dao
interface BlockLogDao {
    
    /**
     * Insert a new block log entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockLog(log: BlockLog): Long
    
    /**
     * Get all block logs ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC")
    fun getAllBlockLogs(): Flow<List<BlockLog>>
    
    /**
     * Get block logs for a specific session
     */
    @Query("SELECT * FROM block_logs WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBlockLogsForSession(sessionId: Long): Flow<List<BlockLog>>
    
    /**
     * Get recent block logs (last N entries)
     */
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBlockLogs(limit: Int = 50): List<BlockLog>
    
    /**
     * Get count of blocks for a specific app
     */
    @Query("SELECT COUNT(*) FROM block_logs WHERE packageName = :packageName")
    suspend fun getBlockCountForApp(packageName: String): Int
    
    /**
     * Delete all block logs
     */
    @Query("DELETE FROM block_logs")
    suspend fun deleteAllBlockLogs()
    
    /**
     * Delete block logs older than timestamp
     */
    @Query("DELETE FROM block_logs WHERE timestamp < :timestamp")
    suspend fun deleteBlockLogsOlderThan(timestamp: Long)
}
