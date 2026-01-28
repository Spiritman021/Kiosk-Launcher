package com.kv.kiosklauncher.presentation.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.model.AppInfo
import com.kv.kiosklauncher.data.model.KioskConfiguration
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.util.AppManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the launcher home screen
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val whitelistRepository: WhitelistRepository,
    private val configurationRepository: ConfigurationRepository,
    private val appManager: AppManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LauncherUiState>(LauncherUiState.Loading)
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()
    
    private val _configuration = MutableStateFlow(KioskConfiguration())
    val configuration: StateFlow<KioskConfiguration> = _configuration.asStateFlow()
    
    init {
        loadWhitelistedApps()
        loadConfiguration()
    }
    
    /**
     * Load whitelisted apps
     */
    private fun loadWhitelistedApps() {
        viewModelScope.launch {
            combine(
                whitelistRepository.getWhitelistedApps(),
                configurationRepository.configuration
            ) { whitelistEntries, config ->
                _configuration.value = config
                
                // Get full app info for whitelisted apps
                val apps = whitelistEntries.mapNotNull { entry ->
                    appManager.getAppInfo(entry.packageName)
                }
                
                if (apps.isEmpty()) {
                    LauncherUiState.Empty
                } else {
                    LauncherUiState.Success(
                        apps = apps,
                        gridColumns = config.gridColumns,
                        showAppNames = config.showAppNames
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * Load configuration
     */
    private fun loadConfiguration() {
        viewModelScope.launch {
            configurationRepository.configuration.collect { config ->
                _configuration.value = config
            }
        }
    }
    
    /**
     * Launch an app
     */
    fun launchApp(app: AppInfo) {
        viewModelScope.launch {
            val success = appManager.launchApp(app.packageName)
            if (!success) {
                _uiState.value = LauncherUiState.Error("Failed to launch app")
            }
        }
    }
    
    /**
     * Disable kiosk mode (for emergency exit)
     */
    fun disableKioskMode() {
        viewModelScope.launch {
            configurationRepository.setKioskModeEnabled(false)
        }
    }
    
    /**
     * Refresh apps list
     */
    fun refreshApps() {
        loadWhitelistedApps()
    }
}

/**
 * UI state for launcher screen
 */
sealed class LauncherUiState {
    object Loading : LauncherUiState()
    object Empty : LauncherUiState()
    data class Success(
        val apps: List<AppInfo>,
        val gridColumns: Int = 3,
        val showAppNames: Boolean = true
    ) : LauncherUiState()
    data class Error(val message: String) : LauncherUiState()
}
