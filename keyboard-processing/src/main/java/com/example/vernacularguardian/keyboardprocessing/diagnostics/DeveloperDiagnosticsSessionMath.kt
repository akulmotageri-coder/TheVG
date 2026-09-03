package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Pure arithmetic extracted from [DeveloperDiagnosticsProductDataAdapter] so
 * it is deterministically unit testable on the JVM, mirroring
 * [DeveloperDiagnosticsResourceMath]'s pattern for the same reason. Zero
 * `android.*` imports.
 *
 * Sprint 9 fix: [averageIntOrNull]/[averageDoubleOrNull]/[pauseFrequencyPer100Chars]
 * return `null` for "no data" instead of a fabricated `0.0` — the Sprint 8
 * diagnostics-fabrication audit found the previous zero-on-empty behavior
 * inconsistent with `minCharCount`/`maxCharCount` in the same
 * [SessionAnalyticsSnapshot], which already correctly returned `null` (shown
 * as "-" on the dashboard) for the same "no sessions yet" case.
 */
object DeveloperDiagnosticsSessionMath {

    fun averageIntOrNull(values: List<Int>): Double? = if (values.isEmpty()) null else values.average()

    fun averageDoubleOrNull(values: List<Double>): Double? = if (values.isEmpty()) null else values.average()

    /**
     * SUM(microPauseCount) / SUM(charCount) * 100 — [BehavioralAggregationEngine]'s
     * own per-day formula, applied globally instead of grouped by day. `null`
     * when there are no characters to divide by (no sessions, or all-empty
     * sessions), never a fabricated `0.0`.
     */
    fun pauseFrequencyPer100Chars(totalChars: Long, totalMicroPauses: Long): Double? {
        if (totalChars == 0L) return null
        return totalMicroPauses.toDouble() / totalChars.toDouble() * 100.0
    }
}
