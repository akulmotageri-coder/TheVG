package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Pure Room-entity -> dashboard-DTO mapping, extracted from
 * [DeveloperDiagnosticsRepository] so it is deterministically unit testable
 * (Sprint 8 Section 33 item 5) without needing an Android [android.content.Context]
 * to construct that class. No Android dependency.
 */

fun DeveloperDiagnosticEventEntity.toDiagnosticEventRow() = DiagnosticEventRow(
    timestampEpochMs = timestampEpochMs,
    eventType = eventType,
    valueLong = valueLong,
    valueDouble = valueDouble,
    durationMs = durationMs
)

private const val CPU_AVAILABLE_NOTE = "processCpuTimeMs / wallClockElapsedMs ratio since the previous sample; " +
    "not clamped to 100% (can exceed it on multi-core devices)."

private const val CPU_UNAVAILABLE_NOTE = "Process CPU time unavailable on this device/API (Android restricts " +
    "per-process /proc access starting around API 26)."

fun DeveloperDiagnosticSampleEntity.toResourceSnapshot(peakTotalPssKb: Int?, averageTotalPssKb: Double?) = ResourceSnapshot(
    timestampEpochMs = timestampEpochMs,
    processCpuTimeMs = processCpuTimeMs,
    cpuUtilizationPercent = cpuUtilizationPercent,
    cpuMeasurementNote = if (processCpuTimeMs != null) CPU_AVAILABLE_NOTE else CPU_UNAVAILABLE_NOTE,
    totalPssKb = totalPssKb.takeIf { it > 0 },
    javaHeapKb = javaHeapKb,
    nativeHeapKb = nativeHeapKb,
    privateDirtyKb = privateDirtyKb,
    peakTotalPssKb = peakTotalPssKb,
    averageTotalPssKb = averageTotalPssKb,
    processUptimeMs = processUptimeMs?.takeIf { it >= 0L }
)
