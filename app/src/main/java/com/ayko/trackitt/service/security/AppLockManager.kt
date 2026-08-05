package com.ayko.trackitt.service.security

import android.content.Context
import androidx.biometric.BiometricManager

class AppLockManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val biometricManager = BiometricManager.from(context)

    fun isLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ENABLED, false) && hasPin()
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled && hasPin()).apply()
    }

    fun hasPin(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun savePin(pin: String) {
        val salt = PinCodeSecurity.generateSalt()
        val hash = PinCodeSecurity.hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return PinCodeSecurity.hashPin(pin, salt) == expected
    }

    fun canUseBiometrics(): Boolean {
        val status = biometricManager.canAuthenticate(BIOMETRIC_AUTHENTICATORS)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(): Boolean {
        return isLockEnabled() && canUseBiometrics() && prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled && canUseBiometrics()).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_lock"
        private const val KEY_LOCK_ENABLED = "enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"

        const val BIOMETRIC_AUTHENTICATORS: Int =
            BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}
