package com.example.wallpapers

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class BiometricLoginActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_login)

        executor = ContextCompat.getMainExecutor(this)
        
        setupBiometricPrompt()

        findViewById<Button>(R.id.btn_unlock).setOnClickListener {
            initiateBiometricCheck()
        }
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    showToast("Authentication error: $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToWallpaperList()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showToast("Authentication failed. Please try again.")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Log in using your biometric credential to access wallpapers")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()
    }

    private fun initiateBiometricCheck() {
        val biometricManager = BiometricManager.from(this)
        
        when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                showToast("No biometric hardware detected on this device.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                showToast("Biometric hardware is currently unavailable.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                showToast("No biometric credentials enrolled. Please set them up in settings.")
                openBiometricSettings()
            }
            else -> {
                showToast("Biometric authentication is not available.")
            }
        }
    }

    private fun navigateToWallpaperList() {
        val intent = Intent(this, WallpaperListActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openBiometricSettings() {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_WEAK)
        }
        startActivity(enrollIntent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}