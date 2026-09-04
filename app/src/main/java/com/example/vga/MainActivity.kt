package com.example.vga

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.vga.audioseparation.AudioSeparationEntry
import com.example.vga.cognitive.CognitiveTestsEntry
import com.example.vga.insight.CognitiveInsightsScreen
import com.example.vga.keyboard.KeyboardBehaviorScreen
import com.example.vga.ui.theme.VGATheme

private enum class VgaTopLevelScreen {
    DASHBOARD,
    AUDIO_PROCESSING,
    KEYBOARD_PROCESSING,
    COGNITIVE_TESTS,
    LINGUISTIC_INSIGHTS
}

class MainActivity : ComponentActivity() {

    // Required for the integrated keyboard-processing module's owner-confirmation
    // notification (OwnerConfirmationNotifier) to ever be able to post on API 33+:
    // POST_NOTIFICATIONS defaults to denied and stays that way unless requested.
    // This only shows the standard system permission dialog; it does not grant
    // anything itself, and OwnerConfirmationNotifier already fails closed on a
    // missing grant either way.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            VGATheme {
                var currentScreen by remember {
                    mutableStateOf(VgaTopLevelScreen.DASHBOARD)
                }

                when (currentScreen) {

                    VgaTopLevelScreen.DASHBOARD -> {
                        MainDashboardScreen(
                            onSelectAudioProcessing = {
                                currentScreen = VgaTopLevelScreen.AUDIO_PROCESSING
                            },
                            onSelectKeyboardProcessing = {
                                currentScreen = VgaTopLevelScreen.KEYBOARD_PROCESSING
                            },
                            onSelectCognitiveTests = {
                                currentScreen = VgaTopLevelScreen.COGNITIVE_TESTS
                            },
                            onSelectLinguisticInsights = {
                                currentScreen = VgaTopLevelScreen.LINGUISTIC_INSIGHTS
                            }
                        )
                    }

                    VgaTopLevelScreen.AUDIO_PROCESSING -> {
                        AudioSeparationEntry(
                            onBack = {
                                currentScreen = VgaTopLevelScreen.DASHBOARD
                            }
                        )
                    }

                    VgaTopLevelScreen.KEYBOARD_PROCESSING -> {
                        KeyboardBehaviorScreen(
                            onBack = {
                                currentScreen = VgaTopLevelScreen.DASHBOARD
                            }
                        )
                    }

                    VgaTopLevelScreen.COGNITIVE_TESTS -> {
                        CognitiveTestsEntry(
                            onBack = {
                                currentScreen = VgaTopLevelScreen.DASHBOARD
                            }
                        )
                    }

                    VgaTopLevelScreen.LINGUISTIC_INSIGHTS -> {
                        CognitiveInsightsScreen(
                            onBack = {
                                currentScreen = VgaTopLevelScreen.DASHBOARD
                            }
                        )
                    }
                }
            }
        }
    }
}