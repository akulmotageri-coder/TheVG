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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
 * Linguistic insight hub.
 *
 * Everything shown here is computed locally and deterministically from stored
 * data: transcript NLP, personal baseline, detected patterns, charts and the
 * timeline. The AI assistant is reachable from here but only ever explains
 * these values.
 */
@Composable
fun CognitiveInsightsScreen(
    onBack: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenSettings: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var keyboard by remember { mutableStateOf<KeyboardSnapshot?>(null) }
    var selectedMetric by remember { mutableStateOf(LanguageMetric.REPETITION_RATE) }
    var selectedInsight by remember { mutableStateOf<LinguisticInsight?>(null) }

    LaunchedEffect(refreshKey) {
        keyboard = readKeyboardSnapshot(context)
    }

    // ---- derived, deterministic view state ----

    val analysable = remember(refreshKey) {
        TranscriptStore.getAll(context)
            .sortedBy { it.timestampMs }
            .filterNot { TranscriptAnalytics.isTooShortToAnalyse(it.text) }
            .map { it to TranscriptAnalytics.compute(
                TranscriptAnalytics.stripPlaceholders(it.text)
            ) }
    }

    val currentMetrics = analysable.lastOrNull()?.second
    val priorMetrics = analysable.dropLast(1).map { it.second }
    val baseline = remember(refreshKey) { TranscriptAnalytics.baselineOf(priorMetrics) }

    val trendPoints = analysable.map {
        TrendPoint(it.first.timestampMs, selectedMetric.valueOf(it.second))
    }

    val insights = remember(refreshKey) { InsightStore.getAll(context) }
    val timeline = remember(refreshKey, keyboard) { TimelineBuilder.build(context, keyboard) }
    val modalities = remember(refreshKey, keyboard) {
        TimelineBuilder.modalityOverview(context, keyboard)
    }
    val recordings = remember(refreshKey) { loadCallRecordings(context) }

    selectedInsight?.let { insight ->
        InsightDetailScreen(insight = insight, onBack = { selectedInsight = null })
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
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp)
        ) {

            // ---------- top bar ----------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                CircleGlyph("‹", onBack)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Insights",
                        color = Slate,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Measured locally on this device",
                        color = SoftMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                CircleGlyph("⚙", onOpenSettings)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ---------- ask ai ----------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Berry, RoundedCornerShape(20.dp))
                    .clickable { onOpenAssistant() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💬", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ask the AI Assistant",
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explains these measurements in plain language",
                        color = White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
                Text(text = "→", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ---------- status ----------

            status?.let { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Butter, RoundedCornerShape(16.dp))
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

            // ---------- multimodal ----------

            SectionLabel("AVAILABLE SIGNALS")
            Spacer(modifier = Modifier.height(10.dp))
            MultimodalOverview(statuses = modalities)

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- trend ----------

            SectionLabel("TREND OVER TIME")
            Spacer(modifier = Modifier.height(10.dp))
            MetricSelector(
                selected = selectedMetric,
                onSelect = { selectedMetric = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            MetricTrendChart(metric = selectedMetric, points = trendPoints)

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- baseline ----------

            SectionLabel("PERSONAL BASELINE")
            Spacer(modifier = Modifier.height(10.dp))

            if (currentMetrics == null) {
                EmptyCard(
                    title = "No analysable transcript yet",
                    body = "Analyse a recording below to start measuring your " +
                        "language patterns."
                )
            } else {
                BaselineComparisonCard(
                    current = currentMetrics,
                    baseline = baseline,
                    historyCount = priorMetrics.size
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- timeline ----------

            SectionLabel("TIMELINE")
            Spacer(modifier = Modifier.height(10.dp))

            if (timeline.isEmpty()) {
                EmptyCard(
                    title = "No events yet",
                    body = "Processed calls, transcripts, detected patterns, " +
                        "keyboard sessions and cognitive tests appear here."
                )
            } else {
                TimelineList(
                    events = timeline,
                    onPatternClick = { selectedInsight = insights.firstOrNull() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- recordings ----------

            SectionLabel("ANALYSE A RECORDING")
            Spacer(modifier = Modifier.height(10.dp))

            if (recordings.isEmpty()) {
                EmptyCard(
                    title = "No call recordings",
                    body = "Share a call recording into VGA and it appears here " +
                        "for transcription and analysis."
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

                                    val text = withContext(Dispatchers.IO) {
                                        TranscriptionSource.transcribe(context, file)
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

                                    status = "Analysing ${record.wordCount} words locally…"

                                    LinguisticAnalyzer.analyze(context, record).getOrThrow()
                                }

                                outcome.fold(
                                    onSuccess = { insight ->
                                        status =
                                            "Analysis complete — " +
                                                "${insight.patterns.size} observation(s)."
                                        refreshKey++
                                    },
                                    onFailure = { error ->
                                        status = error.message
                                            ?: "${error.javaClass.simpleName} during analysis"
                                        refreshKey++
                                    }
                                )

                                isBusy = false
                            }
                        }
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
// TIMELINE
// ============================================================

@Composable
private fun TimelineList(
    events: List<TimelineEvent>,
    onPatternClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {

        events.forEachIndexed { index, event ->

            val accent = accentFor(event.category)

            Row(modifier = Modifier.fillMaxWidth()) {

                // rail
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(30.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(accent.second, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = event.category.icon, fontSize = 12.sp)
                    }

                    if (index != events.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(46.dp)
                                .background(Border)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (event.category == TimelineCategory.PATTERN)
                                Modifier.clickable { onPatternClick() }
                            else Modifier
                        )
                        .padding(bottom = if (index == events.lastIndex) 0.dp else 18.dp)
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = event.title,
                            color = Slate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        event.metric?.let {
                            Box(
                                modifier = Modifier
                                    .background(accent.second, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = it,
                                    color = accent.first,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = event.description,
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${event.category.label} · ${formatTimestamp(event.timestampMs)}",
                        color = SoftMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

private fun accentFor(category: TimelineCategory): Pair<Color, Color> = when (category) {
    TimelineCategory.SPEECH -> Berry to Rose
    TimelineCategory.LANGUAGE -> MintText to Mint
    TimelineCategory.PATTERN -> ButterText to Butter
    TimelineCategory.ACOUSTIC -> BlueText to Blue
    TimelineCategory.KEYBOARD -> BlueText to Blue
    TimelineCategory.COGNITIVE -> ButterText to Butter
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
                CircleGlyph("‹", onBack)
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
                InfoLine("Strength of change", insight.confidence)
                Spacer(modifier = Modifier.height(6.dp))
                InfoLine("Computed by", insight.modelName)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel("OBSERVATIONS")
            Spacer(modifier = Modifier.height(10.dp))

            if (insight.patterns.isEmpty()) {
                EmptyCard(
                    title = "No notable change measured",
                    body = "None of the measured language features moved enough " +
                        "from your baseline to be reported."
                )
            } else {
                insight.patterns.forEach { pattern ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Butter, RoundedCornerShape(18.dp))
                            .padding(15.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pattern.label,
                                color = ButterText,
                                fontSize = 13.sp,
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
                                    color = ButterText,
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

            SectionLabel("BASELINE")
            Spacer(modifier = Modifier.height(10.dp))

            Box(
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

            SectionLabel("MEASURED FEATURES")
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionLabel("SUMMARY")
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
private fun CircleGlyph(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(White)
            .border(1.dp, Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, color = Slate, fontSize = 20.sp)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) Rose else Rose.copy(alpha = 0.5f),
                    RoundedCornerShape(50.dp)
                )
                .clickable(enabled = enabled) { onAnalyze() }
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Transcribe & Analyse",
                color = Berry,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ============================================================
// HELPERS
// ============================================================

internal fun formatTimestamp(timestampMs: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestampMs))

/**
 * Transcription source: the ORIGINAL call recordings.
 *
 * The extracted-voice output is speaker-masked, which removes much of the
 * speech and gives the recogniser very little to work with, so the unmasked
 * recording produces a far more usable transcript.
 */
private fun loadCallRecordings(context: Context): List<File> {
    val directory = File(context.filesDir, "call_recordings")
    if (!directory.exists()) return emptyList()
    return directory.listFiles()
        ?.filter { it.isFile && it.extension.equals("wav", ignoreCase = true) }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}
