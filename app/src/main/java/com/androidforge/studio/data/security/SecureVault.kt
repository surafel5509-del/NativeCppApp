package com.androidforge.studio.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore-backed AES-GCM seal/unseal for API keys, tokens and
 * optional project encryption. Keys never leave secure hardware when available.
 */
@Singleton
class SecureVault @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    /** Encrypts [plain] and returns "iv:payload" Base64 pair. Empty input stays empty. */
    fun seal(plain: String): String {
        if (plain.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$iv:$payload"
    }

    fun unseal(sealed: String): String {
        if (sealed.isEmpty()) return ""
        val parts = sealed.split(':', limit = 2)
        if (parts.size != 2) return ""
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val payload = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "androidforge_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
