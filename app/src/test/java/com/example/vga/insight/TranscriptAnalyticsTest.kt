package com.example.vga.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The supporting metrics are measured locally rather than produced by the
 * model, so they are verified deterministically here.
 */
class TranscriptAnalyticsTest {

    @Test
    fun `empty transcript yields zeroed metrics`() {

        val metrics = TranscriptAnalytics.compute("")

        assertEquals(0, metrics.wordCount)
        assertEquals(0.0, metrics.typeTokenRatio, 0.0)
        assertEquals(0, metrics.repeatedPhraseCount)
    }

    @Test
    fun `word and diversity counts are correct`() {

        val metrics = TranscriptAnalytics.compute("the cat sat on the mat")

        assertEquals(6, metrics.wordCount)
        // distinct: the, cat, sat, on, mat = 5
        assertEquals(5, metrics.distinctWordCount)
        assertEquals(5.0 / 6.0, metrics.typeTokenRatio, 1e-9)
    }

    @Test
    fun `immediate repetition is detected`() {

        val metrics = TranscriptAnalytics.compute("I I went to the the shop")

        assertEquals(2, metrics.immediateRepetitionCount)
    }

    @Test
    fun `repeated phrases are detected`() {

        val metrics =
            TranscriptAnalytics.compute("where is my key where is my key today")

        assertTrue(
            "expected at least one repeated trigram, got ${metrics.repeatedPhraseCount}",
            metrics.repeatedPhraseCount >= 1
        )
    }

    @Test
    fun `vague terms are counted`() {

        val metrics = TranscriptAnalytics.compute("the thing with the stuff and something")

        assertTrue(metrics.vagueTermCount >= 3)
        assertTrue(metrics.vagueTermRatePer100Words > 0.0)
    }

    @Test
    fun `sentence length is computed`() {

        val metrics = TranscriptAnalytics.compute("One two three. Four five six.")

        assertEquals(2, metrics.sentenceCount)
        assertEquals(3.0, metrics.meanSentenceLengthWords, 1e-9)
    }

    @Test
    fun `recogniser placeholders are stripped`() {

        // Real observed output from IndicWhisper for audio with no
        // intelligible speech.
        assertEquals("", TranscriptAnalytics.stripPlaceholders("[SOMETHING]"))
        assertEquals("", TranscriptAnalytics.stripPlaceholders("[BLANK_AUDIO]"))
        assertEquals("", TranscriptAnalytics.stripPlaceholders("(inaudible)"))
        assertEquals("", TranscriptAnalytics.stripPlaceholders("[ Music ]"))

        assertEquals(
            "hello there",
            TranscriptAnalytics.stripPlaceholders("hello [NOISE] there")
        )
    }

    @Test
    fun `placeholder-only transcript is rejected as too short`() {

        // This is the case that previously produced three high-confidence
        // patterns from an effectively empty transcript.
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse("[SOMETHING]"))
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse(""))
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse("just a few words"))
    }

    @Test
    fun `placeholder is not counted as a vague term`() {

        val cleaned = TranscriptAnalytics.stripPlaceholders("[SOMETHING]")
        val metrics = TranscriptAnalytics.compute(cleaned)

        assertEquals(0, metrics.wordCount)
        assertEquals(0, metrics.vagueTermCount)
        assertEquals(0.0, metrics.vagueTermRatePer100Words, 0.0)
    }

    @Test
    fun `a sufficiently long transcript is accepted`() {

        val realSpeech = (1..25).joinToString(" ") { "word$it" }

        assertFalse(TranscriptAnalytics.isTooShortToAnalyse(realSpeech))
    }

    @Test
    fun `baseline comparison is null without history`() {

        val current = TranscriptAnalytics.compute("some words here")

        assertNull(TranscriptAnalytics.compareWithBaseline(current, emptyList()))
    }

    @Test
    fun `baseline comparison reports against history`() {

        val prior = TranscriptAnalytics.compute(
            "a rich and varied description of the garden this morning"
        )
        val current = TranscriptAnalytics.compute(
            "the thing the thing the thing stuff stuff"
        )

        val comparison =
            TranscriptAnalytics.compareWithBaseline(current, listOf(prior))

        assertNotNull(comparison)
        assertTrue(comparison!!.contains("1 previous transcript"))
        assertTrue(comparison.contains("Vocabulary diversity"))
    }
}
