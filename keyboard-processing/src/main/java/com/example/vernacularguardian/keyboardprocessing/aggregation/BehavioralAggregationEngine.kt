package com.example.vernacularguardian.keyboardprocessing.aggregation

import android.util.Log
import androidx.room.withTransaction
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticsAggregationState
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType
import com.example.vernacularguardian.keyboardprocessing.storage.DailySummaryEntity
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import com.example.vernacularguardian.keyboardprocessing.storage.SessionDailyAggregate
import java.time.LocalDate
import java.time.ZoneId

/**
 * The single aggregation + retention implementation shared by
 * [BehavioralAggregationWorker] (scheduled, every 24h) and any internal
 * on-demand `runOnce()` caller (demo/validation). Reads only persisted
 * [com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionEntity]
 * rows — never raw accessibility events, text, or app identity — and never
 * recomputes anything [com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTracker]
 * already produced.
 */
class BehavioralAggregationEngine(private val database: KeyboardProcessingDatabase) {

    /**
     * Aggregates all days with qualifying sessions into `daily_summaries`,
     * then deletes `typing_sessions` rows older than the 30-day retention
     * cutoff — in that order, and atomically: the whole run is one Room
     * transaction, so a failure partway through aggregation rolls back
     * everything, including any retention deletion, rather than leaving a
     * partially-aggregated, partially-purged database.
     */
    suspend fun runOnce(): Boolean {
        // Sprint 8 observational hooks throughout this method: purely additive
        // telemetry (in-memory counters/timestamps only) around the exact same
        // transaction, control flow, and return value as before.
        val startEpochMs = System.currentTimeMillis()
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.AGGREGATION_STARTED)
        DeveloperDiagnosticsAggregationState.recordStart(startEpochMs)
        return try {
            database.withTransaction {
                aggregate()
                runRetention()
            }
            val nowEpochMs = System.currentTimeMillis()
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.AGGREGATION_SUCCESS)
            DeveloperDiagnosticsAggregationState.recordSuccess(nowEpochMs, nowEpochMs - startEpochMs)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Aggregation run failed: ${e.javaClass.simpleName}: ${e.message}")
            val nowEpochMs = System.currentTimeMillis()
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.AGGREGATION_FAILURE)
            DeveloperDiagnosticsAggregationState.recordFailure(nowEpochMs, nowEpochMs - startEpochMs)
            false
        }
    }

    private suspend fun aggregate() {
        val dailyAggregates = database.typingSessionDao().getDailyAggregates()
        for (dailyAggregate in dailyAggregates) {
            database.dailySummaryDao().upsert(dailyAggregate.toDailySummaryEntity())
        }
    }

    private suspend fun runRetention() {
        val cutoffEpochDayExclusive = todayEpochDay() - RETENTION_DAYS
        val deletedCount = database.typingSessionDao().deleteSessionsOlderThan(cutoffEpochDayExclusive)
        // Sprint 8 observational hook: the deleted-row count was already
        // returned by this existing DAO method and previously discarded;
        // this only additionally records it for the diagnostics dashboard.
        DeveloperDiagnosticsAggregationState.recordRetentionDeletion(deletedCount)
    }

    companion object {
        private const val TAG = "BehavioralAggregation"

        /** PRD: raw `typing_sessions` rows older than 30 days are deleted after aggregation. */
        const val RETENTION_DAYS = 30L

        fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    }
}

private fun SessionDailyAggregate.toDailySummaryEntity(): DailySummaryEntity {
    // PRD: total micro-pauses normalized per 100 typed characters —
    // SUM(microPauseCount) / SUM(charCount) * 100, not an average of ratios.
    // Zero-character days are a defensive decision (not PRD-specified): 0.0,
    // to avoid a division by zero while still producing a numeric Double.
    val pauseFrequencyPer100Chars = if (totalCharCount == 0L) {
        0.0
    } else {
        totalMicroPauseCount.toDouble() / totalCharCount.toDouble() * 100.0
    }

    val riskContributionScore = RiskScoring.calculate(
        avgBackspaceRate = avgBackspaceRate,
        pauseFrequencyPer100Chars = pauseFrequencyPer100Chars,
        intervalStdDevMs = avgIntervalStdDevMs
    )

    return DailySummaryEntity(
        dateEpochDay = sessionEpochDay,
        avgTypingSpeedCpm = avgTypingSpeedCpm,
        avgBackspaceRate = avgBackspaceRate,
        intervalStdDevMs = avgIntervalStdDevMs,
        pauseFrequencyPer100Chars = pauseFrequencyPer100Chars,
        sessionCount = sessionCount,
        riskContributionScore = riskContributionScore,
        // A row is only ever generated from an existing group of >=1 real
        // sessions (SQL GROUP BY never emits an empty group), so this is
        // always true for a generated row; the false state is preserved by
        // the model for a zero-session day, which simply never gets a row.
        isReliable = sessionCount > 0
    )
}
