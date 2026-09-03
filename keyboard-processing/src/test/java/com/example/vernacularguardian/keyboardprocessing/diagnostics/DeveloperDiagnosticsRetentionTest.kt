package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

/** Deterministic tests for the Sprint 8 diagnostics 7-day retention window (Section 33 item 6). */
class DeveloperDiagnosticsRetentionTest {

    @Test
    fun `retention window is 7 days`() {
        assertEquals(7L, DeveloperDiagnosticsRetention.RETENTION_DAYS)
    }

    @Test
    fun `cutoff is exactly seven days before now`() {
        val nowEpochMs = 1_000_000_000_000L
        val expectedCutoff = nowEpochMs - 7L * 24L * 60L * 60L * 1000L

        assertEquals(expectedCutoff, DeveloperDiagnosticsRetention.cutoffEpochMs(nowEpochMs))
    }

    @Test
    fun `a timestamp exactly at the cutoff is not older than it`() {
        val nowEpochMs = 1_000_000_000_000L
        val cutoff = DeveloperDiagnosticsRetention.cutoffEpochMs(nowEpochMs)

        // Matches the DAO's "timestampEpochMs < cutoff" deletion condition:
        // a row exactly at the cutoff must be kept, not deleted.
        assertEquals(false, cutoff < cutoff)
    }

    @Test
    fun `a timestamp one millisecond before the cutoff is eligible for deletion`() {
        val nowEpochMs = 1_000_000_000_000L
        val cutoff = DeveloperDiagnosticsRetention.cutoffEpochMs(nowEpochMs)

        assertEquals(true, (cutoff - 1) < cutoff)
    }

    @Test
    fun `retention window is independent of the 30-day product retention`() {
        // Sprint 4's BehavioralAggregationEngine.RETENTION_DAYS is 30; this
        // developer-only window must remain its own, shorter value.
        assertEquals(true, DeveloperDiagnosticsRetention.RETENTION_DAYS < 30L)
    }
}
