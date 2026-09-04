package com.example.vga.insight

import android.content.Context
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
import com.example.vga.audioseparation.processing.AudioDecoder
import com.example.vga.dementia.linguistic.IndicWhisperTranscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ============================================================
// COLORS (VGA palette)
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
 * Linguistic insight module.
 *
 * Reads the real outputs of the existing pipeline (extracted-voice WAVs from
 * CallProcessingWorker), transcribes them with the existing IndicWhisper
 * transcriber, and sends the transcript to the local AI server for
 * pattern-based analysis. Nothing in the audio pipeline or its UI is modified.
 */
@Composable
fun CognitiveInsightsScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var serverState by remember { mutableStateOf("Not checked") }
    var serverOk by remember { mutableStateOf<Boolean?>(null) }

    var insights by remember { mutableStateOf(InsightStore.getAll(context)) }
    var recordings by remember { mutableStateOf(loadExtractedVoiceFiles(context)) }
    var selected by remember { mutableStateOf<LinguisticInsight?>(null) }

    val baseUrl = remember { AiServerConfig.baseUrl(context) }
    val modelName = remember { AiServerConfig.model(context) }


    // ========================================================
    // INSIGHT DETAIL
    // ========================================================

    selected?.let { insight ->
        InsightDetailScreen(
            insight = insight,
            onBack = { selected = null }
        )
        return
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
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 30.dp
                )
        ) {


            // ================================================
            // TOP NAV
            // ================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(1.dp, Border, CircleShape)
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
                        text = "Linguistic Insights",
                        color = Slate,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Transcript analysis · on-device AI",
                        color = SoftMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


            // ================================================
            // SERVER STATUS
            // ================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .border(1.dp, Border, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {

                Text(
                    text = "Local AI server",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val chipBg = when (serverOk) {
                    true -> Mint
                    false -> Butter
                    null -> Color(0xFFF0F0F2)
                }
                val chipFg = when (serverOk) {
                    true -> MintText
                    false -> ButterText
                    null -> Muted
                }

                Row(
                    modifier = Modifier
                        .background(chipBg, RoundedCornerShape(50.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(chipFg, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = serverState,
                        color = chipFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = baseUrl, color = Slate, fontSize = 12.sp)
                Text(text = "Model: $modelName", color = Muted, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(14.dp))

                ActionPill(
                    label = "Test connection",
                    background = Blue,
                    textColor = BlueText,
                    enabled = !isBusy,
                    onClick = {
                        scope.launch {
                            isBusy = true
                            serverState = "Checking…"
                            serverOk = null

                            val result = LocalAiClient(baseUrl).listModels()

                            result.fold(
                                onSuccess = { models ->
                                    serverOk = true
                                    serverState =
                                        if (models.isEmpty()) "Reachable (no models listed)"
                                        else "Reachable · ${models.size} model(s)"
                                    status = "Server models: ${models.joinToString()}"
                                },
                                onFailure = { error ->
                                    serverOk = false
                                    serverState = "Unreachable"
                                    status =
                                        "Cannot reach $baseUrl — " +
                                            "${error.javaClass.simpleName}: ${error.message}"
                                }
                            )

                            isBusy = false
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            // ================================================
            // STATUS MESSAGE
            // ================================================

            status?.let { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Butter, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = message,
                        color = ButterText,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }


            // ================================================
            // EXTRACTED RECORDINGS
            // ================================================

            SectionLabel("EXTRACTED VOICE RECORDINGS")

            Spacer(modifier = Modifier.height(10.dp))

            if (recordings.isEmpty()) {
                EmptyCard(
                    title = "No processed recordings yet",
                    body = "Share a call recording into VGA — once Audio " +
                        "Processing extracts your voice, it appears here for analysis."
                )
            } else {
                recordings.forEach { file ->
                    RecordingRow(
                        file = file,
                        enabled = !isBusy,
                        onAnalyze = {
                            scope.launch {
                                isBusy = true
                                status = "Transcribing ${file.name}…"

                                val outcome = runCatching {

                                    // Real transcription via the existing
                                    // IndicWhisper pipeline - no stubbing.
                                    val text = withContext(Dispatchers.IO) {
                                        val audio = AudioDecoder.decodeToMonoFloat(file)
                                        IndicWhisperTranscriber(context).transcribe(
                                            samples = audio.samples,
                                            sampleRate = audio.sampleRate
                                        )
                                    }

                                    if (text.isBlank()) {
                                        error("Transcription produced no text for ${file.name}")
                                    }

                                    val record = TranscriptRecord(
                                        timestampMs = System.currentTimeMillis(),
                                        sourceFileName = file.name,
                                        text = text
                                    )

                                    TranscriptStore.save(context, record)

                                    status =
                                        "Transcribed ${record.wordCount} words. " +
                                            "Sending to local AI server…"

                                    LinguisticAnalyzer.analyze(context, record).getOrThrow()
                                }

                                outcome.fold(
                                    onSuccess = { insight ->
                                        insights = InsightStore.getAll(context)
                                        status =
                                            "Analysis complete — ${insight.patterns.size} " +
                                                "pattern(s) reported."
                                    },
                                    onFailure = { error ->
                                        status =
                                            "Analysis failed: " +
                                                "${error.javaClass.simpleName}: ${error.message}"
                                    }
                                )

                                isBusy = false
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


            // ================================================
            // TIMELINE
            // ================================================

            SectionLabel("INSIGHT TIMELINE")

            Spacer(modifier = Modifier.height(10.dp))

            if (insights.isEmpty()) {
                EmptyCard(
                    title = "No analyses yet",
                    body = "Analyse a recording above and the result will appear " +
                        "here and on the Main Dashboard."
                )
            } else {
                insights.forEach { insight ->
                    InsightRow(
                        insight = insight,
                        onClick = { selected = insight }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = DiagnosisGuard.SCREENING_NOTICE,
                color = SoftMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}


// ============================================================
// INSIGHT DETAIL
// ============================================================

@Composable
private fun InsightDetailScreen(
    insight: LinguisticInsight,
    onBack: () -> Unit
) {

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
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp)
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
                        .border(1.dp, Border, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Slate, fontSize = 26.sp, fontWeight = FontWeight.Light)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Observed Patterns",
                        color = Slate,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTimestamp(insight.timestampMs),
                        color = SoftMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confidence + source
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .border(1.dp, Border, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                InfoLine("Source", insight.sourceFileName)
                Spacer(modifier = Modifier.height(6.dp))
                InfoLine("Transcript length", "${insight.transcriptWordCount} words")
                Spacer(modifier = Modifier.height(6.dp))
                InfoLine("Pattern strength", insight.confidence.replaceFirstChar { it.uppercase() })
                Spacer(modifier = Modifier.height(6.dp))
                InfoLine("Analysed by", insight.modelName)

                if (insight.guardApplied) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Note: diagnostic phrasing from the model was " +
                            "automatically rewritten to non-diagnostic wording.",
                        color = ButterText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel("DETECTED PATTERNS")
            Spacer(modifier = Modifier.height(10.dp))

            if (insight.patterns.isEmpty()) {
                EmptyCard(
                    title = "No specific pattern reported",
                    body = "The model did not report a distinct linguistic pattern " +
                        "for this transcript."
                )
            } else {
                insight.patterns.forEach { pattern ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Rose, RoundedCornerShape(18.dp))
                            .padding(15.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pattern.label,
                                color = Berry,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(White.copy(alpha = 0.7f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = pattern.strength,
                                    color = Berry,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (pattern.evidence.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pattern.evidence,
                                color = Slate,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SectionLabel("CHANGE FROM BASELINE")
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue, RoundedCornerShape(18.dp))
                    .padding(15.dp)
            ) {
                Text(
                    text = insight.baselineComparison,
                    color = BlueText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("SUPPORTING METRICS")
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .border(1.dp, Border, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                insight.supportingMetrics.forEachIndexed { index, metric ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = metric.label, color = Slate, fontSize = 12.sp)
                            Text(text = metric.source, color = SoftMuted, fontSize = 10.sp)
                        }
                        Text(
                            text = metric.value,
                            color = Slate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("EXPLANATION")
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .border(1.dp, Border, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = insight.explanation,
                    color = Slate,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}


// ============================================================
// PIECES
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

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Text(text = title, color = Slate, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = body, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ActionPill(
    label: String,
    background: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (enabled) background else background.copy(alpha = 0.5f),
                RoundedCornerShape(50.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 13.dp),
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

@Composable
private fun RecordingRow(
    file: File,
    enabled: Boolean,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(20.dp))
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = file.name,
            color = Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "${file.length() / 1024} KB · ${formatTimestamp(file.lastModified())}",
            color = SoftMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActionPill(
            label = "Transcribe & Analyse",
            background = Rose,
            textColor = Berry,
            enabled = enabled,
            onClick = onAnalyze
        )
    }
}

@Composable
private fun InsightRow(
    insight: LinguisticInsight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(20.dp))
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Berry, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.headlinePattern,
                color = Slate,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${formatTimestamp(insight.timestampMs)} · " +
                    "${insight.patterns.size} pattern(s) · ${insight.confidence}",
                color = SoftMuted,
                fontSize = 11.sp
            )
        }
        Text(text = "→", color = Berry, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}


// ============================================================
// HELPERS
// ============================================================

internal fun formatTimestamp(timestampMs: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestampMs))

private fun loadExtractedVoiceFiles(context: Context): List<File> {
    val directory = File(context.filesDir, "extracted_voice")
    if (!directory.exists()) return emptyList()
    return directory.listFiles()
        ?.filter { it.isFile && it.extension.equals("wav", ignoreCase = true) }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}
