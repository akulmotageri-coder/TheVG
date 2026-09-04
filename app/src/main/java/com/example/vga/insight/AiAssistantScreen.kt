package com.example.vga.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull


private val WarmBackground = Color(0xFFF9F8F6)
private val Berry = Color(0xFF9E2A4B)
private val Slate = Color(0xFF2D3142)
private val Muted = Color(0xFF6C727F)
private val SoftMuted = Color(0xFF8D99AE)
private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFFF1ECE7)
private val Rose = Color(0xFFFFE5EC)
private val Butter = Color(0xFFFFF3CD)
private val ButterText = Color(0xFF854D0E)


private data class ChatMessage(
    val fromUser: Boolean,
    val text: String
)


/**
 * Conversational interface over the app's own measurements.
 *
 * The assistant explains stored values; it never produces them. Replies pass
 * through [DiagnosisGuard] inside [AiAssistant.ask] before reaching this UI.
 */
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var keyboard by remember { mutableStateOf<KeyboardSnapshot?>(null) }

    LaunchedEffect(Unit) {
        keyboard = readKeyboardSnapshot(context)
        if (messages.isEmpty()) {
            messages += ChatMessage(
                fromUser = false,
                text = "Hello. I can explain the measurements this app has " +
                    "collected — your language patterns, typing behaviour and " +
                    "cognitive-test results. Ask me anything about them."
            )
        }
    }

    fun send(question: String) {
        if (question.isBlank() || isBusy) return

        messages += ChatMessage(fromUser = true, text = question)
        input = ""
        isBusy = true

        scope.launch {
            val reply = AiAssistant.ask(context, question, keyboard)

            reply.fold(
                onSuccess = { messages += ChatMessage(false, it) },
                onFailure = { error ->
                    messages += ChatMessage(
                        fromUser = false,
                        text = "Unable to reach the AI server.\n\n" +
                            "${error.javaClass.simpleName}: ${error.message}\n\n" +
                            "Check the URL in AI Server Settings and make sure the " +
                            "server is running and reachable."
                    )
                }
            )

            isBusy = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {

            // ---------- top bar ----------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                CircleButton(glyph = "‹", onClick = onBack)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Assistant",
                        color = Slate,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explains your measurements",
                        color = SoftMuted,
                        fontSize = 11.sp
                    )
                }

                CircleButton(glyph = "⚙", onClick = onOpenSettings)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- messages ----------

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                messages.forEach { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isBusy) {
                    Box(
                        modifier = Modifier
                            .background(White, RoundedCornerShape(18.dp))
                            .border(1.dp, Border, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text(text = "Thinking…", color = Muted, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Butter, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "This assistant explains measured patterns only. " +
                            "It cannot provide a medical diagnosis.",
                        color = ButterText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // ---------- suggestions ----------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiAssistant.suggestedQuestions.forEach { question ->
                    Box(
                        modifier = Modifier
                            .background(White, RoundedCornerShape(50.dp))
                            .border(1.dp, Border, RoundedCornerShape(50.dp))
                            .clickable(enabled = !isBusy) { send(question) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = question, color = Muted, fontSize = 11.sp)
                    }
                }
            }

            // ---------- composer ----------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(White, RoundedCornerShape(50.dp))
                        .border(1.dp, Border, RoundedCornerShape(50.dp))
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = "Ask about your results…",
                            color = SoftMuted,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = Slate, fontSize = 13.sp),
                        cursorBrush = SolidColor(Berry),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isBusy) Rose else Berry)
                        .clickable(enabled = !isBusy) { send(input) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        color = if (isBusy) Berry else White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun MessageBubble(message: ChatMessage) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(
                    if (message.fromUser) Berry else White,
                    RoundedCornerShape(18.dp)
                )
                .then(
                    if (message.fromUser) Modifier
                    else Modifier.border(1.dp, Border, RoundedCornerShape(18.dp))
                )
                .padding(14.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.fromUser) White else Slate,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}


@Composable
private fun CircleButton(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(White)
            .border(1.dp, Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, color = Slate, fontSize = 18.sp)
    }
}


internal suspend fun readKeyboardSnapshot(
    context: android.content.Context
): KeyboardSnapshot? = runCatching {

    val api = KeyboardProcessingModule.getInstance(context)

    val summary = withTimeoutOrNull(3_000) {
        api.observeDailyBehavioralSummary().first()
    } ?: return null

    if (summary.sessionCount == 0) return null

    KeyboardSnapshot(
        avgTypingSpeedCpm = summary.avgTypingSpeedCpm,
        avgBackspaceRate = summary.avgBackspaceRate,
        pauseFrequencyPer100Chars = summary.pauseFrequencyPer100Chars,
        sessionCount = summary.sessionCount,
        riskContributionScore = summary.riskContributionScore
    )
}.getOrNull()
