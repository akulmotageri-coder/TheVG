package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock

/**
 * Captures one point-in-time developer resource sample: CPU, memory,
 * battery, thermal status, and process uptime (Sprint 8, Sections 19-23).
 *
 * Every Android API call here is wrapped so an unsupported/restricted API
 * on a given device or Android version degrades to `null` ("not available")
 * rather than crashing (Section 38: must run safely on a real physical
 * phone, not just the emulator this was validated on).
 *
 * Holds no reference to any product class — a pure resource reader with a
 * [Context] dependency, entirely separate from
 * [com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTracker]
 * or any other product logic.
 */
class DeveloperDiagnosticsResourceMonitor(private val context: Context) {

    private val appContext = context.applicationContext
    private val activityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    // Previous-sample state for CPU utilization delta computation. Single
    // instance per process (owned by DeveloperDiagnosticsRepository), so
    // consecutive captureSample() calls see a consistent previous point.
    @Volatile
    private var previousCpuTimeMs: Long? = null

    @Volatile
    private var previousWallClockMs: Long? = null

    fun captureSample(): DeveloperDiagnosticSampleEntity {
        val nowEpochMs = System.currentTimeMillis()
        // Sprint 9 clarity fix: this is boot-relative elapsed-realtime, not
        // wall-clock - named accordingly here even though the persisted
        // entity column it maps to (DeveloperDiagnosticSampleEntity.wallClockElapsedMs)
        // keeps its existing (misleading, but Room-schema-frozen) name.
        val elapsedRealtimeMs = SystemClock.elapsedRealtime()

        val cpuTimeMs = readProcessCpuTimeMs()
        val cpuUtilizationPercent = computeUtilizationPercent(cpuTimeMs, elapsedRealtimeMs)

        val memory = readMemoryInfo()
        val battery = readBatteryInfo()
        val thermalStatus = readThermalStatus()
        val processUptimeMs = readProcessUptimeMs(elapsedRealtimeMs)

        return DeveloperDiagnosticSampleEntity(
            timestampEpochMs = nowEpochMs,
            processCpuTimeMs = cpuTimeMs,
            wallClockElapsedMs = elapsedRealtimeMs,
            cpuUtilizationPercent = cpuUtilizationPercent,
            // Entity uses 0 as the "unavailable" sentinel for this one field
            // (see DeveloperDiagnosticsMapping.toResourceSnapshot, which maps
            // a non-positive value back to null for the dashboard DTO).
            totalPssKb = memory.totalPssKb ?: 0,
            javaHeapKb = memory.javaHeapKb,
            nativeHeapKb = memory.nativeHeapKb,
            privateDirtyKb = memory.privateDirtyKb,
            batteryPercent = battery.percent,
            isCharging = battery.isCharging,
            batteryTemperatureTenthsC = battery.temperatureTenthsC,
            thermalStatus = thermalStatus,
            processUptimeMs = processUptimeMs
        )
    }

    /**
     * [android.os.Process.getElapsedCpuTime] is officially documented as no
     * longer reliable starting with API 26 (many devices always return 0
     * due to restricted `/proc` access). A `null` or non-positive result is
     * therefore treated as "unavailable" rather than a real zero-CPU
     * reading — see [ResourceSnapshot.cpuMeasurementNote] for the exact
     * user-facing wording.
     */
    private fun readProcessCpuTimeMs(): Long? = try {
        val value = Process.getElapsedCpuTime()
        if (value > 0L) value else null
    } catch (e: Exception) {
        null
    }

    /**
     * Ratio of process CPU time consumed to wall-clock time elapsed between
     * this call and the previous one, as a percentage; the actual arithmetic
     * lives in [DeveloperDiagnosticsResourceMath.cpuUtilizationPercent] so it
     * is deterministically unit testable. Deliberately NOT clamped to 100: a
     * multi-threaded process on a multi-core device can legitimately consume
     * more than one core's worth of wall-clock time (Section 20).
     */
    private fun computeUtilizationPercent(cpuTimeMs: Long?, wallClockMs: Long): Double? {
        val previousCpu = previousCpuTimeMs
        val previousWallClock = previousWallClockMs
        previousCpuTimeMs = cpuTimeMs
        previousWallClockMs = wallClockMs
        return DeveloperDiagnosticsResourceMath.cpuUtilizationPercent(
            previousCpuTimeMs = previousCpu,
            previousWallClockMs = previousWallClock,
            currentCpuTimeMs = cpuTimeMs,
            currentWallClockMs = wallClockMs
        )
    }

    private data class MemoryReading(
        val totalPssKb: Int?,
        val javaHeapKb: Int?,
        val nativeHeapKb: Int?,
        val privateDirtyKb: Int?
    )

    private fun readMemoryInfo(): MemoryReading = try {
        val manager = activityManager
        val info = manager?.getProcessMemoryInfo(intArrayOf(Process.myPid()))?.firstOrNull()
        if (info == null) {
            MemoryReading(null, null, null, null)
        } else {
            MemoryReading(
                totalPssKb = info.totalPss,
                javaHeapKb = runCatching { info.getMemoryStat("summary.java-heap").toInt() }.getOrNull(),
                nativeHeapKb = runCatching { info.getMemoryStat("summary.native-heap").toInt() }.getOrNull(),
                privateDirtyKb = runCatching { info.totalPrivateDirty }.getOrNull()
            )
        }
    } catch (e: Exception) {
        MemoryReading(null, null, null, null)
    }

    private data class BatteryReading(
        val percent: Int?,
        val isCharging: Boolean?,
        val temperatureTenthsC: Int?
    )

    /**
     * Reads the sticky `ACTION_BATTERY_CHANGED` intent (registering a `null`
     * receiver for a sticky action synchronously returns the last broadcast
     * without needing a live registration) — the standard, crash-safe way to
     * read device battery state on demand. Device-level telemetry only; see
     * [BatterySnapshot.label] — this module never claims a per-app battery
     * consumption estimate.
     */
    private fun readBatteryInfo(): BatteryReading = try {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent == null) {
            BatteryReading(null, null, null)
        } else {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percent = DeveloperDiagnosticsResourceMath.batteryPercent(level, scale)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = DeveloperDiagnosticsResourceMath.isCharging(status)
            val temperatureTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                .takeIf { it != Int.MIN_VALUE }
            BatteryReading(percent, isCharging, temperatureTenths)
        }
    } catch (e: Exception) {
        BatteryReading(null, null, null)
    }

    /** Android Thermal API (API 29+ only); `null` below that or if unsupported. */
    private fun readThermalStatus(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            (appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.currentThermalStatus
        } catch (e: Exception) {
            null
        }
    }

    private fun readProcessUptimeMs(wallClockElapsedMs: Long): Long? = try {
        val startElapsedRealtime = Process.getStartElapsedRealtime()
        (wallClockElapsedMs - startElapsedRealtime).takeIf { it >= 0L }
    } catch (e: Exception) {
        null
    }

    /**
     * Sprint 9 (master prompt Section 6): real
     * [PowerManager.isIgnoringBatteryOptimizations] read for this app's own
     * package. `null` only if the `PowerManager` system service itself is
     * unavailable/the read fails — never a fabricated `true`/`false`. OEM
     * battery management (documented for MIUI in particular) killing a
     * backgrounded app that has NOT been exempted from battery optimization
     * is a real, well-documented cause of an AccessibilityService being
     * stopped outside the app's control; this is the on-device evidence
     * needed to confirm or rule that out for a specific device, rather than
     * guessing.
     */
    fun isIgnoringBatteryOptimizations(): Boolean? = try {
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isIgnoringBatteryOptimizations(appContext.packageName)
    } catch (e: Exception) {
        null
    }
}
