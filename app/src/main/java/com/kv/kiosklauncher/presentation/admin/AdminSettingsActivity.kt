package com.kv.kiosklauncher.presentation.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kv.kiosklauncher.presentation.permissions.PermissionState
import com.kv.kiosklauncher.presentation.permissions.PermissionType
import com.kv.kiosklauncher.service.AppMonitorService
import com.kv.kiosklauncher.ui.theme.KioskLauncherTheme
import com.kv.kiosklauncher.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint

/**
 * Admin settings activity - allows admin to stop kiosk mode and manage permissions.
 */
@AndroidEntryPoint
class AdminSettingsActivity : ComponentActivity() {
    
    private val viewModel: AdminSettingsViewModel by viewModels()
    private lateinit var permissionHelper: PermissionHelper
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Refresh permission status after returning from settings
        viewModel.refreshPermissions()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)
        
        setContent {
            KioskLauncherTheme {
                AdminSettingsScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onStopKiosk = {
                        viewModel.stopSession()
                        stopMonitoringService()
                        finish()
                    },
                    onPermissionClick = { permissionType ->
                        openPermissionSettings(permissionType)
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
    
    private fun stopMonitoringService() {
        val intent = Intent(this, AppMonitorService::class.java)
        stopService(intent)
    }
    
    private fun openPermissionSettings(permissionType: PermissionType) {
        val intent = when (permissionType) {
            PermissionType.USAGE_STATS -> permissionHelper.openUsageStatsSettings()
            PermissionType.OVERLAY -> permissionHelper.openOverlaySettings()
            PermissionType.ACCESSIBILITY -> permissionHelper.openAccessibilitySettings()
            PermissionType.DEVICE_ADMIN -> permissionHelper.openDeviceAdminSettings()
            PermissionType.BATTERY_OPTIMIZATION -> permissionHelper.openBatteryOptimizationSettings()
        }
        permissionLauncher.launch(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    viewModel: AdminSettingsViewModel,
    onBack: () -> Unit,
    onStopKiosk: () -> Unit,
    onPermissionClick: (PermissionType) -> Unit
) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val permissionStates by viewModel.permissionStates.collectAsState()
    val allPermissionsGranted by viewModel.allPermissionsGranted.collectAsState()
    var showStopConfirmation by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Session Status Card
            item {
                SessionStatusCard(
                    isSessionActive = isSessionActive,
                    onStopKiosk = { showStopConfirmation = true }
                )
            }
            
            // Permissions Section Header
            item {
                Text(
                    text = "Required Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    text = "Enable all permissions for kiosk mode to work properly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Permission Status Summary
            item {
                PermissionSummaryCard(
                    allGranted = allPermissionsGranted,
                    grantedCount = permissionStates.count { it.isGranted },
                    totalCount = permissionStates.size
                )
            }
            
            // Permission Cards
            items(permissionStates) { state ->
                PermissionCard(
                    state = state,
                    onClick = { onPermissionClick(state.type) }
                )
            }
        }
    }
    
    // Confirmation dialog
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = { Text("Stop Kiosk Mode?") },
            text = { Text("Are you sure you want to stop kiosk mode? All restrictions will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmation = false
                        onStopKiosk()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionStatusCard(
    isSessionActive: Boolean,
    onStopKiosk: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSessionActive) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSessionActive) "Kiosk Mode Active" else "No Active Session",
                style = MaterialTheme.typography.titleLarge,
                color = if (isSessionActive) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isSessionActive) {
                    "Only admin can stop kiosk mode"
                } else {
                    "Kiosk mode is not currently active"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSessionActive) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (isSessionActive) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onStopKiosk,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Kiosk Mode")
                }
            }
        }
    }
}

@Composable
fun PermissionSummaryCard(
    allGranted: Boolean,
    grantedCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (allGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (allGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = if (allGranted) {
                        "All Permissions Granted"
                    } else {
                        "Permissions Required"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$grantedCount of $totalCount permissions enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    state: PermissionState,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (state.isGranted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (state.isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (state.isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!state.isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onClick) {
                    Text("Enable")
                }
            }
        }
    }
}
