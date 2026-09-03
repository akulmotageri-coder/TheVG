package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Retention window for `developer_diagnostics.db` (Sprint 8). Deliberately
 * separate from [com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine.RETENTION_DAYS]
 * (30 days, product typing-session retention) — this is a much shorter,
 * developer-only window sized for a several-day real-phone pilot, and
 * changing it never touches Sprint 4 retention logic.
 */
object DeveloperDiagnosticsRetention {

    const val RETENTION_DAYS = 7L

    private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

    /** Rows with a timestamp strictly before this cutoff are eligible for deletion. */
    fun cutoffEpochMs(nowEpochMs: Long): Long = nowEpochMs - RETENTION_DAYS * MS_PER_DAY
}
