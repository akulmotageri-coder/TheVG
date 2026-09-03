package com.example.vga.keyboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.launch

// ============================================================
// COLORS (matches AudioSeparationScreen's palette)
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

/**
 * VGA-side integration screen for the `:keyboard-processing` module.
 *
 * Depends only on the module's public surface (KeyboardProcessingApi /
 * KeyboardProcessingModule / BehavioralDailySummary) - never on any of its
 * internal classes - mirroring the module's own documented integration
 * contract. The logic here (state, flow collection, lifecycle re-check) is
 * carried over unchanged from the module's own known-good demo screen.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 30.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "‹",
                color = Slate,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {

                Text(
                    text = "Keyboard Behavior",
                    color = Slate,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Typing-pattern analysis",
                    color = SoftMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StatusCard(
            isAccessibilityAuthorized = isAccessibilityAuthorized,
            passiveCaptureStatus = passiveCaptureStatus,
            needsOwnerConfirmation = needsOwnerConfirmation
        )

        Spacer(modifier = Modifier.height(16.dp))

        SummaryCard(summary)

        Spacer(modifier = Modifier.height(16.dp))

        ActionRow(
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
            label = "Force aggregate now",
            background = Blue,
            textColor = BlueText,
            onClick = { coroutineScope.launch { api.forceAggregateNow() } }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionRow(
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
            label = "Revoke owner (Not me)",
            background = Rose,
            textColor = Berry,
            onClick = {
                api.revokeOwnerForCurrentSession()
                needsOwnerConfirmation = api.needsOwnerConfirmationForCurrentSession()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionRow(
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

@Composable
private fun StatusCard(
    isAccessibilityAuthorized: Boolean,
    passiveCaptureStatus: String,
    needsOwnerConfirmation: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(17.dp)
    ) {
        Text(
            text = "Accessibility permission: " +
                    if (isAccessibilityAuthorized) "Granted" else "Not granted",
            color = Slate,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Passive capture: $passiveCaptureStatus",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Owner confirmation needed: " +
                    if (needsOwnerConfirmation) "Yes" else "No",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SummaryCard(summary: BehavioralDailySummary?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(17.dp)
    ) {
        Text(
            text = "Today's summary",
            color = Slate,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (summary == null) {
            Text(text = "Loading...", color = Muted, fontSize = 13.sp)
        } else {
            SummaryLine("Avg typing speed (cpm)", summary.avgTypingSpeedCpm.toString())
            SummaryLine("Avg backspace rate", summary.avgBackspaceRate.toString())
            SummaryLine("Interval std dev (ms)", summary.intervalStdDevMs.toString())
            SummaryLine("Pause freq /100 chars", summary.pauseFrequencyPer100Chars.toString())
            SummaryLine("Session count", summary.sessionCount.toString())
            SummaryLine("Risk contribution score", summary.riskContributionScore.toString())
            SummaryLine("Reliable", summary.isReliable.toString())
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun ActionRow(
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
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
