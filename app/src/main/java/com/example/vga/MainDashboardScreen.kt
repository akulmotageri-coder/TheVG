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
import com.example.vga.ui.DotGridBackground

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {

        DotGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Text(
                text = "VGA",
                color = Berry,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = "What would you like\nto analyze?",
                color = Slate,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Choose a processing module to get started.",
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )

            Spacer(
                modifier = Modifier.height(34.dp)
            )

            ModuleCard(
                icon = "🎙",
                title = "Audio Processing",
                description = "Voice separation & speech analysis",
                tag = "Speech",
                iconBackground = Rose,
                iconColor = Berry,
                tagBackground = Rose,
                tagColor = Berry,
                onClick = onSelectAudioProcessing
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            ModuleCard(
                icon = "⌨",
                title = "Keyboard Processing",
                description = "Typing behavior & cognitive signals",
                tag = "Behavior",
                iconBackground = Blue,
                iconColor = BlueText,
                tagBackground = Blue,
                tagColor = BlueText,
                onClick = onSelectKeyboardProcessing
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun ModuleCard(
    icon: String,
    title: String,
    description: String,
    tag: String,
    iconBackground: Color,
    iconColor: Color,
    tagBackground: Color,
    tagColor: Color,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                White,
                RoundedCornerShape(26.dp)
            )
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(26.dp)
            )
            .clickable { onClick() }
            .padding(22.dp)
    ) {

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    iconBackground,
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = icon,
                fontSize = 26.sp
            )
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = title,
                color = Slate,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        WarmBackground,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "→",
                    color = iconColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = description,
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Box(
            modifier = Modifier
                .background(
                    tagBackground,
                    RoundedCornerShape(50.dp)
                )
                .padding(
                    horizontal = 13.dp,
                    vertical = 6.dp
                )
        ) {

            Text(
                text = tag,
                color = tagColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp
            )
        }
    }
}
