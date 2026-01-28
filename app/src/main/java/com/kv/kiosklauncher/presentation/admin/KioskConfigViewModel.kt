package com.kv.kiosklauncher.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.util.AppLaunchMonitor
import com.kv.kiosklauncher.util.LockTaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for kiosk mode configuration
 */
@HiltViewModel
class KioskConfigViewModel @Inject constructor(
    private val configurationRepository: ConfigurationRepository,
    private val lockTaskManager: LockTaskManager,
    private val appLaunchMonitor: AppLaunchMonitor
) : ViewModel() {
    
    val configuration = configurationRepository.configuration
    
    private val _setupStatus = MutableStateFlow<KioskSetupStatus>(KioskSetupStatus.Checking)
    val setupStatus: StateFlow<KioskSetupStatus> = _setupStatus.asStateFlow()
    
    init {
        checkSetupStatus()
    }
    
    /**
     * Check kiosk mode setup status
     */
    fun checkSetupStatus() {
        viewModelScope.launch {
            val isDeviceOwner = lockTaskManager.isDeviceOwner()
            val isDeviceAdmin = lockTaskManager.isDeviceAdminEnabled()
            val hasUsageStats = appLaunchMonitor.hasUsageStatsPermission()
            
            _setupStatus.value = KioskSetupStatus.Ready(
                isDeviceOwner = isDeviceOwner,
                isDeviceAdmin = isDeviceAdmin,
                hasUsageStatsPermission = hasUsageStats,
                lockTaskModeSupported = lockTaskManager.isLockTaskModeSupported()
            )
        }
    }
    
    /**
     * Toggle kiosk mode
     */
    fun toggleKioskMode(enabled: Boolean) {
        viewModelScope.launch {
            configurationRepository.setKioskModeEnabled(enabled)
        }
    }
    
    /**
     * Toggle Lock Task Mode preference
     */
    fun toggleLockTaskMode(enabled: Boolean) {
        viewModelScope.launch {
            configurationRepository.setUseLockTaskMode(enabled)
        }
    }
    
    /**
     * Toggle emergency exit
     */
    fun toggleEmergencyExit(enabled: Boolean) {
        viewModelScope.launch {
            configurationRepository.setEmergencyExitEnabled(enabled)
        }
    }
    
    /**
     * Set grid columns
     */
    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            configurationRepository.setGridColumns(columns)
        }
    }
    
    /**
     * Toggle show app names
     */
    fun toggleShowAppNames(enabled: Boolean) {
        viewModelScope.launch {
            configurationRepository.setShowAppNames(enabled)
        }
    }
    
    /**
     * Get ADB command for device owner setup
     */
    fun getDeviceOwnerCommand(): String {
        return lockTaskManager.getDeviceOwnerAdbCommand()
    }
    
    /**
     * Get setup instructions
     */
    fun getSetupInstructions(): String {
        return lockTaskManager.getSetupInstructions()
    }
}

/**
 * Kiosk setup status
 */
sealed class KioskSetupStatus {
    object Checking : KioskSetupStatus()
    data class Ready(
        val isDeviceOwner: Boolean,
        val isDeviceAdmin: Boolean,
        val hasUsageStatsPermission: Boolean,
        val lockTaskModeSupported: Boolean
    ) : KioskSetupStatus()
}
