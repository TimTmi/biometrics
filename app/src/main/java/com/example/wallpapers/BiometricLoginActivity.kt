package com.example.wallpapers

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricLoginActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    companion object {
        private const val KEY_NAME = "session_token_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val SHARED_PREFS_NAME = "secure_prefs"
        private const val ENCRYPTED_TOKEN_KEY = "encrypted_token"
        private const val IV_KEY = "encryption_iv"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_login)

        // Simulate receiving and encrypting a token during a first-time login
        simulateFirstTimeLogin()

        setupBiometricPrompt()

        findViewById<Button>(R.id.btn_unlock).setOnClickListener {
            initiateBiometricFlow()
        }
    }

    /**
     * STAGE 1: The Gatekeeper (UX)
     * Check if the device is capable of biometrics before trying any crypto.
     */
    private fun initiateBiometricFlow() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> startBiometricAuth()

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToast("No fingerprints enrolled.")
                openBiometricSettings()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> showToast("No biometric hardware found.")
            else -> showToast("Biometric authentication currently unavailable.")
        }
    }

    /**
     * STAGE 2: The Vault Unlocker (Crypto)
     * Prepare the "Locked" Cipher and trigger the prompt.
     */
    private fun startBiometricAuth() {
        try {
            val iv = getSavedIv() ?: throw Exception("Session expired or not found.")

            // Initialize Cipher in DECRYPT_MODE using the stored IV
            val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_NAME, null) as SecretKey

            cipher.init(Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.IvParameterSpec(iv))

            // Pass the locked cipher to the prompt
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        } catch (e: Exception) {
            Log.e("Auth", "Setup failed", e)
            showToast("Security error: ${e.message}")
        }
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    // STAGE 3: The Release
                    // The hardware has unlocked the key; we can now decrypt.
                    val cipher = result.cryptoObject?.cipher
                    val encryptedToken = getSavedEncryptedToken()

                    if (cipher != null && encryptedToken != null) {
                        try {
                            val plainTokenBytes = cipher.doFinal(encryptedToken)
                            val sessionToken = String(plainTokenBytes, Charsets.UTF_8)

                            Log.d("Auth", "Decrypted Token: $sessionToken")
                            showToast("Access Granted!")
                            navigateToWallpaperList()
                        } catch (e: Exception) {
                            showToast("Decryption failed. Key might have been invalidated.")
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showToast("Authentication error: $errString")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Access")
            .setSubtitle("Unlock your session to view wallpapers")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    // --- HELPER METHODS FOR STORAGE & MOCK DATA ---

    private fun simulateFirstTimeLogin() {
        val prefs = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        if (prefs.contains(ENCRYPTED_TOKEN_KEY)) return

        // Generate hardware-bound key
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(KeyGenParameterSpec.Builder(KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .setInvalidatedByBiometricEnrollment(true)
            .build())
        val key = keyGenerator.generateKey()

        // Encrypt dummy session token
        val cipher = Cipher.getInstance("${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal("session-token-or-something".toByteArray())

        // Save Encrypted Data + IV
        prefs.edit().apply {
            putString(ENCRYPTED_TOKEN_KEY, Base64.encodeToString(encrypted, Base64.DEFAULT))
            putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
            apply()
        }
    }

    private fun getSavedEncryptedToken(): ByteArray? {
        val b64 = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE).getString(ENCRYPTED_TOKEN_KEY, null)
        return b64?.let { Base64.decode(it, Base64.DEFAULT) }
    }

    private fun getSavedIv(): ByteArray? {
        val b64 = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE).getString(IV_KEY, null)
        return b64?.let { Base64.decode(it, Base64.DEFAULT) }
    }

    private fun openBiometricSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        startActivity(intent)
    }

    private fun navigateToWallpaperList() {
        startActivity(Intent(this, WallpaperListActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}