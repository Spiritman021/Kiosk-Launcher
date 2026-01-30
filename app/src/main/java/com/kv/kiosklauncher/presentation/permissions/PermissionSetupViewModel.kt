package com.kv.kiosklauncher.presentation.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for PermissionSetupActivity.
 * Manages permission states and checks.
 */
@HiltViewModel
class PermissionSetupViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val permissionHelper = PermissionHelper(application)
    
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
                    name = "Usage Stats Access",
                    description = "Required to detect which app is currently running",
                    isGranted = permissionHelper.hasUsageStatsPermission()
                ),
                PermissionState(
                    type = PermissionType.OVERLAY,
                    name = "Display Over Other Apps",
                    description = "Required to show blocking overlay",
                    isGranted = permissionHelper.hasOverlayPermission()
                ),
                PermissionState(
                    type = PermissionType.ACCESSIBILITY,
                    name = "Accessibility Service",
                    description = "Required for instant app detection",
                    isGranted = permissionHelper.isAccessibilityServiceEnabled()
                ),
                PermissionState(
                    type = PermissionType.DEVICE_ADMIN,
                    name = "Device Administrator",
                    description = "Required to lock screen when blocking apps",
                    isGranted = permissionHelper.isDeviceAdminEnabled()
                ),
                PermissionState(
                    type = PermissionType.BATTERY_OPTIMIZATION,
                    name = "Battery Optimization",
                    description = "Required to keep monitoring service running",
                    isGranted = permissionHelper.isBatteryOptimizationDisabled()
                )
            )
            
            _permissionStates.value = states
            _allPermissionsGranted.value = states.all { it.isGranted }
        }
    }
}
