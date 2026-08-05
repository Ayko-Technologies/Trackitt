package com.ayko.trackitt.service.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PinCodeSecurity {

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
