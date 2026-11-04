package com.ayko.trackitt

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ayko.trackitt.service.security.AppLockManager
import com.ayko.trackitt.ui.navigation.TrackittNavGraph
import com.ayko.trackitt.ui.screen.security.AppLockScreen
import com.ayko.trackitt.ui.theme.TrackittTheme

class MainActivity : FragmentActivity() {
    private lateinit var appLockManager: AppLockManager
    private var sessionUnlocked by mutableStateOf(false)
    private var showLock by mutableStateOf(false)
    private var biometricPromptNonce by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appLockManager = AppLockManager(this)
        if (appLockManager.isLockEnabled()) {
            // With app lock on, keep finances out of screenshots and the recent-apps preview too.
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        sessionUnlocked = !appLockManager.isLockEnabled()
        showLock = appLockManager.isLockEnabled()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            TrackittTheme {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TrackittNavGraph()
                        if (showLock) {
                            AppLockScreen(
                                showBiometric = appLockManager.isBiometricEnabled(),
                                biometricPromptNonce = biometricPromptNonce,
                                onUnlockWithPin = { pin ->
                                    val unlocked = appLockManager.verifyPin(pin)
                                    if (unlocked) unlockApp()
                                    unlocked
                                },
                                onRequestBiometric = { showBiometricPrompt() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (appLockManager.isLockEnabled() && !sessionUnlocked) {
            lockApp()
        } else if (!appLockManager.isLockEnabled()) {
            sessionUnlocked = true
            showLock = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && appLockManager.isLockEnabled()) {
            sessionUnlocked = false
        }
    }

    private fun lockApp() {
        showLock = true
        biometricPromptNonce++
    }

    private fun unlockApp() {
        sessionUnlocked = true
        showLock = false
    }

    private fun showBiometricPrompt() {
        if (!appLockManager.isBiometricEnabled()) return

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockApp()
                }
            },
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Trackitt")
            .setSubtitle("Use biometrics to open the app")
            .setNegativeButtonText("Use PIN instead")
            .setAllowedAuthenticators(AppLockManager.BIOMETRIC_AUTHENTICATORS)
            .build()
        prompt.authenticate(promptInfo)
    }
}
