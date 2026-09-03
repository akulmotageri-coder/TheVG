package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deterministic tests for [DeveloperDiagnosticsSessionMath] (Sprint 9
 * diagnostics-fabrication-audit fix: session-analytics averages must report
 * "no data" as `null`, never a fabricated `0.0`). Pure arithmetic, no
 * Android runtime required.
 */
class DeveloperDiagnosticsSessionMathTest {

    @Test
    fun `average of an empty int list is null, not zero`() {
        assertNull(DeveloperDiagnosticsSessionMath.averageIntOrNull(emptyList()))
    }

    @Test
    fun `average of a non-empty int list is the real average`() {
        val result = DeveloperDiagnosticsSessionMath.averageIntOrNull(listOf(10, 20, 30))
        assertEquals(20.0, result!!, 0.0001)
    }

    @Test
    fun `average of an empty double list is null, not zero`() {
        assertNull(DeveloperDiagnosticsSessionMath.averageDoubleOrNull(emptyList()))
    }

    @Test
    fun `average of a non-empty double list is the real average`() {
        val result = DeveloperDiagnosticsSessionMath.averageDoubleOrNull(listOf(1.0, 2.0, 3.0))
        assertEquals(2.0, result!!, 0.0001)
    }

    @Test
    fun `pause frequency is null when there are no characters to divide by`() {
        assertNull(DeveloperDiagnosticsSessionMath.pauseFrequencyPer100Chars(totalChars = 0L, totalMicroPauses = 0L))
        // Even with a nonsensical positive pause count and zero chars, still
        // null (undefined), never a fabricated number.
        assertNull(DeveloperDiagnosticsSessionMath.pauseFrequencyPer100Chars(totalChars = 0L, totalMicroPauses = 5L))
    }

    @Test
    fun `pause frequency matches BehavioralAggregationEngine's SUM over SUM times one hundred formula`() {
        val result = DeveloperDiagnosticsSessionMath.pauseFrequencyPer100Chars(totalChars = 200L, totalMicroPauses = 10L)
        // 10 / 200 * 100 = 5.0
        assertEquals(5.0, result!!, 0.0001)
    }
}
