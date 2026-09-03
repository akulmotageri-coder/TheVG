package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic tests for [DeveloperDiagnosticCounters] (Sprint 8 Section
 * 33, items 2-5, 10). Pure Kotlin, no Android dependency.
 */
class DeveloperDiagnosticCountersTest {

    @After
    fun resetSingletonState() {
        DeveloperDiagnosticCounters.reset()
    }

    @Test
    fun `a fresh counter starts at zero`() {
        assertEquals(0L, DeveloperDiagnosticCounters.get(DiagnosticEventType.OWNER_PROMPT_SHOWN))
    }

    @Test
    fun `increment increases the counter by one by default`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_PROMPT_SHOWN)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_PROMPT_SHOWN)
        assertEquals(2L, DeveloperDiagnosticCounters.get(DiagnosticEventType.OWNER_PROMPT_SHOWN))
    }

    @Test
    fun `increment supports a custom amount`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEXT_CHANGED_TOTAL, amount = 5L)
        assertEquals(5L, DeveloperDiagnosticCounters.get(DiagnosticEventType.TEXT_CHANGED_TOTAL))
    }

    @Test
    fun `counters for different event types are independent`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_CONFIRMED)
        assertEquals(0L, DeveloperDiagnosticCounters.get(DiagnosticEventType.OWNER_REVOKED))
        assertEquals(1L, DeveloperDiagnosticCounters.get(DiagnosticEventType.OWNER_CONFIRMED))
    }

    @Test
    fun `seed overwrites the counter rather than adding to it`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.SESSION_PERSISTED, amount = 3L)
        DeveloperDiagnosticCounters.seed(DiagnosticEventType.SESSION_PERSISTED, 10L)
        assertEquals(10L, DeveloperDiagnosticCounters.get(DiagnosticEventType.SESSION_PERSISTED))
    }

    @Test
    fun `reset zeroes every counter`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.APP_SWITCH_BOUNDARY)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEARDOWN_BOUNDARY)

        DeveloperDiagnosticCounters.reset()

        assertEquals(0L, DeveloperDiagnosticCounters.get(DiagnosticEventType.APP_SWITCH_BOUNDARY))
        assertEquals(0L, DeveloperDiagnosticCounters.get(DiagnosticEventType.TEARDOWN_BOUNDARY))
    }

    @Test
    fun `snapshot contains every event type`() {
        val snapshot = DeveloperDiagnosticCounters.snapshot()
        assertEquals(DiagnosticEventType.entries.size, snapshot.size)
        assertTrue(DiagnosticEventType.entries.all { snapshot.containsKey(it) })
    }

    @Test
    fun `snapshot reflects increments made before it was taken`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.EVENT_FORWARDED_TO_TRACKER, amount = 4L)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.EVENT_DROPPED_UNCONFIRMED, amount = 2L)

        val snapshot = DeveloperDiagnosticCounters.snapshot()

        assertEquals(4L, snapshot[DiagnosticEventType.EVENT_FORWARDED_TO_TRACKER])
        assertEquals(2L, snapshot[DiagnosticEventType.EVENT_DROPPED_UNCONFIRMED])
    }

    @Test
    fun `snapshot sums for a derived aggregate (e g total inactivity boundaries)`() {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.INACTIVITY_BOUNDARY_CONFIRMED, amount = 3L)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.INACTIVITY_BOUNDARY_UNCONFIRMED, amount = 2L)

        val snapshot = DeveloperDiagnosticCounters.snapshot()
        val totalInactivityBoundaries =
            (snapshot[DiagnosticEventType.INACTIVITY_BOUNDARY_CONFIRMED] ?: 0L) +
                (snapshot[DiagnosticEventType.INACTIVITY_BOUNDARY_UNCONFIRMED] ?: 0L)

        assertEquals(5L, totalInactivityBoundaries)
    }

    @Test
    fun `isError is true only for ERROR_ prefixed event types`() {
        assertTrue(DiagnosticEventType.ERROR_DATABASE.isError)
        assertTrue(DiagnosticEventType.ERROR_WORKMANAGER.isError)
        assertTrue(!DiagnosticEventType.OWNER_CONFIRMED.isError)
    }
}
