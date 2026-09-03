package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Deterministic tests for [DeveloperDiagnosticsAggregationState] (Sprint 8
 * Section 33 item 10: aggregation of diagnostic counters/state). Pure
 * Kotlin, no Android dependency.
 */
class DeveloperDiagnosticsAggregationStateTest {

    @After
    fun resetSingletonState() {
        DeveloperDiagnosticsAggregationState.reset()
    }

    @Test
    fun `initial state has no recorded runs`() {
        assertNull(DeveloperDiagnosticsAggregationState.lastStartEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastSuccessEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastFailureEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastDurationMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastRetentionDeletedRows)
    }

    @Test
    fun `recordStart sets the start timestamp only`() {
        DeveloperDiagnosticsAggregationState.recordStart(100L)

        assertEquals(100L, DeveloperDiagnosticsAggregationState.lastStartEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastSuccessEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastFailureEpochMs)
    }

    @Test
    fun `recordSuccess sets success timestamp and duration`() {
        DeveloperDiagnosticsAggregationState.recordStart(100L)
        DeveloperDiagnosticsAggregationState.recordSuccess(150L, 50L)

        assertEquals(150L, DeveloperDiagnosticsAggregationState.lastSuccessEpochMs)
        assertEquals(50L, DeveloperDiagnosticsAggregationState.lastDurationMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastFailureEpochMs)
    }

    @Test
    fun `recordFailure sets failure timestamp and duration`() {
        DeveloperDiagnosticsAggregationState.recordStart(100L)
        DeveloperDiagnosticsAggregationState.recordFailure(200L, 100L)

        assertEquals(200L, DeveloperDiagnosticsAggregationState.lastFailureEpochMs)
        assertEquals(100L, DeveloperDiagnosticsAggregationState.lastDurationMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastSuccessEpochMs)
    }

    @Test
    fun `a later successful run overwrites an earlier failure`() {
        DeveloperDiagnosticsAggregationState.recordFailure(100L, 10L)
        DeveloperDiagnosticsAggregationState.recordSuccess(200L, 20L)

        assertEquals(200L, DeveloperDiagnosticsAggregationState.lastSuccessEpochMs)
        assertEquals(20L, DeveloperDiagnosticsAggregationState.lastDurationMs)
        // The earlier failure timestamp is preserved (each field is its own
        // independent "last" - a success does not erase a prior failure).
        assertEquals(100L, DeveloperDiagnosticsAggregationState.lastFailureEpochMs)
    }

    @Test
    fun `recordRetentionDeletion records the deleted row count`() {
        DeveloperDiagnosticsAggregationState.recordRetentionDeletion(42)
        assertEquals(42, DeveloperDiagnosticsAggregationState.lastRetentionDeletedRows)
    }

    @Test
    fun `reset clears every field`() {
        DeveloperDiagnosticsAggregationState.recordStart(1L)
        DeveloperDiagnosticsAggregationState.recordSuccess(2L, 1L)
        DeveloperDiagnosticsAggregationState.recordRetentionDeletion(5)

        DeveloperDiagnosticsAggregationState.reset()

        assertNull(DeveloperDiagnosticsAggregationState.lastStartEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastSuccessEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastFailureEpochMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastDurationMs)
        assertNull(DeveloperDiagnosticsAggregationState.lastRetentionDeletedRows)
    }
}
