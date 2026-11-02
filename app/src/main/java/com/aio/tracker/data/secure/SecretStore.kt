package com.aio.tracker.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the Gemini API key encrypted with an AES-256-GCM key that lives inside the
 * Android Keystore (hardware backed on most devices). The raw key material never leaves
 * the Keystore, the plaintext API key is never written to disk, never shown again in the UI,
 * and it is excluded from backups and data exports.
 */
class SecretStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _hasApiKey = MutableStateFlow(prefs.contains(KEY_GEMINI))
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    /** Returns a masked hint such as "AIza…9xQ2" without revealing the key. */
    val apiKeyHint: String? get() = prefs.getString(KEY_HINT, null)

    suspend fun saveApiKey(plain: String) = withContext(Dispatchers.IO) {
        val clean = plain.trim()
        require(clean.isNotEmpty()) { "API key is empty" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        val payload = b64(cipher.iv) + ":" + b64(encrypted)
        val hint = if (clean.length > 8) clean.take(4) + "••••" + clean.takeLast(4) else "••••"
        prefs.edit().putString(KEY_GEMINI, payload).putString(KEY_HINT, hint).commit()
        _hasApiKey.value = true
    }

    /** Decrypts the key in memory only for the duration of a request. */
    suspend fun readApiKey(): String? = withContext(Dispatchers.IO) {
        val payload = prefs.getString(KEY_GEMINI, null) ?: return@withContext null
        runCatching {
            val (ivPart, dataPart) = payload.split(":", limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, unb64(ivPart)))
            String(cipher.doFinal(unb64(dataPart)), Charsets.UTF_8)
        }.getOrNull()
    }

    suspend fun clearApiKey() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_GEMINI).remove(KEY_HINT).commit()
        _hasApiKey.value = false
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    private companion object {
        const val PREFS = "secure_store"
        const val KEY_GEMINI = "gemini_api_key_enc"
        const val KEY_HINT = "gemini_api_key_hint"
        const val ALIAS = "aio_tracker_api_key_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
