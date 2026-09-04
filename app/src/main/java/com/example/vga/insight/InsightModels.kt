package com.example.vga.insight

/**
 * A transcript produced by the existing IndicWhisper pipeline, persisted so it
 * can be analysed later and so a personal baseline can be built from history.
 */
data class TranscriptRecord(
    val timestampMs: Long,
    val sourceFileName: String,
    val text: String
) {
    val wordCount: Int
        get() = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }

    /** Distinct words / total words. A crude lexical-diversity ratio. */
    val typeTokenRatio: Double
        get() {
            val words = text.lowercase()
                .split(Regex("[^\\p{L}\\p{N}']+"))
                .filter { it.isNotBlank() }
            if (words.isEmpty()) return 0.0
            return words.toSet().size.toDouble() / words.size.toDouble()
        }
}


/** One linguistic pattern the model reported. */
data class DetectedPattern(
    val label: String,
    val strength: String,
    val evidence: String
)


/** A supporting metric shown alongside the insight. */
data class SupportingMetric(
    val label: String,
    val value: String,
    val source: String
)


/**
 * The stored result of one transcript analysis. This is the "timeline event"
 * surfaced on the Main Dashboard.
 *
 * Deliberately carries no disease label or diagnosis - only observed patterns,
 * supporting metrics and a plain-language explanation.
 */
data class LinguisticInsight(
    val timestampMs: Long,
    val sourceFileName: String,
    val patterns: List<DetectedPattern>,
    val supportingMetrics: List<SupportingMetric>,
    val baselineComparison: String,
    val confidence: String,
    val explanation: String,
    val transcriptWordCount: Int,
    val modelName: String,
    /** True when [DiagnosisGuard] had to redact diagnostic phrasing. */
    val guardApplied: Boolean
) {
    /** Highest-strength pattern, used for the compact timeline row. */
    val headlinePattern: String
        get() = patterns.firstOrNull()?.label ?: "No specific pattern reported"
}


/**
 * What the fusion step managed to gather. Any field may be absent - the
 * analyser sends only what genuinely exists and never invents values.
 */
data class FusionInputs(
    val transcript: TranscriptRecord,
    val acousticFeatures: FloatArray?,
    val keyboardSummary: KeyboardSnapshot?,
    val cognitiveSummary: CognitiveSnapshot?,
    val priorTranscripts: List<TranscriptRecord>
)


data class KeyboardSnapshot(
    val avgTypingSpeedCpm: Double,
    val avgBackspaceRate: Double,
    val pauseFrequencyPer100Chars: Double,
    val sessionCount: Int,
    val riskContributionScore: Double
)


data class CognitiveSnapshot(
    val stroopAccuracyPercent: Double?,
    val stroopAvgResponseMs: Long?,
    val digitSpanForward: Int?,
    val digitSpanBackward: Int?,
    val trailMakingPartAMs: Long?,
    val trailMakingPartBMs: Long?
)
