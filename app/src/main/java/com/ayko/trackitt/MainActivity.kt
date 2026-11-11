package com.ayko.trackitt

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.ayko.trackitt.ui.component.LocalStatusBarBackdrop
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ayko.trackitt.service.security.AppLockManager
import com.ayko.trackitt.service.sms.SmsCatchUpWorker
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
        // Rotating recreates the activity; it shouldn't ask for the PIN again. Leaving the app
        // clears this in onStop before the state is saved, so it still locks then.
        sessionUnlocked = !appLockManager.isLockEnabled() ||
            savedInstanceState?.getBoolean(KEY_SESSION_UNLOCKED) == true
        showLock = !sessionUnlocked
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            TrackittTheme {
                val statusBarBackdrop = remember { mutableStateOf<Color?>(null) }
                CompositionLocalProvider(LocalStatusBarBackdrop provides statusBarBackdrop) {
                    // Behind the status bar: the app's surface, unless a full-bleed screen asked for its own colour.
                    val backdrop = statusBarBackdrop.value?.takeUnless { showLock } ?: MaterialTheme.colorScheme.surface
                    Box(Modifier.fillMaxSize().background(backdrop)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                        ) {
                            // In landscape the camera cutout sits on a side edge; keep content out from under it.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                            ) {
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
        }
    }

    override fun onStart() {
        super.onStart()
        SmsCatchUpWorker.runNow(this)
        if (appLockManager.isLockEnabled() && !sessionUnlocked) {
            lockApp()
        } else if (!appLockManager.isLockEnabled()) {
            sessionUnlocked = true
            showLock = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SESSION_UNLOCKED, sessionUnlocked)
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
            // Title only: the system panel is short in landscape and extra lines crowd it.
            .setNegativeButtonText("Use PIN instead")
            .setAllowedAuthenticators(AppLockManager.BIOMETRIC_AUTHENTICATORS)
            .build()
        prompt.authenticate(promptInfo)
    }

    private companion object {
        const val KEY_SESSION_UNLOCKED = "session_unlocked"
    }
}
