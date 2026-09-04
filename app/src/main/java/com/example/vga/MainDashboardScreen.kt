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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vga.insight.TimelineBuilder
import com.example.vga.insight.TimelineCategory
import com.example.vga.insight.TimelineEvent
import com.example.vga.insight.formatTimestamp
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
private val Butter = Color(0xFFFFF3CD)
private val ButterText = Color(0xFF854D0E)
private val Mint = Color(0xFFE6F4EA)
private val MintText = Color(0xFF137333)

/**
 * VGA's central module-selection screen: the app's first/launch screen.
 * Selects between the existing Audio Processing flow (AudioSeparationEntry)
 * and the existing Keyboard Processing flow (KeyboardBehaviorScreen) - it
 * contains no processing logic of its own.
 */
@Composable
fun MainDashboardScreen(
    onSelectAudioProcessing: () -> Unit,
    onSelectKeyboardProcessing: () -> Unit,
    onSelectCognitiveTests: () -> Unit,
    onSelectLinguisticInsights: () -> Unit
) {

    val context = LocalContext.current

    // Real stored events only - the timeline shows nothing until actual
    // recordings, transcripts, analyses or tests exist on the device.
    val recentEvents = remember { TimelineBuilder.build(context, keyboard = null, limit = 4) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {

        DotGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Applied before verticalScroll so content scrolls *below* the
                // status bar rather than underneath it (the app is edge-to-edge).
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {

            Spacer(
                modifier = Modifier.height(14.dp)
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
                modifier = Modifier.height(16.dp)
            )

            ModuleCard(
                icon = "🧠",
                title = "Cognitive Tests",
                description = "Attention, memory & processing speed",
                tag = "Cognition",
                iconBackground = Butter,
                iconColor = ButterText,
                tagBackground = Butter,
                tagColor = ButterText,
                onClick = onSelectCognitiveTests
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            ModuleCard(
                icon = "💬",
                title = "Linguistic Insights",
                description = "Transcript analysis with on-device AI",
                tag = "Language",
                iconBackground = Mint,
                iconColor = MintText,
                tagBackground = Mint,
                tagColor = MintText,
                onClick = onSelectLinguisticInsights
            )

            // ====================================================
            // TIMELINE
            //
            // A completed transcript analysis appears here as an
            // event. Hidden entirely until real results exist.
            // ====================================================

            if (recentEvents.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "RECENT ACTIVITY",
                    color = SoftMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.3.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                recentEvents.forEachIndexed { index, event ->

                    TimelineRow(
                        event = event,
                        isLast = index == recentEvents.lastIndex,
                        onClick = onSelectLinguisticInsights
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun TimelineRow(
    event: TimelineEvent,
    isLast: Boolean,
    onClick: () -> Unit
) {

    val tint = when (event.category) {
        TimelineCategory.SPEECH -> Rose
        TimelineCategory.LANGUAGE -> Mint
        TimelineCategory.PATTERN -> Butter
        TimelineCategory.ACOUSTIC -> Blue
        TimelineCategory.KEYBOARD -> Blue
        TimelineCategory.COGNITIVE -> Butter
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {

        // rail
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(tint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = event.category.icon, fontSize = 11.sp)
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(Border)
                )
            }
        }

        Spacer(
            modifier = Modifier.width(11.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = event.title,
                    color = Slate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                event.metric?.let {
                    Text(
                        text = it,
                        color = SoftMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = "${event.category.label} · ${formatTimestamp(event.timestampMs)}",
                color = SoftMuted,
                fontSize = 10.sp,
                maxLines = 1
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
