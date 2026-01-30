package com.kv.kiosklauncher.service

import com.kv.kiosklauncher.data.dao.SessionDao
import com.kv.kiosklauncher.data.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton service that manages kiosk sessions.
 * Handles timer-based session lifecycle and state management.
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionDao: SessionDao
) {
    
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()
    
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()
    
    /**
     * Start a new kiosk session with the specified duration
     */
    suspend fun startSession(durationMinutes: Int): Session {
        // Deactivate any existing sessions
        sessionDao.deactivateAllSessions()
        
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes * 60 * 1000L)
        
        val session = Session(
            startTime = now,
            durationMinutes = durationMinutes,
            endTime = endTime,
            isActive = true,
            createdAt = now
        )
        
        val sessionId = sessionDao.insertSession(session)
        val createdSession = session.copy(id = sessionId)
        
        _currentSession.value = createdSession
        _isSessionActive.value = true
        
        return createdSession
    }
    
    /**
     * Stop the current active session
     */
    suspend fun stopSession() {
        _currentSession.value?.let { session ->
            val updatedSession = session.copy(isActive = false)
            sessionDao.updateSession(updatedSession)
            _currentSession.value = null
            _isSessionActive.value = false
        }
    }
    
    /**
     * Load the active session from database (e.g., after app restart)
     */
    suspend fun loadActiveSession() {
        val session = sessionDao.getActiveSession()
        
        if (session != null) {
            // Check if session has expired
            if (session.isExpired()) {
                // Auto-stop expired session
                val updatedSession = session.copy(isActive = false)
                sessionDao.updateSession(updatedSession)
                _currentSession.value = null
                _isSessionActive.value = false
            } else {
                _currentSession.value = session
                _isSessionActive.value = true
            }
        } else {
            _currentSession.value = null
            _isSessionActive.value = false
        }
    }
    
    /**
     * Get remaining time in milliseconds for current session
     */
    fun getRemainingTimeMs(): Long {
        return _currentSession.value?.getRemainingTimeMs() ?: 0L
    }
    
    /**
     * Check if current session has expired
     */
    fun isSessionExpired(): Boolean {
        return _currentSession.value?.isExpired() ?: true
    }
    
    /**
     * Observe active session from database
     */
    fun observeActiveSession(): Flow<Session?> {
        return sessionDao.observeActiveSession()
    }
    
    /**
     * Get session history
     */
    suspend fun getSessionHistory(limit: Int = 10): List<Session> {
        return sessionDao.getSessionHistory(limit)
    }
}
