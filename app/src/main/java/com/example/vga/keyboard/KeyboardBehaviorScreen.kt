package com.example.vga.keyboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vernacularguardian.keyboardprocessing.BehavioralDailySummary
import com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingApi
import com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingModule
import com.example.vga.ui.DotGridBackground
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

// ============================================================
// COLORS (matches VGA's existing palette)
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
private val Mint = Color(0xFFE6F4EA)
private val MintText = Color(0xFF137333)
private val Butter = Color(0xFFFFF3CD)
private val ButterText = Color(0xFF854D0E)

/**
 * VGA-side integration screen for the `:keyboard-processing` module.
 *
 * Depends only on the module's public surface (KeyboardProcessingApi /
 * KeyboardProcessingModule / BehavioralDailySummary) - never on any of its
 * internal classes - mirroring the module's own documented integration
 * contract. The state, flow collection, and lifecycle re-check logic here are
 * unchanged from the previous implementation; only presentation was revised.
 */
@Composable
fun KeyboardBehaviorScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val api: KeyboardProcessingApi =
        remember { KeyboardProcessingModule.getInstance(context) }

    val summaryFlow = remember(api) { api.observeDailyBehavioralSummary() }
    val summary by summaryFlow.collectAsStateWithLifecycle(initialValue = null)

    var isAccessibilityAuthorized by remember {
        mutableStateOf(api.isPassiveCaptureAuthorized())
    }

    var needsOwnerConfirmation by remember {
        mutableStateOf(api.needsOwnerConfirmationForCurrentSession())
    }

    var passiveCaptureStatus by remember {
        mutableStateOf("Not changed this session")
    }

    DisposableEffect(lifecycleOwner) {
        // On a cold start, the Activity can already reach RESUMED before this
        // observer is attached, so the very first ON_RESUME event is missed
        // and the initial `remember` value above (read at first composition)
        // would otherwise never get corrected. Re-check once immediately on
        // attach, in addition to every later ON_RESUME.
        isAccessibilityAuthorized = api.isPassiveCaptureAuthorized()
        needsOwnerConfirmation = api.needsOwnerConfirmationForCurrentSession()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityAuthorized = api.isPassiveCaptureAuthorized()
                needsOwnerConfirmation = api.needsOwnerConfirmationForCurrentSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {

        DotGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 30.dp
                )
        ) {

            TopNav(
                title = "Keyboard Behavior",
                subtitle = "Typing-pattern analysis",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(24.dp))

            StatusCard(
                isAccessibilityAuthorized = isAccessibilityAuthorized,
                passiveCaptureStatus = passiveCaptureStatus,
                needsOwnerConfirmation = needsOwnerConfirmation
            )

            Spacer(modifier = Modifier.height(16.dp))

            SummaryCard(summary)

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(text = "PASSIVE CAPTURE")

            Spacer(modifier = Modifier.height(10.dp))

            ActionRow(
                icon = "▶",
                label = "Start passive capture",
                background = Mint,
                textColor = MintText,
                onClick = {
                    api.startPassiveCapture()
                    passiveCaptureStatus = "Scheduled"
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionRow(
                icon = "■",
                label = "Stop passive capture",
                background = Rose,
                textColor = Berry,
                onClick = {
                    api.stopPassiveCapture()
                    passiveCaptureStatus = "Stopped"
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionRow(
                icon = "↻",
                label = "Force aggregate now",
                background = Blue,
                textColor = BlueText,
                onClick = { coroutineScope.launch { api.forceAggregateNow() } }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel(text = "OWNER CONFIRMATION")

            Spacer(modifier = Modifier.height(10.dp))

            ActionRow(
                icon = "✓",
                label = "Confirm owner (Yes)",
                background = Mint,
                textColor = MintText,
                onClick = {
                    api.confirmOwnerForCurrentSession()
                    needsOwnerConfirmation = api.needsOwnerConfirmationForCurrentSession()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ActionRow(
                icon = "✕",
                label = "Revoke owner (Not me)",
                background = Rose,
                textColor = Berry,
                onClick = {
                    api.revokeOwnerForCurrentSession()
                    needsOwnerConfirmation = api.needsOwnerConfirmationForCurrentSession()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            ActionRow(
                icon = "⚙",
                label = "Open Accessibility Settings",
                background = White,
                textColor = Slate,
                bordered = true,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }
}


// ============================================================
// TOP NAV
// ============================================================

@Composable
private fun TopNav(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(White)
                .border(
                    width = 1.dp,
                    color = Border,
                    shape = CircleShape
                )
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "‹",
                color = Slate,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {

            Text(
                text = title,
                color = Slate,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = SoftMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


// ============================================================
// SECTION LABEL
// ============================================================

@Composable
private fun SectionLabel(text: String) {

    Text(
        text = text,
        color = SoftMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.3.sp
    )
}


// ============================================================
// STATUS CARD
// ============================================================

@Composable
private fun StatusCard(
    isAccessibilityAuthorized: Boolean,
    passiveCaptureStatus: String,
    needsOwnerConfirmation: Boolean
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(24.dp))
            .border(1.dp, Border, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {

        Text(
            text = "Accessibility permission",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        val statusBackground = if (isAccessibilityAuthorized) Mint else Butter
        val statusColor = if (isAccessibilityAuthorized) MintText else ButterText
        val statusLabel = if (isAccessibilityAuthorized) "Granted" else "Not granted"

        Row(
            modifier = Modifier
                .background(statusBackground, RoundedCornerShape(50.dp))
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StatusInfoRow(label = "Passive capture", value = passiveCaptureStatus)

        Spacer(modifier = Modifier.height(6.dp))

        StatusInfoRow(
            label = "Owner confirmation needed",
            value = if (needsOwnerConfirmation) "Yes" else "No"
        )
    }
}

@Composable
private fun StatusInfoRow(label: String, value: String) {

    Row(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = label,
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


// ============================================================
// SUMMARY CARD
// ============================================================

@Composable
private fun SummaryCard(summary: BehavioralDailySummary?) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(24.dp))
            .border(1.dp, Border, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {

        Text(
            text = "Today's summary",
            color = Slate,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (summary == null || summary.sessionCount == 0) {

            Spacer(modifier = Modifier.height(10.dp))
            EmptySummaryState()
            return@Column
        }

        Text(
            text = "Based on ${summary.sessionCount} completed session" +
                    if (summary.sessionCount == 1) "" else "s",
            color = Muted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        MetricTileRow(
            left = MetricData(
                label = "Typing speed",
                value = "%.0f".format(Locale.US, summary.avgTypingSpeedCpm),
                unit = "cpm",
                background = Rose,
                valueColor = Berry
            ),
            right = MetricData(
                label = "Backspace rate",
                value = "%.1f".format(Locale.US, summary.avgBackspaceRate * 100),
                unit = "%",
                background = Blue,
                valueColor = BlueText
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        MetricTileRow(
            left = MetricData(
                label = "Rhythm variability",
                value = "%.0f".format(Locale.US, summary.intervalStdDevMs),
                unit = "ms",
                background = Mint,
                valueColor = MintText
            ),
            right = MetricData(
                label = "Pause frequency",
                value = "%.1f".format(Locale.US, summary.pauseFrequencyPer100Chars),
                unit = "/100 chars",
                background = Butter,
                valueColor = ButterText
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiskScoreBar(score = summary.riskContributionScore)

        Spacer(modifier = Modifier.height(14.dp))

        ReliabilityBadge(isReliable = summary.isReliable)
    }
}

@Composable
private fun EmptySummaryState() {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(52.dp)
                .background(WarmBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "⌨",
                fontSize = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No behavioral data yet today",
            color = Slate,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Keep using your keyboard normally — insights\nwill appear here once a session completes.",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

private data class MetricData(
    val label: String,
    val value: String,
    val unit: String,
    val background: Color,
    val valueColor: Color
)

@Composable
private fun MetricTileRow(left: MetricData, right: MetricData) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        MetricTile(data = left, modifier = Modifier.weight(1f))
        MetricTile(data = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricTile(data: MetricData, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .background(data.background, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {

        Text(
            text = data.label,
            color = data.valueColor.copy(alpha = 0.75f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.Bottom) {

            Text(
                text = data.value,
                color = data.valueColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = data.unit,
                color = data.valueColor.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun RiskScoreBar(score: Double) {

    val clamped = score.coerceIn(0.0, 100.0)
    val trackColor = WarmBackground
    val fillColor = when {
        clamped < 35.0 -> MintText
        clamped < 65.0 -> ButterText
        else -> Berry
    }

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Risk contribution score",
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${clamped.roundToInt()} / 100",
                color = Slate,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(trackColor, RoundedCornerShape(50.dp))
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (clamped / 100.0).toFloat().coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(fillColor, RoundedCornerShape(50.dp))
            )
        }
    }
}

@Composable
private fun ReliabilityBadge(isReliable: Boolean) {

    val background = if (isReliable) Mint else Butter
    val color = if (isReliable) MintText else ButterText
    val label = if (isReliable) "Reliable" else "Limited data"

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {

        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}


// ============================================================
// ACTION ROW
// ============================================================

@Composable
private fun ActionRow(
    icon: String,
    label: String,
    background: Color,
    textColor: Color,
    bordered: Boolean = false,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(50.dp))
            .let {
                if (bordered) it.border(1.dp, Border, RoundedCornerShape(50.dp)) else it
            }
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = icon,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(9.dp))

        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
