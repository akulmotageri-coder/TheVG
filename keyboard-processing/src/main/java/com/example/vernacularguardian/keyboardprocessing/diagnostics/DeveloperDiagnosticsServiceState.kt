package com.example.vernacularguardian.keyboardprocessing.diagnostics

import java.util.concurrent.atomic.AtomicLong

/**
 * Pure, in-memory, process-wide mirror of
 * [com.example.vernacularguardian.keyboardprocessing.service.KeystrokeAccessibilityService]'s
 * lifecycle, observed via purely-additive hooks in that class. No Android
 * dependency, so it is deterministically unit testable. Diagnostics only
 * observe this — nothing here feeds back into service behavior.
 */
object DeveloperDiagnosticsServiceState {

    enum class State { CONNECTED, DISCONNECTED }

    @Volatile
    var state: State = State.DISCONNECTED
        private set

    // Hardening-pass fix: was a bare @Volatile var incremented via `x++`,
    // which is visibility-only, not atomicity - a genuine (if rare) race
    // between concurrent recordConnected/recordDestroyed calls could
    // silently lose an increment. AtomicLong matches the pattern already
    // used for the same purpose in DeveloperDiagnosticCounters.
    private val connectionCountValue = AtomicLong(0L)
    private val destroyCountValue = AtomicLong(0L)

    val connectionCount: Long get() = connectionCountValue.get()
    val destroyCount: Long get() = destroyCountValue.get()

    @Volatile
    var lastConnectedEpochMs: Long? = null
        private set

    @Volatile
    var lastDisconnectedEpochMs: Long? = null
        private set

    fun recordConnected(epochMs: Long) {
        state = State.CONNECTED
        connectionCountValue.incrementAndGet()
        lastConnectedEpochMs = epochMs
    }

    fun recordDestroyed(epochMs: Long) {
        state = State.DISCONNECTED
        destroyCountValue.incrementAndGet()
        lastDisconnectedEpochMs = epochMs
    }

    /** Test/clear-diagnostics-history support only; never called from product code paths. */
    fun reset() {
        state = State.DISCONNECTED
        connectionCountValue.set(0L)
        destroyCountValue.set(0L)
        lastConnectedEpochMs = null
        lastDisconnectedEpochMs = null
    }
}
