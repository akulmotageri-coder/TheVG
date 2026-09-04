package com.example.vga.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The measured features drive every chart, observation and AI context string,
 * so they are verified deterministically here.
 */
class TranscriptAnalyticsTest {

    @Test
    fun `empty transcript yields zeroed metrics`() {
        val metrics = TranscriptAnalytics.compute("")

        assertEquals(0, metrics.wordCount)
        assertEquals(0.0, metrics.typeTokenRatio, 0.0)
        assertEquals(0, metrics.repeatedPhraseCount)
        assertEquals(0.0, metrics.repetitionRatePercent, 0.0)
        assertEquals(0, metrics.fillerCount)
    }

    @Test
    fun `word and diversity counts are correct`() {
        val metrics = TranscriptAnalytics.compute("the cat sat on the mat")

        assertEquals(6, metrics.wordCount)
        assertEquals(5, metrics.distinctWordCount)
        assertEquals(5.0 / 6.0, metrics.typeTokenRatio, 1e-9)
    }

    @Test
    fun `immediate repetition is detected`() {
        val metrics = TranscriptAnalytics.compute("I I went to the the shop")
        assertEquals(2, metrics.immediateRepetitionCount)
    }

    @Test
    fun `repeated phrases and repetition rate are detected`() {
        val metrics =
            TranscriptAnalytics.compute("where is my key where is my key today")

        assertTrue(metrics.repeatedPhraseCount >= 1)
        assertTrue(
            "repetition rate should be above zero, got ${metrics.repetitionRatePercent}",
            metrics.repetitionRatePercent > 0.0
        )
    }

    @Test
    fun `repeated sentences are detected`() {
        val metrics = TranscriptAnalytics.compute(
            "Where are my keys. I looked outside. Where are my keys."
        )
        assertEquals(1, metrics.repeatedSentenceCount)
    }

    @Test
    fun `no repetition yields zero rate`() {
        val metrics = TranscriptAnalytics.compute(
            "every single word here happens to be entirely distinct from another"
        )
        assertEquals(0.0, metrics.repetitionRatePercent, 1e-9)
        assertEquals(0, metrics.repeatedSentenceCount)
    }

    @Test
    fun `filler words are counted including phrases`() {
        val metrics = TranscriptAnalytics.compute(
            "um so you know it was like basically fine i mean okay"
        )

        // um, like, basically  +  "you know", "i mean"
        assertTrue(
            "expected several fillers, got ${metrics.fillerCount}",
            metrics.fillerCount >= 5
        )
        assertTrue(metrics.fillerRatePer100Words > 0.0)
    }

    @Test
    fun `vague terms are counted`() {
        val metrics = TranscriptAnalytics.compute("the thing with the stuff and something")
        assertTrue(metrics.vagueTermCount >= 3)
        assertTrue(metrics.vagueTermRatePer100Words > 0.0)
    }

    @Test
    fun `self corrections are detected`() {
        val metrics = TranscriptAnalytics.compute(
            "we went on tuesday no wait it was wednesday i meant wednesday"
        )
        assertTrue(
            "expected self-corrections, got ${metrics.selfCorrectionCount}",
            metrics.selfCorrectionCount >= 2
        )
    }

    @Test
    fun `sentence statistics are computed`() {
        val metrics = TranscriptAnalytics.compute("One two three. Four five six.")

        assertEquals(2, metrics.sentenceCount)
        assertEquals(3.0, metrics.meanSentenceLengthWords, 1e-9)
        assertEquals(0.0, metrics.sentenceLengthStdDev, 1e-9)
    }

    @Test
    fun `sentence length variability is measured`() {
        val varied = TranscriptAnalytics.compute(
            "Yes. This one is a considerably longer sentence with many more words."
        )
        assertTrue(
            "expected non-zero variability, got ${varied.sentenceLengthStdDev}",
            varied.sentenceLengthStdDev > 0.0
        )
    }

    @Test
    fun `short sentence rate is measured`() {
        val metrics = TranscriptAnalytics.compute("Yes. No. Maybe. This sentence is a longer one.")

        // three of four sentences are under four words
        assertEquals(75.0, metrics.shortSentenceRatePercent, 1e-9)
    }

    @Test
    fun `recogniser placeholders are stripped`() {
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
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse("[SOMETHING]"))
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse(""))
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse("just a few words"))
    }

    @Test
    fun `recogniser loops are rejected as degenerate`() {

        // Real observed IndicWhisper output: one token repeated 38 times.
        // Long enough to pass the word-count gate, so it needs its own guard.
        val loop = (1..38).joinToString(" ") { "ndashar" }

        assertTrue(TranscriptAnalytics.isDegenerateOutput(loop))
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse(loop))
    }

    @Test
    fun `phrase-level recogniser loops are rejected`() {

        // Real observed output from the 8 kHz call recording: Whisper looped
        // on "How are you?" ~43 times. No single token dominates (each is
        // about a third of the text), so this needs the phrase-level check.
        val loop = "Hello. How are you? " + (1..43).joinToString(" ") { "How are you?" }

        assertTrue(TranscriptAnalytics.isDegenerateOutput(loop))
        assertTrue(TranscriptAnalytics.isTooShortToAnalyse(loop))
    }

    @Test
    fun `clinically meaningful repetition is not rejected as a loop`() {

        // Genuine repetition of interest is far below the loop threshold and
        // must still reach analysis, otherwise the app would filter out the
        // very signal it exists to measure.
        val repetitive =
            "I went to the shop this morning. I could not find my keys anywhere. " +
                "I went to the shop this morning. Then I made a cup of tea and " +
                "sat down by the window for a while thinking about the garden."

        assertFalse(TranscriptAnalytics.isDegenerateOutput(repetitive))
        assertFalse(TranscriptAnalytics.isTooShortToAnalyse(repetitive))

        val metrics = TranscriptAnalytics.compute(repetitive)
        assertTrue(
            "expected measurable repetition, got ${metrics.repetitionRatePercent}",
            metrics.repetitionRatePercent > 0.0
        )
    }

    @Test
    fun `a dominant single token is degenerate`() {
        val dominated = "yes yes yes yes yes yes yes okay fine"
        assertTrue(TranscriptAnalytics.isDegenerateOutput(dominated))
    }

    @Test
    fun `natural speech is not treated as degenerate`() {

        val natural = "I went to the shop this morning and bought some bread " +
            "then walked home through the park because the weather was pleasant"

        assertFalse(TranscriptAnalytics.isDegenerateOutput(natural))
        assertFalse(TranscriptAnalytics.isTooShortToAnalyse(natural))
    }

    @Test
    fun `short natural phrases are not flagged degenerate`() {
        // Below the sample floor, so the guard must not fire on ordinary text.
        assertFalse(TranscriptAnalytics.isDegenerateOutput("hello there"))
    }

    @Test
    fun `a sufficiently long transcript is accepted`() {
        val realSpeech = (1..25).joinToString(" ") { "word$it" }
        assertFalse(TranscriptAnalytics.isTooShortToAnalyse(realSpeech))
    }

    @Test
    fun `baseline requires enough history`() {
        val one = listOf(TranscriptAnalytics.compute("some words here for testing"))
        assertNull(TranscriptAnalytics.baselineOf(one))

        val enough = (1..TranscriptAnalytics.MIN_HISTORY_FOR_BASELINE).map {
            TranscriptAnalytics.compute("some words here for testing number $it")
        }
        assertNotNull(TranscriptAnalytics.baselineOf(enough))
    }

    @Test
    fun `baseline averages each metric`() {
        val priors = listOf(
            TranscriptAnalytics.compute("alpha beta gamma delta epsilon zeta"),
            TranscriptAnalytics.compute("one two three four five six"),
            TranscriptAnalytics.compute("red green blue yellow white black")
        )

        val baseline = TranscriptAnalytics.baselineOf(priors)
        assertNotNull(baseline)

        val expected = priors.map { it.typeTokenRatio }.average()
        assertEquals(
            expected,
            baseline!![LanguageMetric.VOCABULARY_DIVERSITY]!!,
            1e-9
        )
    }

    @Test
    fun `percent change handles zero baseline`() {
        assertNull(TranscriptAnalytics.percentChange(5.0, 0.0))
        assertEquals(100.0, TranscriptAnalytics.percentChange(4.0, 2.0)!!, 1e-9)
        assertEquals(-50.0, TranscriptAnalytics.percentChange(1.0, 2.0)!!, 1e-9)
    }
}
