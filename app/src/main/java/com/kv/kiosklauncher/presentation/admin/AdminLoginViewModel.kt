package com.kv.kiosklauncher.presentation.admin

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
 * ViewModel for admin authentication
 */
@HiltViewModel
class AdminLoginViewModel @Inject constructor(
    private val securePrefsRepository: SecurePreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AdminLoginUiState>(AdminLoginUiState.Idle)
    val uiState: StateFlow<AdminLoginUiState> = _uiState.asStateFlow()
    
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
     * Verify login credentials
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AdminLoginUiState.Loading
            
            // Simulate network delay
            kotlinx.coroutines.delay(500)
            
            // Verify password
            val isValid = securePrefsRepository.verifyPassword(password)
            
            if (isValid) {
                _uiState.value = AdminLoginUiState.Success
            } else {
                _uiState.value = AdminLoginUiState.Error("Invalid credentials")
            }
        }
    }
    
    /**
     * Reset state
     */
    fun resetState() {
        _uiState.value = AdminLoginUiState.Idle
    }
}

/**
 * UI state for admin login
 */
sealed class AdminLoginUiState {
    object Idle : AdminLoginUiState()
    object Loading : AdminLoginUiState()
    object Success : AdminLoginUiState()
    data class Error(val message: String) : AdminLoginUiState()
}
