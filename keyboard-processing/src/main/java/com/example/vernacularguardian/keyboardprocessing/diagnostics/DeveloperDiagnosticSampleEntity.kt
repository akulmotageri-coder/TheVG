package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One low-frequency (default every 5 minutes) developer resource sample
 * (Sprint 8): CPU, memory, battery, thermal, and process-uptime telemetry.
 * Stored only in `developer_diagnostics.db`. Nullable fields mean "not
 * available on this device/API level" (see [DeveloperDiagnosticsResourceMonitor])
 * rather than a fabricated zero.
 *
 * [cpuUtilizationPercent] can exceed 100.0 on a multi-core device (it is a
 * ratio of process CPU time to wall-clock time, not per-core normalized) —
 * intentionally not clamped; see [DeveloperDiagnosticsResourceMonitor].
 */
@Entity(tableName = "diagnostic_samples")
data class DeveloperDiagnosticSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampEpochMs: Long,
    val processCpuTimeMs: Long?,
    val wallClockElapsedMs: Long,
    val cpuUtilizationPercent: Double?,
    val totalPssKb: Int,
    val javaHeapKb: Int?,
    val nativeHeapKb: Int?,
    val privateDirtyKb: Int?,
    val batteryPercent: Int?,
    val isCharging: Boolean?,
    val batteryTemperatureTenthsC: Int?,
    val thermalStatus: Int?,
    val processUptimeMs: Long?
)
