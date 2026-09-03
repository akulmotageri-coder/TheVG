package com.example.vga

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// COLORS (VGA's existing palette, matches AudioSeparationScreen)
// ============================================================

private val WarmBackground = Color(0xFFF9F8F6)
private val Berry = Color(0xFF9E2A4B)
private val Slate = Color(0xFF2D3142)
private val Muted = Color(0xFF6C727F)
private val SoftMuted = Color(0xFF8D99AE)
private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFFF1ECE7)
private val Rose = Color(0xFFFFE5EC)
private val Blue = Color(0xFFE3F2FD)
private val BlueText = Color(0xFF1E6091)

/**
 * VGA's central module-selection screen: the app's first/launch screen.
 * Selects between the existing Audio Processing flow (AudioSeparationEntry)
 * and the existing Keyboard Processing flow (KeyboardBehaviorScreen) - it
 * contains no processing logic of its own.
 */
@Composable
fun MainDashboardScreen(
    onSelectAudioProcessing: () -> Unit,
    onSelectKeyboardProcessing: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "VGA",
            color = Slate,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = "Choose what you'd like to analyze.",
            color = Muted,
            fontSize = 15.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        DashboardCard(
            icon = "🎙",
            title = "Audio Processing",
            description = "Voice separation & speech analysis",
            iconBackground = Rose,
            iconColor = Berry,
            onClick = onSelectAudioProcessing
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        DashboardCard(
            icon = "⌨",
            title = "Keyboard Processing",
            description = "Keyboard behavior analysis",
            iconBackground = Blue,
            iconColor = BlueText,
            onClick = onSelectKeyboardProcessing
        )
    }
}

@Composable
private fun DashboardCard(
    icon: String,
    title: String,
    description: String,
    iconBackground: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                White,
                RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        iconBackground,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = icon,
                    fontSize = 22.sp
                )
            }

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = Slate,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = description,
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "→",
                color = iconColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
