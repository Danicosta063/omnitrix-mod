
package com.codeforge.ai

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class ApiKeyManager(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "codeforge_secure_settings",
            Context.MODE_PRIVATE
        )

    private val keyAlias = "codeforge_openrouter_key"

    private val androidKeyStore = KeyStore.getInstance(
        "AndroidKeyStore"
    ).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = androidKeyStore.getKey(
            keyAlias,
            null
        ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keySpecification = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or
                KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpecification)

        return keyGenerator.generateKey()
    }

    fun saveApiKey(apiKey: String) {
        require(apiKey.isNotBlank()) {
            "A chave de API não pode estar vazia."
        }

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes = cipher.doFinal(
            apiKey.trim().toByteArray(
                StandardCharsets.UTF_8
            )
        )

        val encryptedText = Base64.encodeToString(
            encryptedBytes,
            Base64.NO_WRAP
        )

        val initializationVector = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        val saved = preferences.edit()
            .putString("encrypted_api_key", encryptedText)
            .putString("api_key_iv", initializationVector)
            .commit()

        check(saved) {
            "Não foi possível salvar a chave de API."
        }
    }

    fun getApiKey(): String? {
        val encryptedText = preferences.getString(
            "encrypted_api_key",
            null
        ) ?: return null

        val initializationVector = preferences.getString(
            "api_key_iv",
            null
        ) ?: return null

        val encryptedBytes = Base64.decode(
            encryptedText,
            Base64.NO_WRAP
        )

        val ivBytes = Base64.decode(
            initializationVector,
            Base64.NO_WRAP
        )

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, ivBytes)
        )

        val decryptedBytes = cipher.doFinal(
            encryptedBytes
        )

        return String(
            decryptedBytes,
            StandardCharsets.UTF_8
        )
    }

    fun hasApiKey(): Boolean {
        return preferences.contains(
            "encrypted_api_key"
        ) && preferences.contains(
            "api_key_iv"
        )
    }

    fun deleteApiKey() {
        val removed = preferences.edit()
            .remove("encrypted_api_key")
            .remove("api_key_iv")
            .commit()

        check(removed) {
            "Não foi possível apagar a chave armazenada."
        }
    }
}
