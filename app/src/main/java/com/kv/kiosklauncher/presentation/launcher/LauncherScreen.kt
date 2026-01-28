package com.kv.kiosklauncher.presentation.launcher

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.kv.kiosklauncher.data.model.AppInfo
import com.kv.kiosklauncher.data.repository.ConfigurationRepository
import com.kv.kiosklauncher.presentation.admin.AdminLoginDialog
import com.kv.kiosklauncher.presentation.admin.AdminSettingsActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Launcher home screen displaying whitelisted apps
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdminDialog by remember { mutableStateOf(false) }
    var showEmergencyExitDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kiosk Launcher") },
                actions = {
                    // Settings button with long-press for emergency exit
                    IconButton(
                        onClick = { showAdminDialog = true },
                        modifier = Modifier.combinedClickable(
                            onClick = { showAdminDialog = true },
                            onLongClick = { showEmergencyExitDialog = true }
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is LauncherUiState.Loading -> {
                    LoadingScreen()
                }
                is LauncherUiState.Empty -> { // This state might be removed if Success handles empty list
                    EmptyScreen()
                }
                is LauncherUiState.Success -> {
                    if (state.apps.isEmpty()) {
                        EmptyScreen()
                    } else {
                        AppGrid(
                            apps = state.apps,
                            columns = state.gridColumns,
                            showAppNames = state.showAppNames,
                            onAppClick = { app ->
                                scope.launch {
                                    viewModel.launchApp(app)
                                }
                            }
                        )
                    }
                }
                is LauncherUiState.Error -> {
                    ErrorScreen(message = state.message)
                }
            }
        }
    }
    
    // Admin login dialog
    if (showAdminDialog) {
        AdminLoginDialog(
            onDismiss = { showAdminDialog = false },
            onLoginSuccess = {
                showAdminDialog = false
                // Navigate to admin settings
                val intent = Intent(context, AdminSettingsActivity::class.java)
                context.startActivity(intent)
            }
        )
    }
    
    // Emergency exit dialog
    if (showEmergencyExitDialog) {
        com.kv.kiosklauncher.presentation.emergency.EmergencyExitVerificationDialog(
            onDismiss = { showEmergencyExitDialog = false },
            onVerified = {
                showEmergencyExitDialog = false
                scope.launch {
                    // Disable kiosk mode
                    viewModel.disableKioskMode()
                    // Navigate to admin settings
                    val intent = Intent(context, AdminSettingsActivity::class.java)
                    context.startActivity(intent)
                }
            }
        )
    }
}

/**
 * Grid display of apps
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    columns: Int,
    showAppNames: Boolean,
    onAppClick: (AppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppItem(
                app = app,
                showAppName = showAppNames,
                onClick = { onAppClick(app) }
            )
        }
    }
}

/**
 * Individual app item
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo,
    showAppName: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon
        Card(
            modifier = Modifier.size(96.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                app.icon?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier.size(72.dp)
                    )
                } ?: Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = app.appName,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        
        // App name
        if (showAppName) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Loading state
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state
 */
@Composable
fun EmptyScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No apps whitelisted",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Access admin settings to add apps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state
 */
@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
