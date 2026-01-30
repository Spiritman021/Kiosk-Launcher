package com.kv.kiosklauncher.presentation.permissions

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kv.kiosklauncher.ui.theme.KioskLauncherTheme
import com.kv.kiosklauncher.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that guides users through granting all required permissions.
 */
@AndroidEntryPoint
class PermissionSetupActivity : ComponentActivity() {
    
    private val viewModel: PermissionSetupViewModel by viewModels()
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
                PermissionSetupScreen(
                    viewModel = viewModel,
                    onPermissionClick = { permissionType ->
                        openPermissionSettings(permissionType)
                    },
                    onFinish = {
                        finish()
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
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

enum class PermissionType {
    USAGE_STATS,
    OVERLAY,
    ACCESSIBILITY,
    DEVICE_ADMIN,
    BATTERY_OPTIMIZATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    viewModel: PermissionSetupViewModel,
    onPermissionClick: (PermissionType) -> Unit,
    onFinish: () -> Unit
) {
    val permissionStates by viewModel.permissionStates.collectAsState()
    val allGranted by viewModel.allPermissionsGranted.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Setup") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Required Permissions",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grant all permissions for kiosk mode to work properly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(permissionStates) { state ->
                    PermissionCard(
                        state = state,
                        onClick = { onPermissionClick(state.type) }
                    )
                }
            }
            
            if (allGranted) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Continue")
                }
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
                imageVector = if (state.isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (state.isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
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
                TextButton(onClick = onClick) {
                    Text("Grant")
                }
            }
        }
    }
}

data class PermissionState(
    val type: PermissionType,
    val name: String,
    val description: String,
    val isGranted: Boolean
)
