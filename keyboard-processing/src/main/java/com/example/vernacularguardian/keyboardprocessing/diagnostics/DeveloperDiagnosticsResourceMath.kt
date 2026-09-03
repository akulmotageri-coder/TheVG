package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Pure arithmetic extracted from [DeveloperDiagnosticsResourceMonitor] so it
 * is deterministically unit testable on the JVM with no Android runtime.
 * Deliberately has zero `android.*` imports — even the battery status
 * constants below are copied literal values (documented against their
 * [android.os.BatteryManager] source), not a live Android reference.
 */
object DeveloperDiagnosticsResourceMath {

    // Mirrors android.os.BatteryManager.BATTERY_STATUS_CHARGING (2) and
    // BATTERY_STATUS_FULL (5).
    private const val BATTERY_STATUS_CHARGING = 2
    private const val BATTERY_STATUS_FULL = 5

    /**
     * Percentage ratio of CPU time consumed to wall-clock time elapsed
     * between two samples. Returns `null` when either sample's CPU time is
     * unavailable, when no wall-clock time has actually elapsed, or when the
     * CPU time counter went backwards (a rare counter reset) — never a
     * fabricated number in those cases. Deliberately NOT clamped to 100: a
     * multi-threaded process on a multi-core device can legitimately exceed
     * one core's worth of wall-clock time (Sprint 8 Section 20).
     */
    fun cpuUtilizationPercent(
        previousCpuTimeMs: Long?,
        previousWallClockMs: Long?,
        currentCpuTimeMs: Long?,
        currentWallClockMs: Long
    ): Double? {
        if (currentCpuTimeMs == null || previousCpuTimeMs == null || previousWallClockMs == null) return null
        val wallDeltaMs = currentWallClockMs - previousWallClockMs
        if (wallDeltaMs <= 0L) return null
        val cpuDeltaMs = currentCpuTimeMs - previousCpuTimeMs
        if (cpuDeltaMs < 0L) return null
        return (cpuDeltaMs.toDouble() / wallDeltaMs.toDouble()) * 100.0
    }

    /** `EXTRA_LEVEL`/`EXTRA_SCALE` -> whole-number battery percent, or `null` if either is invalid. */
    fun batteryPercent(level: Int, scale: Int): Int? =
        if (level >= 0 && scale > 0) (level * 100) / scale else null

    /** `EXTRA_STATUS` -> whether the device is charging or already full. */
    fun isCharging(status: Int): Boolean =
        status == BATTERY_STATUS_CHARGING || status == BATTERY_STATUS_FULL
}
