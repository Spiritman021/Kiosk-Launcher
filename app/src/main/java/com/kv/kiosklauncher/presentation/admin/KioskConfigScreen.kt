package com.kv.kiosklauncher.presentation.admin

import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Kiosk mode configuration screen
 */
@Composable
fun KioskConfigScreen(
    viewModel: KioskConfigViewModel = hiltViewModel()
) {
    val configuration by viewModel.configuration.collectAsState(initial = com.kv.kiosklauncher.data.model.KioskConfiguration())
    val setupStatus by viewModel.setupStatus.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Setup Status Card
        SetupStatusCard(
            setupStatus = setupStatus,
            onCheckStatus = { viewModel.checkSetupStatus() },
            onEnableDeviceAdmin = {
                val intent = viewModel.getDeviceOwnerCommand().let {
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                }
                context.startActivity(intent)
            },
            onRequestUsageStats = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
            },
            onCopyAdbCommand = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ADB Command", viewModel.getDeviceOwnerCommand())
                clipboard.setPrimaryClip(clip)
            }
        )
        
        // Kiosk Mode Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Kiosk Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Enable Kiosk Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Kiosk Mode", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Lock device to whitelisted apps only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = configuration.isKioskModeEnabled,
                        onCheckedChange = { viewModel.toggleKioskMode(it) }
                    )
                }
                
                Divider()
                
                // Use Lock Task Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use Lock Task Mode", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Requires device owner (Android 9+)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = configuration.useLockTaskMode,
                        onCheckedChange = { viewModel.toggleLockTaskMode(it) },
                        enabled = configuration.isKioskModeEnabled
                    )
                }
                
                Divider()
                
                // Emergency Exit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Emergency Exit", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Allow exit with emergency code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = configuration.emergencyExitEnabled,
                        onCheckedChange = { viewModel.toggleEmergencyExit(it) }
                    )
                }
            }
        }
        
        // Display Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Display Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Grid Columns
                Column {
                    Text("Grid Columns: ${configuration.gridColumns}", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = configuration.gridColumns.toFloat(),
                        onValueChange = { viewModel.setGridColumns(it.toInt()) },
                        valueRange = 2f..6f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Divider()
                
                // Show App Names
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show App Names", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Display app names below icons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = configuration.showAppNames,
                        onCheckedChange = { viewModel.toggleShowAppNames(it) }
                    )
                }
            }
        }
    }
}

/**
 * Setup status card showing permissions and requirements
 */
@Composable
fun SetupStatusCard(
    setupStatus: KioskSetupStatus,
    onCheckStatus: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
    onRequestUsageStats: () -> Unit,
    onCopyAdbCommand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Setup Status",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onCheckStatus) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            
            when (setupStatus) {
                is KioskSetupStatus.Checking -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                is KioskSetupStatus.Ready -> {
                    // Lock Task Mode Support
                    StatusItem(
                        label = "Lock Task Mode",
                        isReady = setupStatus.lockTaskModeSupported,
                        readyText = "Supported",
                        notReadyText = "Not supported (Android 5.0+ required)"
                    )
                    
                    // Device Owner
                    StatusItem(
                        label = "Device Owner",
                        isReady = setupStatus.isDeviceOwner,
                        readyText = "Configured",
                        notReadyText = "Not configured",
                        action = if (!setupStatus.isDeviceOwner) {
                            {
                                Column {
                                    Button(
                                        onClick = onCopyAdbCommand,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copy ADB Command")
                                    }
                                    Text(
                                        text = "Run the copied command via ADB to set device owner",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else null
                    )
                    
                    // Device Admin
                    StatusItem(
                        label = "Device Admin",
                        isReady = setupStatus.isDeviceAdmin,
                        readyText = "Enabled",
                        notReadyText = "Not enabled",
                        action = if (!setupStatus.isDeviceAdmin) {
                            {
                                Button(
                                    onClick = onEnableDeviceAdmin,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Enable Device Admin")
                                }
                            }
                        } else null
                    )
                    
                    // Usage Stats Permission
                    StatusItem(
                        label = "Usage Stats Permission",
                        isReady = setupStatus.hasUsageStatsPermission,
                        readyText = "Granted",
                        notReadyText = "Not granted",
                        action = if (!setupStatus.hasUsageStatsPermission) {
                            {
                                Button(
                                    onClick = onRequestUsageStats,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}

/**
 * Individual status item
 */
@Composable
fun StatusItem(
    label: String,
    isReady: Boolean,
    readyText: String,
    notReadyText: String,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isReady) readyText else notReadyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
        
        if (!isReady && action != null) {
            action()
        }
    }
}
