package com.kv.kiosklauncher.presentation.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kv.kiosklauncher.presentation.theme.KioskLauncherTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Admin settings activity for managing whitelist and configuration
 */
@AndroidEntryPoint
class AdminSettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            KioskLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminSettingsScreen(
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}
