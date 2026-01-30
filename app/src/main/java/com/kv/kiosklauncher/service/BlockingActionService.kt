package com.kv.kiosklauncher.service

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kv.kiosklauncher.data.dao.BlockLogDao
import com.kv.kiosklauncher.data.dao.KioskSettingsDao
import com.kv.kiosklauncher.data.model.BlockLog
import com.kv.kiosklauncher.data.model.BlockingMode
import com.kv.kiosklauncher.receiver.KioskDeviceAdmin
import com.kv.kiosklauncher.util.AppDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that executes blocking actions when unauthorized apps are detected.
 * Handles screen-off, redirect, and overlay blocking based on settings.
 */
@Singleton
class BlockingActionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kioskSettingsDao: KioskSettingsDao,
    private val blockLogDao: BlockLogDao,
    private val sessionManager: SessionManager,
    private val appDetector: AppDetector
) {
    
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val deviceAdminComponent: ComponentName by lazy {
        ComponentName(context, KioskDeviceAdmin::class.java)
    }
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Execute blocking action for the given package
     */
    suspend fun blockApp(packageName: String) {
        val settings = kioskSettingsDao.getSettings()
        val appName = appDetector.getAppName(packageName)
        val currentSession = sessionManager.currentSession.value
        
        // Log the block event
        if (currentSession != null) {
            val blockLog = BlockLog(
                packageName = packageName,
                appName = appName,
                sessionId = currentSession.id,
                actionTaken = settings?.blockingMode?.name ?: BlockingMode.BOTH.name
            )
            blockLogDao.insertBlockLog(blockLog)
        }
        
        // Vibrate if enabled
        if (settings?.vibrateOnBlock == true) {
            vibrateShort()
        }
        
        // Execute blocking action based on mode
        when (settings?.blockingMode ?: BlockingMode.BOTH) {
            BlockingMode.REDIRECT -> {
                redirectToLauncher()
            }
            BlockingMode.SCREEN_OFF -> {
                lockScreen()
            }
            BlockingMode.BOTH -> {
                lockScreen()
                // Wait a bit then redirect
                CoroutineScope(Dispatchers.Main).launch {
                    delay(settings?.screenOffRedirectDelayMs ?: 500)
                    redirectToLauncher()
                }
            }
        }
    }
    
    /**
     * Lock the screen using Device Admin
     */
    private fun lockScreen() {
        try {
            if (isDeviceAdminActive()) {
                devicePolicyManager.lockNow()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to redirect if screen lock fails
            redirectToLauncher()
        }
    }
    
    /**
     * Redirect user to launcher home screen
     */
    private fun redirectToLauncher() {
        try {
            val launcherIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launcherIntent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                context.startActivity(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if device admin is active
     */
    fun isDeviceAdminActive(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(deviceAdminComponent)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Vibrate for a short duration
     */
    private fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
