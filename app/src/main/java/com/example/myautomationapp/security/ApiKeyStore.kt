package com.example.myautomationapp.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences("secure_provider_settings", Context.MODE_PRIVATE)
    private val alias = "my_automation_provider_key"

    fun save(provider: String, key: String) {
        require(provider in setOf("openai", "gemini")) { "Unsupported provider" }
        require(key.trim().length >= 10) { "API key is too short" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = Base64.encodeToString(cipher.doFinal(key.trim().toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        preferences.edit().putString("${provider}_ciphertext", encrypted).putString("${provider}_iv", iv).apply()
    }

    fun read(provider: String): String? = runCatching {
        val ciphertext = preferences.getString("${provider}_ciphertext", null) ?: return null
        val iv = preferences.getString("${provider}_iv", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8)
    }.getOrNull()

    fun delete(provider: String) = preferences.edit().remove("${provider}_ciphertext").remove("${provider}_iv").apply()
    fun isConfigured(provider: String) = read(provider) != null

    private fun secretKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }
}
