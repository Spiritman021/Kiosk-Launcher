package com.kv.kiosklauncher.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.repository.SecurePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for emergency exit functionality
 */
@HiltViewModel
class EmergencyExitViewModel @Inject constructor(
    private val securePrefsRepository: SecurePreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EmergencyExitUiState>(EmergencyExitUiState.Idle)
    val uiState: StateFlow<EmergencyExitUiState> = _uiState.asStateFlow()
    
    private val _hasEmergencyCode = MutableStateFlow(false)
    val hasEmergencyCode: StateFlow<Boolean> = _hasEmergencyCode.asStateFlow()
    
    init {
        checkEmergencyCode()
    }
    
    /**
     * Check if emergency code is set
     */
    private fun checkEmergencyCode() {
        viewModelScope.launch {
            _hasEmergencyCode.value = securePrefsRepository.hasEmergencyCode()
        }
    }
    
    /**
     * Set emergency exit code
     */
    fun setEmergencyCode(code: String, confirmCode: String) {
        viewModelScope.launch {
            _uiState.value = EmergencyExitUiState.Loading
            
            // Validate inputs
            if (code.isBlank() || confirmCode.isBlank()) {
                _uiState.value = EmergencyExitUiState.Error("All fields are required")
                return@launch
            }
            
            if (code != confirmCode) {
                _uiState.value = EmergencyExitUiState.Error("Codes do not match")
                return@launch
            }
            
            if (code.length < 4) {
                _uiState.value = EmergencyExitUiState.Error("Code must be at least 4 characters")
                return@launch
            }
            
            // Set emergency code
            val success = securePrefsRepository.setEmergencyCode(code)
            
            if (success) {
                _hasEmergencyCode.value = true
                _uiState.value = EmergencyExitUiState.Success("Emergency code set successfully")
            } else {
                _uiState.value = EmergencyExitUiState.Error("Failed to set emergency code")
            }
        }
    }
    
    /**
     * Verify emergency exit code
     */
    fun verifyEmergencyCode(code: String): Boolean {
        return securePrefsRepository.verifyEmergencyCode(code)
    }
    
    /**
     * Clear emergency code
     */
    fun clearEmergencyCode() {
        viewModelScope.launch {
            securePrefsRepository.clearEmergencyCode()
            _hasEmergencyCode.value = false
            _uiState.value = EmergencyExitUiState.Success("Emergency code cleared")
        }
    }
    
    /**
     * Reset state
     */
    fun resetState() {
        _uiState.value = EmergencyExitUiState.Idle
    }
}

/**
 * UI state for emergency exit
 */
sealed class EmergencyExitUiState {
    object Idle : EmergencyExitUiState()
    object Loading : EmergencyExitUiState()
    data class Success(val message: String) : EmergencyExitUiState()
    data class Error(val message: String) : EmergencyExitUiState()
}
