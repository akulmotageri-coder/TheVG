package com.example.vga.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pattern detection must be deterministic and must never be influenced by the
 * AI, so the rules are pinned down here.
 */
class PatternDetectorTest {

    private fun metricsWith(
        repetition: Double = 0.0,
        diversity: Double = 0.7,
        filler: Double = 0.0,
        vague: Double = 0.0,
        sentenceLength: Double = 10.0,
        variability: Double = 2.0,
        selfCorrection: Double = 0.0
    ) = TranscriptMetrics(
        wordCount = 100,
        distinctWordCount = 70,
        typeTokenRatio = diversity,
        sentenceCount = 10,
        meanSentenceLengthWords = sentenceLength,
        sentenceLengthStdDev = variability,
        shortSentenceRatePercent = 10.0,
        immediateRepetitionCount = 0,
        repeatedPhraseCount = 0,
        repeatedSentenceCount = 0,
        repetitionRatePercent = repetition,
        fillerCount = 0,
        fillerRatePer100Words = filler,
        vagueTermCount = 0,
        vagueTermRatePer100Words = vague,
        selfCorrectionCount = 0,
        selfCorrectionRatePer100Words = selfCorrection
    )

    private fun baselineOf(
        repetition: Double = 0.0,
        diversity: Double = 0.7,
        filler: Double = 0.0,
        vague: Double = 0.0,
        sentenceLength: Double = 10.0,
        variability: Double = 2.0,
        selfCorrection: Double = 0.0
    ) = mapOf(
        LanguageMetric.REPETITION_RATE to repetition,
        LanguageMetric.VOCABULARY_DIVERSITY to diversity,
        LanguageMetric.FILLER_RATE to filler,
        LanguageMetric.VAGUE_RATE to vague,
        LanguageMetric.MEAN_SENTENCE_LENGTH to sentenceLength,
        LanguageMetric.SENTENCE_VARIABILITY to variability,
        LanguageMetric.SELF_CORRECTION_RATE to selfCorrection
    )

    @Test
    fun `stable metrics produce no observations`() {
        val patterns = PatternDetector.detect(
            current = metricsWith(repetition = 3.0, diversity = 0.70),
            baseline = baselineOf(repetition = 3.0, diversity = 0.70)
        )
        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `rising repetition is reported`() {
        val patterns = PatternDetector.detect(
            current = metricsWith(repetition = 8.4),
            baseline = baselineOf(repetition = 3.1)
        )

        assertTrue(patterns.any { it.label.contains("Repetition increased") })
        val pattern = patterns.first { it.label.contains("Repetition") }
        assertTrue(pattern.evidence.contains("8.4"))
        assertTrue(pattern.evidence.contains("3.1"))
    }

    @Test
    fun `falling vocabulary diversity is reported`() {
        val patterns = PatternDetector.detect(
            current = metricsWith(diversity = 0.40),
            baseline = baselineOf(diversity = 0.72)
        )
        assertTrue(patterns.any { it.label.contains("Vocabulary diversity decreased") })
    }

    @Test
    fun `improvement is not flagged as a concern`() {
        // Diversity going UP is not a pattern requiring attention.
        val patterns = PatternDetector.detect(
            current = metricsWith(diversity = 0.95),
            baseline = baselineOf(diversity = 0.50)
        )
        assertFalse(patterns.any { it.label.contains("Vocabulary diversity") })
    }

    @Test
    fun `falling repetition is not flagged`() {
        val patterns = PatternDetector.detect(
            current = metricsWith(repetition = 1.0),
            baseline = baselineOf(repetition = 8.0)
        )
        assertFalse(patterns.any { it.label.contains("Repetition") })
    }

    @Test
    fun `small changes stay below the reporting threshold`() {
        // +10% is ordinary variation and must not be surfaced.
        val patterns = PatternDetector.detect(
            current = metricsWith(repetition = 3.3),
            baseline = baselineOf(repetition = 3.0)
        )
        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `large changes are marked high strength`() {
        val patterns = PatternDetector.detect(
            current = metricsWith(filler = 12.0),
            baseline = baselineOf(filler = 2.0)
        )
        assertEquals("high", patterns.first { it.label.contains("filler") }.strength)
    }

    @Test
    fun `without baseline only absolute thresholds apply`() {
        val quiet = PatternDetector.detect(
            current = metricsWith(repetition = 2.0, filler = 1.0),
            baseline = null
        )
        assertTrue(quiet.isEmpty())

        val loud = PatternDetector.detect(
            current = metricsWith(repetition = 15.0),
            baseline = null
        )
        assertTrue(loud.any { it.label.contains("repetition") })
    }

    @Test
    fun `confidence reflects the detected set`() {
        assertEquals("none", PatternDetector.confidenceOf(emptyList(), true))
        assertEquals(
            "low",
            PatternDetector.confidenceOf(
                listOf(DetectedPattern("x", "moderate", "")),
                false
            )
        )
        assertEquals(
            "high",
            PatternDetector.confidenceOf(
                listOf(DetectedPattern("x", "high", "")),
                true
            )
        )
    }

    @Test
    fun `summary never contains diagnostic wording`() {
        val patterns = PatternDetector.detect(
            current = metricsWith(repetition = 9.0, diversity = 0.3),
            baseline = baselineOf(repetition = 3.0, diversity = 0.7)
        )

        val summary = PatternDetector.summarise(patterns, hasBaseline = true)

        listOf("alzheimer", "dementia", "you have", "diagnos").forEach { banned ->
            assertFalse(
                "summary must not contain '$banned': $summary",
                summary.contains(banned, ignoreCase = true)
            )
        }
    }

    @Test
    fun `summary states when baseline is missing`() {
        val summary = PatternDetector.summarise(emptyList(), hasBaseline = false)
        assertTrue(summary.contains("Baseline not yet established"))
    }
}
