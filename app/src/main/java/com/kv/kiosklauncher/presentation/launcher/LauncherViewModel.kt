package com.kv.kiosklauncher.presentation.launcher

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kv.kiosklauncher.data.dao.WhitelistedAppDao
import com.kv.kiosklauncher.data.model.AppInfo
import com.kv.kiosklauncher.service.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for LauncherActivity.
 * Manages whitelisted apps display and session state.
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {
    
    private val packageManager: PackageManager = application.packageManager
    
    val isSessionActive = sessionManager.isSessionActive
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val whitelistedApps: StateFlow<List<AppInfo>> = whitelistedAppDao
        .getAllWhitelistedApps()
        .map { whitelisted ->
            whitelisted.mapNotNull { app ->
                try {
                    val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                    AppInfo(
                        packageName = app.packageName,
                        appName = app.appName,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        isWhitelisted = true
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null // App not installed
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    private val _remainingTimeText = MutableStateFlow("No active session")
    val remainingTimeText: StateFlow<String> = _remainingTimeText.asStateFlow()
    
    init {
        // Update time display every second
        viewModelScope.launch {
            while (true) {
                if (isSessionActive.value) {
                    val session = sessionManager.currentSession.value
                    if (session != null) {
                        if (session.isIndefinite) {
                            // Show elapsed time for indefinite sessions
                            val elapsedMs = session.getElapsedTimeMs()
                            val hours = (elapsedMs / 1000 / 60 / 60).toInt()
                            val minutes = ((elapsedMs / 1000 / 60) % 60).toInt()
                            val seconds = ((elapsedMs / 1000) % 60).toInt()
                            _remainingTimeText.value = if (hours > 0) {
                                String.format("Active: %d:%02d:%02d", hours, minutes, seconds)
                            } else {
                                String.format("Active: %d:%02d", minutes, seconds)
                            }
                        } else {
                            // Show remaining time for timed sessions
                            val remainingMs = sessionManager.getRemainingTimeMs()
                            val minutes = (remainingMs / 1000 / 60).toInt()
                            val seconds = ((remainingMs / 1000) % 60).toInt()
                            _remainingTimeText.value = String.format("%d:%02d remaining", minutes, seconds)
                        }
                    }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
        
        // Load active session on start
        viewModelScope.launch {
            sessionManager.loadActiveSession()
        }
    }
}
