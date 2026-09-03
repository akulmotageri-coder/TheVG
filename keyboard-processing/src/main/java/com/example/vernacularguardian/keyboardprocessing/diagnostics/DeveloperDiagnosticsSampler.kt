package com.example.vernacularguardian.keyboardprocessing.diagnostics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Low-frequency, lifecycle-managed periodic resource sampler (Sprint 8
 * Sections 19/30/31): a single coroutine loop that sleeps [SAMPLE_INTERVAL_MS]
 * between samples — never a tight/busy loop, never once-per-second. Only
 * runs while [start] has been called (i.e. diagnostics are enabled);
 * [stop] cancels it immediately and is always safe to call even if never
 * started.
 *
 * Keeps only O(1) aggregate state (peak/sum/count) for the current process
 * lifetime rather than an in-memory history list — the durable sample
 * history lives in `developer_diagnostics.db` via [onSample].
 */
class DeveloperDiagnosticsSampler(
    private val monitor: DeveloperDiagnosticsResourceMonitor,
    private val onSample: suspend (DeveloperDiagnosticSampleEntity) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    private val _latest = MutableStateFlow<DeveloperDiagnosticSampleEntity?>(null)
    val latest: StateFlow<DeveloperDiagnosticSampleEntity?> = _latest.asStateFlow()

    @Volatile
    private var peakTotalPssKb: Int = 0

    @Volatile
    private var pssSampleSum: Long = 0L

    @Volatile
    private var pssSampleCount: Long = 0L

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                sampleNow()
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        // Hardening-pass fix: without this, buildSnapshot()'s
        // `sampler.latest.value ?: resourceMonitor.captureSample()` fallback
        // would keep reusing this frozen last-known sample indefinitely on
        // every future refresh (however many hours/days later diagnostics
        // happens to be reopened) instead of falling back to a fresh
        // capture, with nothing on screen but the small-print sample
        // timestamp to reveal it was stale.
        _latest.value = null
    }

    /** Captures and records one sample immediately, independent of the periodic loop's schedule. */
    suspend fun sampleNow(): DeveloperDiagnosticSampleEntity {
        val sample = monitor.captureSample()
        _latest.value = sample
        val pss = sample.totalPssKb
        if (pss > 0) {
            peakTotalPssKb = maxOf(peakTotalPssKb, pss)
            pssSampleSum += pss
            pssSampleCount++
        }
        onSample(sample)
        return sample
    }

    /**
     * Sprint 9 fabrication-audit fix: `null` (never a fabricated `0`) until
     * at least one valid (positive) PSS reading has actually been captured
     * this process lifetime - mirrors the neighboring
     * [DeveloperDiagnosticSampleEntity.totalPssKb] -> `null` mapping in
     * [toResourceSnapshot], which this aggregate previously did not match.
     */
    fun peakTotalPssKb(): Int? = if (pssSampleCount == 0L) null else peakTotalPssKb

    fun averageTotalPssKb(): Double? =
        if (pssSampleCount == 0L) null else pssSampleSum.toDouble() / pssSampleCount

    /** Used by "Clear diagnostics history" — resets this-process aggregate stats only. */
    fun resetStats() {
        peakTotalPssKb = 0
        pssSampleSum = 0L
        pssSampleCount = 0L
        _latest.value = null
    }

    companion object {
        const val SAMPLE_INTERVAL_MS = 5 * 60 * 1000L
    }
}
