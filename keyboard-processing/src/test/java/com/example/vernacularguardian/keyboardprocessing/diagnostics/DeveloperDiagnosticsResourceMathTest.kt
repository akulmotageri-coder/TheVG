package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic tests for [DeveloperDiagnosticsResourceMath] (Sprint 8
 * Section 33 items 7-8: resource-sample and battery-state mapping). Pure
 * arithmetic, no Android runtime required.
 */
class DeveloperDiagnosticsResourceMathTest {

    @Test
    fun `cpu utilization is null on the first sample (no previous point)`() {
        val result = DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = null,
            previousWallClockMs = null,
            currentCpuTimeMs = 1000L,
            currentWallClockMs = 5000L
        )
        assertNull(result)
    }

    @Test
    fun `cpu utilization is null when cpu time is unavailable`() {
        val result = DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = 1000L,
            previousWallClockMs = 5000L,
            currentCpuTimeMs = null,
            currentWallClockMs = 10000L
        )
        assertNull(result)
    }

    @Test
    fun `cpu utilization at half load over the interval is fifty percent`() {
        val result = DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = 1000L,
            previousWallClockMs = 0L,
            currentCpuTimeMs = 3000L,
            currentWallClockMs = 4000L
        )
        // 2000ms of CPU time over a 4000ms wall-clock window = 50%.
        assertEquals(50.0, result!!, 0.0001)
    }

    @Test
    fun `cpu utilization can exceed one hundred percent on a multi-core device and is not clamped`() {
        val result = DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = 0L,
            previousWallClockMs = 0L,
            currentCpuTimeMs = 3000L,
            currentWallClockMs = 1000L
        )
        // 3000ms of CPU time over only a 1000ms wall-clock window = 300%.
        assertEquals(300.0, result!!, 0.0001)
        assertTrue(result > 100.0)
    }

    @Test
    fun `cpu utilization is null when no wall-clock time has elapsed`() {
        val result = DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = 1000L,
            previousWallClockMs = 5000L,
            currentCpuTimeMs = 2000L,
            currentWallClockMs = 5000L
        )
        assertNull(result)
    }

    @Test
    fun `cpu utilization is null when the cpu counter goes backwards`() {
        val result = DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = 5000L,
            previousWallClockMs = 0L,
            currentCpuTimeMs = 1000L,
            currentWallClockMs = 1000L
        )
        assertNull(result)
    }

    @Test
    fun `battery percent computes from level and scale`() {
        assertEquals(50, DeveloperDiagnosticsResourceMath.batteryPercent(level = 50, scale = 100))
        assertEquals(100, DeveloperDiagnosticsResourceMath.batteryPercent(level = 5, scale = 5))
    }

    @Test
    fun `battery percent is null for an invalid level or scale`() {
        assertNull(DeveloperDiagnosticsResourceMath.batteryPercent(level = -1, scale = 100))
        assertNull(DeveloperDiagnosticsResourceMath.batteryPercent(level = 50, scale = 0))
    }

    @Test
    fun `isCharging is true for the charging and full statuses`() {
        assertTrue(DeveloperDiagnosticsResourceMath.isCharging(status = 2)) // BATTERY_STATUS_CHARGING
        assertTrue(DeveloperDiagnosticsResourceMath.isCharging(status = 5)) // BATTERY_STATUS_FULL
    }

    @Test
    fun `isCharging is false for discharging or unknown statuses`() {
        assertTrue(!DeveloperDiagnosticsResourceMath.isCharging(status = 3)) // BATTERY_STATUS_DISCHARGING
        assertTrue(!DeveloperDiagnosticsResourceMath.isCharging(status = 1)) // BATTERY_STATUS_UNKNOWN
    }
}
