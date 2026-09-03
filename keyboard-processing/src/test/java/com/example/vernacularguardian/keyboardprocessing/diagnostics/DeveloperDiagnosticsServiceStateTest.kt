package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deterministic tests for [DeveloperDiagnosticsServiceState] (Sprint 8
 * Section 33 items 3-4: counter increment/reset behavior). Pure Kotlin, no
 * Android dependency.
 */
class DeveloperDiagnosticsServiceStateTest {

    @After
    fun resetSingletonState() {
        DeveloperDiagnosticsServiceState.reset()
    }

    @Test
    fun `initial state is disconnected with zero counts`() {
        assertEquals(DeveloperDiagnosticsServiceState.State.DISCONNECTED, DeveloperDiagnosticsServiceState.state)
        assertEquals(0L, DeveloperDiagnosticsServiceState.connectionCount)
        assertEquals(0L, DeveloperDiagnosticsServiceState.destroyCount)
    }

    @Test
    fun `recordConnected sets state to connected and increments connection count`() {
        DeveloperDiagnosticsServiceState.recordConnected(1000L)

        assertEquals(DeveloperDiagnosticsServiceState.State.CONNECTED, DeveloperDiagnosticsServiceState.state)
        assertEquals(1L, DeveloperDiagnosticsServiceState.connectionCount)
        assertEquals(1000L, DeveloperDiagnosticsServiceState.lastConnectedEpochMs)
    }

    @Test
    fun `recordDestroyed sets state to disconnected and increments destroy count`() {
        DeveloperDiagnosticsServiceState.recordConnected(1000L)
        DeveloperDiagnosticsServiceState.recordDestroyed(2000L)

        assertEquals(DeveloperDiagnosticsServiceState.State.DISCONNECTED, DeveloperDiagnosticsServiceState.state)
        assertEquals(1L, DeveloperDiagnosticsServiceState.destroyCount)
        assertEquals(2000L, DeveloperDiagnosticsServiceState.lastDisconnectedEpochMs)
    }

    @Test
    fun `repeated reconnects accumulate the connection count`() {
        DeveloperDiagnosticsServiceState.recordConnected(1L)
        DeveloperDiagnosticsServiceState.recordDestroyed(2L)
        DeveloperDiagnosticsServiceState.recordConnected(3L)
        DeveloperDiagnosticsServiceState.recordDestroyed(4L)
        DeveloperDiagnosticsServiceState.recordConnected(5L)

        assertEquals(3L, DeveloperDiagnosticsServiceState.connectionCount)
        assertEquals(2L, DeveloperDiagnosticsServiceState.destroyCount)
        assertEquals(DeveloperDiagnosticsServiceState.State.CONNECTED, DeveloperDiagnosticsServiceState.state)
        assertEquals(5L, DeveloperDiagnosticsServiceState.lastConnectedEpochMs)
    }

    @Test
    fun `reset clears state back to the initial disconnected values`() {
        DeveloperDiagnosticsServiceState.recordConnected(10L)
        DeveloperDiagnosticsServiceState.recordDestroyed(20L)

        DeveloperDiagnosticsServiceState.reset()

        assertEquals(DeveloperDiagnosticsServiceState.State.DISCONNECTED, DeveloperDiagnosticsServiceState.state)
        assertEquals(0L, DeveloperDiagnosticsServiceState.connectionCount)
        assertEquals(0L, DeveloperDiagnosticsServiceState.destroyCount)
        assertEquals(null, DeveloperDiagnosticsServiceState.lastConnectedEpochMs)
        assertEquals(null, DeveloperDiagnosticsServiceState.lastDisconnectedEpochMs)
    }
}
