package com.kv.kiosklauncher.presentation.timer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kv.kiosklauncher.presentation.permissions.PermissionSetupActivity
import com.kv.kiosklauncher.service.AppMonitorService
import com.kv.kiosklauncher.ui.theme.KioskLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for setting up and controlling kiosk session timer.
 */
@AndroidEntryPoint
class TimerActivity : ComponentActivity() {
    
    private val viewModel: TimerViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KioskLauncherTheme {
                TimerScreen(
                    viewModel = viewModel,
                    onStartSession = {
                        if (viewModel.hasAllPermissions()) {
                            viewModel.startIndefiniteSession()
                            startMonitoringService()
                            finish()
                        } else {
                            // Navigate to permission setup
                            startActivity(Intent(this, PermissionSetupActivity::class.java))
                        }
                    },
                    onStopSession = {
                        viewModel.stopSession()
                        stopMonitoringService()
                        finish()
                    }
                )
            }
        }
    }
    
    private fun startMonitoringService() {
        val intent = Intent(this, AppMonitorService::class.java).apply {
            action = AppMonitorService.ACTION_START_MONITORING
        }
        startForegroundService(intent)
    }
    
    private fun stopMonitoringService() {
        val intent = Intent(this, AppMonitorService::class.java).apply {
            action = AppMonitorService.ACTION_STOP_MONITORING
        }
        startService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit
) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val whitelistCount by viewModel.whitelistCount.collectAsState()
    val hasAllPermissions by viewModel.hasAllPermissions.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kiosk Mode") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSessionActive) {
                // Session is active - show stop button
                ActiveSessionView(
                    onStopSession = onStopSession
                )
            } else {
                // No active session - show start button
                KioskSetupView(
                    whitelistCount = whitelistCount,
                    hasAllPermissions = hasAllPermissions,
                    onStartSession = onStartSession
                )
            }
        }
    }
}

@Composable
fun KioskSetupView(
    whitelistCount: Int,
    hasAllPermissions: Boolean,
    onStartSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kiosk Mode",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Start kiosk mode to restrict access to whitelisted apps only",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "$whitelistCount apps whitelisted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!hasAllPermissions) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Some permissions are missing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Kiosk Mode")
            }
        }
    }
}

@Composable
fun ActiveSessionView(
    onStopSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Session Active",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Kiosk mode is currently running",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onStopSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Session")
            }
        }
    }
}
