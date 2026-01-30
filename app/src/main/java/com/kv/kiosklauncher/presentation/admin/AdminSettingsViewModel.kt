package com.kv.kiosklauncher.presentation.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.presentation.permissions.PermissionState
import com.kv.kiosklauncher.presentation.permissions.PermissionType
import com.kv.kiosklauncher.service.SessionManager
import com.kv.kiosklauncher.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AdminSettingsActivity.
 * Manages session state, permission states, and admin operations.
 */
@HiltViewModel
class AdminSettingsViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {
    
    private val permissionHelper = PermissionHelper(application)
    
    val isSessionActive: StateFlow<Boolean> = sessionManager.isSessionActive
    
    private val _permissionStates = MutableStateFlow<List<PermissionState>>(emptyList())
    val permissionStates: StateFlow<List<PermissionState>> = _permissionStates.asStateFlow()
    
    private val _allPermissionsGranted = MutableStateFlow(false)
    val allPermissionsGranted: StateFlow<Boolean> = _allPermissionsGranted.asStateFlow()
    
    init {
        refreshPermissions()
    }
    
    fun refreshPermissions() {
        viewModelScope.launch {
            val states = listOf(
                PermissionState(
                    type = PermissionType.USAGE_STATS,
                    name = "App usage data",
                    description = "Track what apps you're using and how often",
                    isGranted = permissionHelper.hasUsageStatsPermission()
                ),
                PermissionState(
                    type = PermissionType.OVERLAY,
                    name = "Display over other apps",
                    description = "Display on top of other apps you're using",
                    isGranted = permissionHelper.hasOverlayPermission()
                ),
                PermissionState(
                    type = PermissionType.ACCESSIBILITY,
                    name = "Accessibility Service",
                    description = "Monitor app launches to enforce kiosk restrictions",
                    isGranted = permissionHelper.isAccessibilityServiceEnabled()
                ),
                PermissionState(
                    type = PermissionType.DEVICE_ADMIN,
                    name = "Device Administrator",
                    description = "Lock the screen when blocking apps",
                    isGranted = permissionHelper.isDeviceAdminEnabled()
                ),
                PermissionState(
                    type = PermissionType.BATTERY_OPTIMIZATION,
                    name = "Battery Optimization",
                    description = "Run in background to keep monitoring active",
                    isGranted = permissionHelper.isBatteryOptimizationDisabled()
                )
            )
            
            _permissionStates.value = states
            _allPermissionsGranted.value = states.all { it.isGranted }
        }
    }
    
    fun stopSession() {
        viewModelScope.launch {
            sessionManager.stopSession()
        }
    }
}
