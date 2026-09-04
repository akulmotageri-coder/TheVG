package com.example.vga.insight

import android.content.Context
import com.example.vga.dementia.acoustic.AcousticFeatureStore
import java.io.File
import kotlin.math.roundToInt


enum class TimelineCategory(val label: String, val icon: String) {
    SPEECH("Speech", "🎙"),
    LANGUAGE("Language", "💬"),
    PATTERN("Pattern", "◈"),
    ACOUSTIC("Acoustic", "≋"),
    KEYBOARD("Keyboard", "⌨"),
    COGNITIVE("Cognitive", "🧠")
}


/**
 * One chronological event. Every field comes from a file or record that
 * actually exists on the device - nothing here is generated or predicted.
 */
data class TimelineEvent(
    val timestampMs: Long,
    val category: TimelineCategory,
    val title: String,
    val description: String,
    /** Optional compact metric shown on the right of the row. */
    val metric: String?
)


/**
 * Assembles the dashboard timeline from real stored application data.
 *
 * Sources, all pre-existing except the transcript/insight stores:
 *  - call_recordings (WAV)          (share-sheet imports)
 *  - extracted_voice (WAV)          (CallProcessingWorker output)
 *  - acoustic_features.json         (openSMILE eGeMAPSv02)
 *  - linguistic_transcripts.json    (IndicWhisper transcripts)
 *  - linguistic_insights.json       (deterministic pattern analysis)
 *  - cognitive_results.json         (cognitive test results)
 *  - keyboard daily summary         (keyboard-processing Room DB)
 *
 * If a source is absent it contributes no events, rather than a placeholder.
 */
object TimelineBuilder {

    fun build(
        context: Context,
        keyboard: KeyboardSnapshot? = null,
        limit: Int = 40
    ): List<TimelineEvent> {

        val events = mutableListOf<TimelineEvent>()

        // ----- imported call recordings -----

        listWavs(context, "call_recordings").forEach { file ->
            events += TimelineEvent(
                timestampMs = file.lastModified(),
                category = TimelineCategory.SPEECH,
                title = "Call recording imported",
                description = file.name,
                metric = "${file.length() / 1024} KB"
            )
        }

        // ----- speaker-separated output -----

        listWavs(context, "extracted_voice").forEach { file ->
            events += TimelineEvent(
                timestampMs = file.lastModified(),
                category = TimelineCategory.SPEECH,
                title = "Voice separated",
                description = "Speaker-separated audio produced from the call",
                metric = "${file.length() / 1024} KB"
            )
        }

        // ----- acoustic features -----

        AcousticFeatureStore.get(context)?.let { features ->
            val file = File(context.filesDir, "acoustic_features.json")
            if (file.exists()) {
                events += TimelineEvent(
                    timestampMs = file.lastModified(),
                    category = TimelineCategory.ACOUSTIC,
                    title = "Acoustic features extracted",
                    description = "openSMILE eGeMAPSv02 feature set computed",
                    metric = "${features.size} values"
                )
            }
        }

        // ----- transcripts -----

        TranscriptStore.getAll(context).forEach { record ->
            val metrics = TranscriptAnalytics.compute(
                TranscriptAnalytics.stripPlaceholders(record.text)
            )
            events += TimelineEvent(
                timestampMs = record.timestampMs,
                category = TimelineCategory.LANGUAGE,
                title = "Transcript processed",
                description = record.sourceFileName,
                metric = "${metrics.wordCount} words"
            )
        }

        // ----- detected patterns -----

        InsightStore.getAll(context).forEach { insight ->
            if (insight.patterns.isEmpty()) {
                events += TimelineEvent(
                    timestampMs = insight.timestampMs,
                    category = TimelineCategory.PATTERN,
                    title = "Language analysis completed",
                    description = "No notable change measured",
                    metric = null
                )
            } else {
                events += TimelineEvent(
                    timestampMs = insight.timestampMs,
                    category = TimelineCategory.PATTERN,
                    title = insight.patterns.first().label,
                    description =
                        if (insight.patterns.size > 1) {
                            "+${insight.patterns.size - 1} further observation" +
                                (if (insight.patterns.size == 2) "" else "s")
                        } else {
                            "Measured from the transcript"
                        },
                    metric = insight.confidence
                )
            }
        }

        // ----- cognitive tests -----

        CognitiveResultStore.get(context)?.let { snapshot ->
            val file = File(context.filesDir, "cognitive_results.json")
            if (file.exists()) {
                val parts = buildList {
                    snapshot.stroopAccuracyPercent?.let { add("Stroop ${it.roundToInt()}%") }
                    snapshot.digitSpanForward?.let { add("Digit span $it") }
                    snapshot.trailMakingPartAMs?.let { add("Trail A ${it / 1000}s") }
                }
                events += TimelineEvent(
                    timestampMs = file.lastModified(),
                    category = TimelineCategory.COGNITIVE,
                    title = "Cognitive test completed",
                    description =
                        if (parts.isEmpty()) "Results recorded"
                        else parts.joinToString(" · "),
                    metric = null
                )
            }
        }

        // ----- keyboard -----

        keyboard?.let {
            events += TimelineEvent(
                timestampMs = System.currentTimeMillis(),
                category = TimelineCategory.KEYBOARD,
                title = "Keyboard sessions analysed",
                description =
                    "${it.sessionCount} session" +
                        (if (it.sessionCount == 1) "" else "s") + " today",
                metric = "${it.avgTypingSpeedCpm.roundToInt()} cpm"
            )
        }

        return events
            .sortedByDescending { it.timestampMs }
            .take(limit)
    }

    private fun listWavs(context: Context, dirName: String): List<File> {
        val dir = File(context.filesDir, dirName)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("wav", ignoreCase = true) }
            ?: emptyList()
    }


    /**
     * Which modalities currently have real data, for the multimodal overview.
     * "Changed" is only true when a measured comparison actually exists.
     */
    data class ModalityStatus(
        val label: String,
        val icon: String,
        val available: Boolean,
        val detail: String
    )

    fun modalityOverview(
        context: Context,
        keyboard: KeyboardSnapshot?
    ): List<ModalityStatus> {

        val transcripts = TranscriptStore.getAll(context)
        val insights = InsightStore.getAll(context)
        val acoustic = AcousticFeatureStore.get(context)
        val cognitive = CognitiveResultStore.get(context)
        val extracted = listWavs(context, "extracted_voice")

        val analysable = transcripts.count {
            !TranscriptAnalytics.isTooShortToAnalyse(it.text)
        }

        return listOf(
            ModalityStatus(
                label = "Speech",
                icon = TimelineCategory.SPEECH.icon,
                available = extracted.isNotEmpty(),
                detail =
                    if (extracted.isEmpty()) "No processed recordings"
                    else "${extracted.size} processed recording" +
                        (if (extracted.size == 1) "" else "s")
            ),
            ModalityStatus(
                label = "Language",
                icon = TimelineCategory.LANGUAGE.icon,
                available = analysable > 0,
                detail =
                    if (analysable == 0) "No analysable transcript yet"
                    else "$analysable analysable transcript" +
                        (if (analysable == 1) "" else "s") +
                        (if (analysable >= TranscriptAnalytics.MIN_HISTORY_FOR_BASELINE)
                            " · baseline ready" else " · baseline pending")
            ),
            ModalityStatus(
                label = "Keyboard",
                icon = TimelineCategory.KEYBOARD.icon,
                available = keyboard != null,
                detail =
                    keyboard?.let {
                        "${it.sessionCount} session" +
                            (if (it.sessionCount == 1) "" else "s") +
                            " · ${it.avgTypingSpeedCpm.roundToInt()} cpm"
                    } ?: "No typing sessions today"
            ),
            ModalityStatus(
                label = "Cognitive",
                icon = TimelineCategory.COGNITIVE.icon,
                available = cognitive != null,
                detail =
                    cognitive?.let { snapshot ->
                        buildList {
                            snapshot.stroopAccuracyPercent?.let { add("Stroop ${it.roundToInt()}%") }
                            snapshot.digitSpanForward?.let { add("Span $it") }
                        }.joinToString(" · ").ifBlank { "Results recorded" }
                    } ?: "No test completed yet"
            ),
            ModalityStatus(
                label = "Acoustic",
                icon = TimelineCategory.ACOUSTIC.icon,
                available = acoustic != null,
                detail =
                    acoustic?.let { "${it.size} eGeMAPSv02 values" }
                        ?: "No acoustic features yet"
            ),
            ModalityStatus(
                label = "Patterns",
                icon = TimelineCategory.PATTERN.icon,
                available = insights.isNotEmpty(),
                detail =
                    if (insights.isEmpty()) "No analysis run yet"
                    else "${insights.size} analysis" +
                        (if (insights.size == 1) "" else "es") +
                        " · ${insights.first().patterns.size} observation(s) latest"
            )
        )
    }
}
