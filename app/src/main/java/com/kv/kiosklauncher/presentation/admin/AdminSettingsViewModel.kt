package com.kv.kiosklauncher.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.model.AppInfo
import com.kv.kiosklauncher.data.repository.WhitelistRepository
import com.kv.kiosklauncher.util.AppManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for admin settings
 */
@HiltViewModel
class AdminSettingsViewModel @Inject constructor(
    private val whitelistRepository: WhitelistRepository,
    private val appManager: AppManager
) : ViewModel() {
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()
    
    private val _whitelistedPackages = MutableStateFlow<Set<String>>(emptySet())
    val whitelistedPackages: StateFlow<Set<String>> = _whitelistedPackages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadApps()
    }
    
    /**
     * Load all installed apps and whitelist status
     */
    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Load installed apps
            val apps = appManager.getAllInstalledApps()
            _installedApps.value = apps
            
            // Load whitelisted packages
            whitelistRepository.getAllApps().collect { whitelistEntries ->
                _whitelistedPackages.value = whitelistEntries
                    .filter { it.isEnabled }
                    .map { it.packageName }
                    .toSet()
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle app whitelist status
     */
    fun toggleAppWhitelist(app: AppInfo) {
        viewModelScope.launch {
            val isWhitelisted = _whitelistedPackages.value.contains(app.packageName)
            
            if (isWhitelisted) {
                whitelistRepository.removeFromWhitelist(app.packageName)
            } else {
                whitelistRepository.addToWhitelist(app.packageName, app.appName)
            }
        }
    }
    
    /**
     * Check if app is whitelisted
     */
    fun isAppWhitelisted(packageName: String): Boolean {
        return _whitelistedPackages.value.contains(packageName)
    }
}
