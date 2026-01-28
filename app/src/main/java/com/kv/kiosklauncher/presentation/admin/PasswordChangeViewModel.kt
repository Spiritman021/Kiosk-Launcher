package com.kv.kiosklauncher.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.SecurePreferencesRepository
import com.kv.kiosklauncher.util.LockTaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for password change functionality
 */
@HiltViewModel
class PasswordChangeViewModel @Inject constructor(
    private val securePrefsRepository: SecurePreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PasswordChangeUiState>(PasswordChangeUiState.Idle)
    val uiState: StateFlow<PasswordChangeUiState> = _uiState.asStateFlow()
    
    private val _isDefaultPassword = MutableStateFlow(true)
    val isDefaultPassword: StateFlow<Boolean> = _isDefaultPassword.asStateFlow()
    
    init {
        checkDefaultPassword()
    }
    
    /**
     * Check if using default password
     */
    private fun checkDefaultPassword() {
        viewModelScope.launch {
            _isDefaultPassword.value = securePrefsRepository.isUsingDefaultPassword()
        }
    }
    
    /**
     * Change password
     */
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = PasswordChangeUiState.Loading
            
            // Validate inputs
            if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                _uiState.value = PasswordChangeUiState.Error("All fields are required")
                return@launch
            }
            
            if (newPassword != confirmPassword) {
                _uiState.value = PasswordChangeUiState.Error("New passwords do not match")
                return@launch
            }
            
            if (newPassword.length < 6) {
                _uiState.value = PasswordChangeUiState.Error("Password must be at least 6 characters")
                return@launch
            }
            
            // Verify current password
            val isCurrentValid = securePrefsRepository.verifyPassword(currentPassword)
            if (!isCurrentValid) {
                _uiState.value = PasswordChangeUiState.Error("Current password is incorrect")
                return@launch
            }
            
            // Change password
            val success = securePrefsRepository.changePassword(newPassword)
            
            if (success) {
                _isDefaultPassword.value = false
                _uiState.value = PasswordChangeUiState.Success
            } else {
                _uiState.value = PasswordChangeUiState.Error("Failed to change password")
            }
        }
    }
    
    /**
     * Reset state
     */
    fun resetState() {
        _uiState.value = PasswordChangeUiState.Idle
    }
}

/**
 * UI state for password change
 */
sealed class PasswordChangeUiState {
    object Idle : PasswordChangeUiState()
    object Loading : PasswordChangeUiState()
    object Success : PasswordChangeUiState()
    data class Error(val message: String) : PasswordChangeUiState()
}
