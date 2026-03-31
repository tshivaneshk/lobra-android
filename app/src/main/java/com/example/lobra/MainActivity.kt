package com.example.lobra

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.lobra.theme.LobraTheme
import com.example.lobra.ui.LobraApp
import com.example.lobra.viewmodel.LobraViewModel
import com.example.lobra.viewmodel.LobraViewModelFactory

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            val shortcut = android.content.pm.ShortcutInfo.Builder(this, "add_reminder")
                .setShortLabel("New Reminder")
                .setLongLabel("Quickly add a new reminder")
                .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(android.content.Intent(this, MainActivity::class.java).apply {
                    action = "ADD_REMINDER_SHORTCUT"
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                .build()
            java.lang.Thread {
                shortcutManager?.dynamicShortcuts = listOf(shortcut)
            }.start()
        }
        
        val factory = LobraViewModelFactory(applicationContext)
        val viewModel = ViewModelProvider(this, factory)[LobraViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when(themeMode) {
                "Dark Mode" -> true
                "Light Mode" -> false
                else -> systemDark
            }
            val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
            var isUnlocked by remember { mutableStateOf(!isAppLockEnabled) }
            val context = this@MainActivity

            LaunchedEffect(isAppLockEnabled) {
                if (isAppLockEnabled && !isUnlocked) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val biometricPrompt = BiometricPrompt(context, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isUnlocked = true
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE || 
                                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || 
                                    errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                                    android.widget.Toast.makeText(context, "No secure lock screen set up! Bypassing App Lock.", android.widget.Toast.LENGTH_LONG).show()
                                    isUnlocked = true 
                                }
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Lobra Locked")
                        .setSubtitle("Authenticate to access your reminders")
                        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                }
            }

            LobraTheme(darkTheme = isDark) {
                if (!isAppLockEnabled || isUnlocked) {
                    LobraApp(viewModel)
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Lobra is Locked", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                val executor = ContextCompat.getMainExecutor(context)
                                val biometricPrompt = BiometricPrompt(context, executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            isUnlocked = true
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            super.onAuthenticationError(errorCode, errString)
                                            if (errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE || 
                                                errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || 
                                                errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                                                errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                                                android.widget.Toast.makeText(context, "No secure lock screen set up! Bypassing App Lock.", android.widget.Toast.LENGTH_LONG).show()
                                                isUnlocked = true 
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed: $errString", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    })
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Lobra Locked")
                                    .setSubtitle("Authenticate to access your reminders")
                                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                                    .build()
                                biometricPrompt.authenticate(promptInfo)
                            }) {
                                Text("Unlock")
                            }
                        }
                    }
                }
            }
        }
    }
}