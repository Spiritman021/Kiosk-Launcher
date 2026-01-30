package com.kv.kiosklauncher.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.service.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AdminSettingsActivity.
 * Manages session state and admin operations.
 */
@HiltViewModel
class AdminSettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    val isSessionActive: StateFlow<Boolean> = sessionManager.isSessionActive
    
    fun stopSession() {
        viewModelScope.launch {
            sessionManager.stopSession()
        }
    }
}
