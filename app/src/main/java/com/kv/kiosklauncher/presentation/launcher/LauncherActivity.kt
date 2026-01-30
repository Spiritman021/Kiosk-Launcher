package com.kv.kiosklauncher.presentation.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kv.kiosklauncher.data.model.AppInfo
import com.kv.kiosklauncher.presentation.admin.AdminSettingsActivity
import com.kv.kiosklauncher.presentation.timer.TimerActivity
import com.kv.kiosklauncher.ui.theme.KioskLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main launcher activity - shows whitelisted apps during active session.
 * Acts as the home screen for the kiosk mode.
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {
    
    private val viewModel: LauncherViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KioskLauncherTheme {
                LauncherScreen(
                    viewModel = viewModel,
                    onAppClick = { appInfo ->
                        launchApp(appInfo.packageName)
                    },
                    onTimerClick = {
                        startActivity(Intent(this, TimerActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, AdminSettingsActivity::class.java))
                    }
                )
            }
        }
    }
    
    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onBackPressed() {
        // Disable back button during active session
        if (viewModel.isSessionActive.value) {
            // Do nothing - prevent exit
        } else {
            super.onBackPressed()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    onAppClick: (AppInfo) -> Unit,
    onTimerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val whitelistedApps by viewModel.whitelistedApps.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val remainingTime by viewModel.remainingTimeText.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSessionActive) {
                        Column {
                            Text("Kiosk Mode Active")
                            Text(
                                text = remainingTime,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text("Kiosk Launcher")
                    }
                },
                actions = {
                    if (!isSessionActive) {
                        IconButton(onClick = onTimerClick) {
                            Icon(Icons.Default.Timer, "Start Session")
                        }
                        IconButton(onClick = onSettingsClick) { // Kept onSettingsClick lambda
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (whitelistedApps.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onTimerClick = onTimerClick
            )
        } else {
            AppGrid(
                apps = whitelistedApps,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAppClick = onAppClick
            )
        }
    }
}

@Composable
fun AppGrid(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    onAppClick: (AppInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(apps) { app ->
            AppItem(app = app, onClick = { onAppClick(app) })
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon would go here
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            // Placeholder for app icon
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    onTimerClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No whitelisted apps",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add apps to whitelist in settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onTimerClick) {
            Icon(Icons.Default.Timer, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Kiosk Session")
        }
    }
}
