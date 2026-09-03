package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic tests for the Room-entity -> dashboard-DTO mapping (Sprint 8
 * Section 33 item 5: snapshot mapping) and for [DeveloperDiagnosticEventEntity]'s
 * shape (Section 33 item 1: diagnostic event serialization). Pure Kotlin,
 * no Android dependency, no database access.
 */
class DeveloperDiagnosticsMappingTest {

    @Test
    fun `event entity round-trips its event type name`() {
        val entity = DeveloperDiagnosticEventEntity(
            timestampEpochMs = 123L,
            eventType = DiagnosticEventType.OWNER_CONFIRMED.name,
            valueLong = 7L,
            valueDouble = 1.5,
            durationMs = 42L
        )

        assertEquals(DiagnosticEventType.OWNER_CONFIRMED, DiagnosticEventType.valueOf(entity.eventType))
    }

    @Test
    fun `event entity defaults are safe zero values, never sensitive content`() {
        val entity = DeveloperDiagnosticEventEntity(
            timestampEpochMs = 0L,
            eventType = DiagnosticEventType.SERVICE_CONNECTED.name
        )

        assertEquals(0L, entity.valueLong)
        assertEquals(0.0, entity.valueDouble, 0.0)
        assertEquals(0L, entity.durationMs)
    }

    @Test
    fun `toDiagnosticEventRow maps every field without altering values`() {
        val entity = DeveloperDiagnosticEventEntity(
            id = 9L,
            timestampEpochMs = 555L,
            eventType = DiagnosticEventType.AGGREGATION_SUCCESS.name,
            valueLong = 3L,
            valueDouble = 2.5,
            durationMs = 10L
        )

        val row = entity.toDiagnosticEventRow()

        assertEquals(555L, row.timestampEpochMs)
        assertEquals(DiagnosticEventType.AGGREGATION_SUCCESS.name, row.eventType)
        assertEquals(3L, row.valueLong)
        assertEquals(2.5, row.valueDouble, 0.0)
        assertEquals(10L, row.durationMs)
    }

    @Test
    fun `toResourceSnapshot reports cpu as available when process cpu time is present`() {
        val sample = sampleWithCpu(processCpuTimeMs = 1234L, cpuUtilizationPercent = 12.5)

        val snapshot = sample.toResourceSnapshot(peakTotalPssKb = 500, averageTotalPssKb = 400.0)

        assertEquals(1234L, snapshot.processCpuTimeMs)
        assertEquals(12.5, snapshot.cpuUtilizationPercent!!, 0.0)
        assertTrue(snapshot.cpuMeasurementNote.contains("wallClockElapsedMs"))
    }

    @Test
    fun `toResourceSnapshot documents unavailability when process cpu time is null`() {
        val sample = sampleWithCpu(processCpuTimeMs = null, cpuUtilizationPercent = null)

        val snapshot = sample.toResourceSnapshot(peakTotalPssKb = 0, averageTotalPssKb = 0.0)

        assertNull(snapshot.processCpuTimeMs)
        assertNull(snapshot.cpuUtilizationPercent)
        assertTrue(snapshot.cpuMeasurementNote.contains("unavailable"))
    }

    @Test
    fun `toResourceSnapshot carries through peak and average pss unchanged`() {
        val sample = sampleWithCpu(processCpuTimeMs = 1L, cpuUtilizationPercent = 1.0)

        val snapshot = sample.toResourceSnapshot(peakTotalPssKb = 999, averageTotalPssKb = 321.5)

        assertEquals(999, snapshot.peakTotalPssKb)
        assertEquals(321.5, snapshot.averageTotalPssKb!!, 0.0)
    }

    @Test
    fun `toResourceSnapshot maps a null peak or average pss to null (not a fabricated zero)`() {
        // Sprint 9 fabrication-audit fix: DeveloperDiagnosticsSampler now reports
        // null (not 0/0.0) until at least one valid PSS sample has been captured;
        // toResourceSnapshot must pass that through rather than substituting zero.
        val sample = sampleWithCpu(processCpuTimeMs = 1L, cpuUtilizationPercent = 1.0)

        val snapshot = sample.toResourceSnapshot(peakTotalPssKb = null, averageTotalPssKb = null)

        assertNull(snapshot.peakTotalPssKb)
        assertNull(snapshot.averageTotalPssKb)
    }

    @Test
    fun `toResourceSnapshot maps a non-positive totalPssKb to null (not a fabricated zero)`() {
        val sample = sampleWithCpu(processCpuTimeMs = 1L, cpuUtilizationPercent = 1.0).copy(totalPssKb = 0)

        val snapshot = sample.toResourceSnapshot(peakTotalPssKb = 0, averageTotalPssKb = 0.0)

        assertNull(snapshot.totalPssKb)
    }

    @Test
    fun `toResourceSnapshot maps a negative processUptimeMs to null`() {
        val sample = sampleWithCpu(processCpuTimeMs = 1L, cpuUtilizationPercent = 1.0).copy(processUptimeMs = -1L)

        val snapshot = sample.toResourceSnapshot(peakTotalPssKb = 0, averageTotalPssKb = 0.0)

        assertNull(snapshot.processUptimeMs)
    }

    private fun sampleWithCpu(processCpuTimeMs: Long?, cpuUtilizationPercent: Double?) = DeveloperDiagnosticSampleEntity(
        timestampEpochMs = 1000L,
        processCpuTimeMs = processCpuTimeMs,
        wallClockElapsedMs = 2000L,
        cpuUtilizationPercent = cpuUtilizationPercent,
        totalPssKb = 100,
        javaHeapKb = 10,
        nativeHeapKb = 20,
        privateDirtyKb = 30,
        batteryPercent = 80,
        isCharging = true,
        batteryTemperatureTenthsC = 250,
        thermalStatus = null,
        processUptimeMs = 5000L
    )
}
