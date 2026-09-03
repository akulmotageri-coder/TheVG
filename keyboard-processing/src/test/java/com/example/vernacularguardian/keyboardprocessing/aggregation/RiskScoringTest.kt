package com.example.vernacularguardian.keyboardprocessing.aggregation

import org.junit.Assert.assertEquals
import org.junit.Test

class RiskScoringTest {

    private val tolerance = 1e-9

    // --- normalizeBackspaceRate: domain 0.0..1.0, saturates at 1.0 -> 100 ---

    @Test
    fun `backspace rate zero normalizes to zero`() {
        assertEquals(0.0, RiskScoring.normalizeBackspaceRate(0.0), tolerance)
    }

    @Test
    fun `backspace rate at max (1_0) normalizes to 100`() {
        assertEquals(100.0, RiskScoring.normalizeBackspaceRate(1.0), tolerance)
    }

    @Test
    fun `backspace rate midpoint (0_5) normalizes to 50`() {
        assertEquals(50.0, RiskScoring.normalizeBackspaceRate(0.5), tolerance)
    }

    @Test
    fun `backspace rate above 1_0 clamps to 100`() {
        assertEquals(100.0, RiskScoring.normalizeBackspaceRate(1.5), tolerance)
    }

    // --- normalizePauseFrequency: saturates at 20.0 pauses per 100 chars -> 100 ---

    @Test
    fun `pause frequency zero normalizes to zero`() {
        assertEquals(0.0, RiskScoring.normalizePauseFrequency(0.0), tolerance)
    }

    @Test
    fun `pause frequency at saturation (20_0) normalizes to 100`() {
        assertEquals(100.0, RiskScoring.normalizePauseFrequency(20.0), tolerance)
    }

    @Test
    fun `pause frequency midpoint (10_0) normalizes to 50`() {
        assertEquals(50.0, RiskScoring.normalizePauseFrequency(10.0), tolerance)
    }

    @Test
    fun `pause frequency above saturation clamps to 100`() {
        assertEquals(100.0, RiskScoring.normalizePauseFrequency(30.0), tolerance)
    }

    // --- normalizeIntervalStdDev: saturates at 1000ms -> 100 ---

    @Test
    fun `interval std dev zero normalizes to zero`() {
        assertEquals(0.0, RiskScoring.normalizeIntervalStdDev(0.0), tolerance)
    }

    @Test
    fun `interval std dev at saturation (1000ms) normalizes to 100`() {
        assertEquals(100.0, RiskScoring.normalizeIntervalStdDev(1000.0), tolerance)
    }

    @Test
    fun `interval std dev midpoint (500ms) normalizes to 50`() {
        assertEquals(50.0, RiskScoring.normalizeIntervalStdDev(500.0), tolerance)
    }

    @Test
    fun `interval std dev above saturation clamps to 100`() {
        assertEquals(100.0, RiskScoring.normalizeIntervalStdDev(2000.0), tolerance)
    }

    // --- calculate(): PRD-exact weights (0.40 / 0.35 / 0.25), final clamp 0-100 ---

    @Test
    fun `calculate all-zero inputs yields zero risk`() {
        assertEquals(0.0, RiskScoring.calculate(0.0, 0.0, 0.0), tolerance)
    }

    @Test
    fun `calculate all-saturated inputs yields 100 risk`() {
        assertEquals(100.0, RiskScoring.calculate(1.0, 20.0, 1000.0), tolerance)
    }

    @Test
    fun `calculate all-above-saturation inputs still clamps to 100`() {
        assertEquals(100.0, RiskScoring.calculate(5.0, 200.0, 10_000.0), tolerance)
    }

    @Test
    fun `calculate matches the sprint 4 brief's worked two-session example`() {
        // Session 1: cpm=60, backspaceRate=0.10, intervalStdDevMs=100, microPauseCount=2, charCount=100
        // Session 2: cpm=120, backspaceRate=0.20, intervalStdDevMs=300, microPauseCount=3, charCount=200
        // avgBackspaceRate = AVG(0.10, 0.20) = 0.15
        // intervalStdDevMs (daily) = AVG(100, 300) = 200
        // pauseFrequencyPer100Chars = (2+3)/(100+200)*100 = 1.6666666666666667
        val avgBackspaceRate = 0.15
        val intervalStdDevMs = 200.0
        val pauseFrequencyPer100Chars = (2 + 3).toDouble() / (100 + 200).toDouble() * 100.0

        val normalizedBackspace = 15.0 // clamp(0.15,0,1)*100
        val normalizedPause = (pauseFrequencyPer100Chars / 20.0) * 100.0 // 8.333333333333334
        val normalizedInterval = 20.0 // clamp(200/1000,0,1)*100

        val expected = 0.40 * normalizedBackspace + 0.35 * normalizedPause + 0.25 * normalizedInterval

        assertEquals(
            expected,
            RiskScoring.calculate(avgBackspaceRate, pauseFrequencyPer100Chars, intervalStdDevMs),
            tolerance
        )
        // Sanity: expected sits well within (0, 100) for this moderate example.
        assertEquals(13.916666666666666, expected, tolerance)
    }
}
