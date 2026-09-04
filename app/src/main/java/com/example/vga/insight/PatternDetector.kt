package com.example.vga.insight

import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * Turns measured metrics into plain-language observations.
 *
 * This is deliberately deterministic and reproducible: the same transcript and
 * the same history always produce the same observations. The AI assistant is
 * never consulted here - it may later *explain* these observations, but it
 * cannot create, remove or alter one.
 *
 * Pure Kotlin, so the whole rule set is unit-testable on the JVM.
 */
object PatternDetector {

    /**
     * A metric must move at least this far from the personal baseline before
     * it is reported, so ordinary session-to-session noise is not surfaced as
     * a finding.
     */
    private const val NOTABLE_CHANGE_PERCENT = 25.0
    private const val STRONG_CHANGE_PERCENT = 50.0

    /**
     * Thresholds used only when no baseline exists yet. These describe the
     * transcript on its own terms and are intentionally conservative.
     */
    private const val HIGH_REPETITION_PERCENT = 10.0
    private const val HIGH_FILLER_PER_100 = 8.0
    private const val HIGH_VAGUE_PER_100 = 6.0
    private const val HIGH_SHORT_SENTENCE_PERCENT = 60.0

    /**
     * Detects observations for [current], comparing against [baseline] when
     * one exists.
     *
     * @param baseline mean of each metric over prior transcripts, or null when
     *   there is not enough history.
     */
    fun detect(
        current: TranscriptMetrics,
        baseline: Map<LanguageMetric, Double>?
    ): List<DetectedPattern> {

        return if (baseline == null) {
            detectWithoutBaseline(current)
        } else {
            detectAgainstBaseline(current, baseline)
        }
    }

    // ========================================================
    // WITH BASELINE
    // ========================================================

    private fun detectAgainstBaseline(
        current: TranscriptMetrics,
        baseline: Map<LanguageMetric, Double>
    ): List<DetectedPattern> {

        val found = mutableListOf<DetectedPattern>()

        LanguageMetric.entries.forEach { metric ->

            val now = metric.valueOf(current)
            val before = baseline[metric] ?: return@forEach
            val change = TranscriptAnalytics.percentChange(now, before) ?: return@forEach

            if (abs(change) < NOTABLE_CHANGE_PERCENT) return@forEach

            val rising = change > 0
            val notable = rising == metric.higherIsNotable
            if (!notable) return@forEach

            val strength = if (abs(change) >= STRONG_CHANGE_PERCENT) "high" else "moderate"

            found += DetectedPattern(
                label = observationLabel(metric, rising),
                strength = strength,
                evidence =
                    "${metric.label}: ${metric.format(now)} now vs " +
                        "${metric.format(before)} baseline " +
                        "(${if (rising) "+" else ""}${change.roundToInt()}%)"
            )
        }

        return found.sortedByDescending { if (it.strength == "high") 1 else 0 }
    }

    private fun observationLabel(metric: LanguageMetric, rising: Boolean): String =
        when (metric) {
            LanguageMetric.REPETITION_RATE ->
                "Repetition increased compared with baseline"
            LanguageMetric.VOCABULARY_DIVERSITY ->
                "Vocabulary diversity decreased compared with baseline"
            LanguageMetric.FILLER_RATE ->
                "More frequent filler words than baseline"
            LanguageMetric.VAGUE_RATE ->
                "More frequent use of vague words than baseline"
            LanguageMetric.MEAN_SENTENCE_LENGTH ->
                "Average sentence length decreased compared with baseline"
            LanguageMetric.SENTENCE_VARIABILITY ->
                if (rising) "Sentence-length variability increased"
                else "Sentence-length variability decreased"
            LanguageMetric.SELF_CORRECTION_RATE ->
                "More frequent self-corrections than baseline"
        }

    // ========================================================
    // WITHOUT BASELINE
    // ========================================================

    private fun detectWithoutBaseline(
        current: TranscriptMetrics
    ): List<DetectedPattern> {

        val found = mutableListOf<DetectedPattern>()

        if (current.repetitionRatePercent >= HIGH_REPETITION_PERCENT) {
            found += DetectedPattern(
                label = "Noticeable repetition in this recording",
                strength = "moderate",
                evidence =
                    "Repetition rate ${round1(current.repetitionRatePercent)}% " +
                        "(${current.repeatedPhraseCount} repeated phrases, " +
                        "${current.repeatedSentenceCount} repeated sentences)"
            )
        }

        if (current.fillerRatePer100Words >= HIGH_FILLER_PER_100) {
            found += DetectedPattern(
                label = "Frequent filler words in this recording",
                strength = "moderate",
                evidence =
                    "${current.fillerCount} filler words " +
                        "(${round1(current.fillerRatePer100Words)} per 100 words)"
            )
        }

        if (current.vagueTermRatePer100Words >= HIGH_VAGUE_PER_100) {
            found += DetectedPattern(
                label = "Frequent use of vague or general words",
                strength = "moderate",
                evidence =
                    "${current.vagueTermCount} vague words " +
                        "(${round1(current.vagueTermRatePer100Words)} per 100 words)"
            )
        }

        if (current.shortSentenceRatePercent >= HIGH_SHORT_SENTENCE_PERCENT) {
            found += DetectedPattern(
                label = "Many short or fragmented sentences",
                strength = "moderate",
                evidence =
                    "${current.shortSentenceRatePercent.roundToInt()}% of sentences " +
                        "were under 4 words"
            )
        }

        return found
    }

    /**
     * Short, non-diagnostic summary of the overall picture. Never a score and
     * never a disease label.
     */
    fun summarise(
        patterns: List<DetectedPattern>,
        hasBaseline: Boolean
    ): String {

        if (patterns.isEmpty()) {
            return if (hasBaseline) {
                "No notable change from your personal baseline was measured in " +
                    "this recording."
            } else {
                "No notable language pattern was measured in this recording. " +
                    "Baseline not yet established."
            }
        }

        val strongest = if (patterns.any { it.strength == "high" }) "high" else "moderate"

        val lead = patterns.joinToString("; ") { it.label }

        return "Observed: $lead. Strength of change: $strongest. " +
            "These are measured language patterns only."
    }

    /** Overall confidence label derived from the detected set, not from AI. */
    fun confidenceOf(patterns: List<DetectedPattern>, hasBaseline: Boolean): String = when {
        patterns.isEmpty() -> "none"
        !hasBaseline -> "low"
        patterns.any { it.strength == "high" } -> "high"
        else -> "moderate"
    }
}
