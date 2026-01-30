package com.kv.kiosklauncher.presentation.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.dao.WhitelistedAppDao
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.service.SessionManager
import com.kv.kiosklauncher.util.PermissionHelper
import com.kv.kiosklauncher.util.WhitelistChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TimerActivity.
 * Manages session creation and permission checks.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val whitelistChecker: WhitelistChecker,
    private val whitelistRepository: WhitelistRepository
) : AndroidViewModel(application) {
    
    private val permissionHelper = PermissionHelper(application)
    
    val isSessionActive = sessionManager.isSessionActive
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val whitelistCount = whitelistedAppDao.observeWhitelistedAppCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    
    private val _hasAllPermissions = MutableStateFlow(false)
    val hasAllPermissions: StateFlow<Boolean> = _hasAllPermissions.asStateFlow()
    
    init {
        checkPermissions()
        initializeWhitelist()
    }
    
    /**
     * Initialize whitelist with phone app on first launch
     */
    private fun initializeWhitelist() {
        viewModelScope.launch {
            // Auto-whitelist phone app for emergency calls
            whitelistRepository.autoWhitelistPhoneApp()
            
            // Refresh cache
            whitelistChecker.refreshCache()
        }
    }
    
    fun startIndefiniteSession() {
        viewModelScope.launch {
            // Refresh whitelist cache before starting
            whitelistChecker.refreshCache()
            
            // Start indefinite session
            sessionManager.startIndefiniteSession()
        }
    }
    
    fun startSession(durationMinutes: Int) {
        viewModelScope.launch {
            // Refresh whitelist cache before starting
            whitelistChecker.refreshCache()
            
            // Start session
            sessionManager.startSession(durationMinutes)
        }
    }
    
    fun stopSession() {
        viewModelScope.launch {
            sessionManager.stopSession()
        }
    }
    
    fun checkPermissions() {
        _hasAllPermissions.value = permissionHelper.hasAllPermissions()
    }
    
    fun hasAllPermissions(): Boolean {
        return permissionHelper.hasAllPermissions()
    }
}
