package com.example.vernacularguardian.keyboardprocessing

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deterministic, Android-runtime-free guard against
 * [BehavioralDailySummary] drifting from the PRD-exact public field list
 * (master-prompt Section 8/27 item 9's "no field added/removed" contract).
 */
class KeyboardProcessingApiTest {

    @Test
    fun `BehavioralDailySummary exposes exactly the PRD-required fields`() {
        val fieldNames = BehavioralDailySummary::class.java.declaredFields
            .map { it.name }
            .toSet()

        assertEquals(
            setOf(
                "dateEpochDay",
                "avgTypingSpeedCpm",
                "avgBackspaceRate",
                "intervalStdDevMs",
                "pauseFrequencyPer100Chars",
                "sessionCount",
                "riskContributionScore",
                "isReliable"
            ),
            fieldNames
        )
    }

    @Test
    fun `BehavioralDailySummary field types match the PRD contract`() {
        val summary = BehavioralDailySummary(
            dateEpochDay = 1L,
            avgTypingSpeedCpm = 2.0,
            avgBackspaceRate = 3.0,
            intervalStdDevMs = 4.0,
            pauseFrequencyPer100Chars = 5.0,
            sessionCount = 6,
            riskContributionScore = 7.0,
            isReliable = true
        )

        assertEquals(1L, summary.dateEpochDay)
        assertEquals(2.0, summary.avgTypingSpeedCpm, 0.0)
        assertEquals(3.0, summary.avgBackspaceRate, 0.0)
        assertEquals(4.0, summary.intervalStdDevMs, 0.0)
        assertEquals(5.0, summary.pauseFrequencyPer100Chars, 0.0)
        assertEquals(6, summary.sessionCount)
        assertEquals(7.0, summary.riskContributionScore, 0.0)
        assertEquals(true, summary.isReliable)
    }
}
