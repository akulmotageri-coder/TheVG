package com.example.vernacularguardian.keyboardprocessing.diagnostics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide, in-memory-only counters for every [DiagnosticEventType].
 * Pure Kotlin (no Android dependency) so it is deterministically unit
 * testable on the JVM, exactly like [com.example.vernacularguardian.keyboardprocessing.owner.OwnerSessionManager].
 *
 * Incrementing here is always safe and unconditional (a handful of atomic
 * map/counter operations) regardless of whether developer diagnostics are
 * enabled — the enable/disable toggle only controls whether
 * [DeveloperDiagnosticsRepository] additionally persists a compact event row
 * for the sparser boundary/lifecycle event types. This is what lets
 * high-frequency call sites (e.g. every accessibility text-change event)
 * call [increment] directly without any per-event database write.
 *
 * [seed] lets [DeveloperDiagnosticsRepository] restore cumulative totals
 * from the persisted `developer_diagnostics.db` at process start, so
 * "since app install" counts survive a process restart even though this
 * object itself starts at zero every time.
 */
object DeveloperDiagnosticCounters {

    private val counters = ConcurrentHashMap<DiagnosticEventType, AtomicLong>()

    private fun counterFor(type: DiagnosticEventType): AtomicLong =
        counters.getOrPut(type) { AtomicLong(0L) }

    fun increment(type: DiagnosticEventType, amount: Long = 1L): Long =
        counterFor(type).addAndGet(amount)

    fun seed(type: DiagnosticEventType, value: Long) {
        counterFor(type).set(value)
    }

    fun get(type: DiagnosticEventType): Long = counters[type]?.get() ?: 0L

    fun snapshot(): Map<DiagnosticEventType, Long> =
        DiagnosticEventType.entries.associateWith { get(it) }

    /** Resets every counter to zero. Used by "Clear diagnostics history". */
    fun reset() {
        counters.clear()
    }
}
