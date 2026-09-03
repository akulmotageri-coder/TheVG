package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.content.Context
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine
import com.example.vernacularguardian.keyboardprocessing.storage.DailySummaryEntity
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionEntity

/**
 * Developer-only, read-only adapter over the existing product database
 * (Sprint 8 Section 3/16). Reads exclusively through
 * [KeyboardProcessingDatabase]'s existing [com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionDao.getAllSessions]
 * and [com.example.vernacularguardian.keyboardprocessing.storage.DailySummaryDao.getAllSummaries] —
 * the same DAO methods already used elsewhere in the module — and computes
 * every diagnostic statistic in Kotlin from those exact rows. No Sprint 1-4
 * formula is reimplemented or recalculated with different logic; the one
 * exception ([totalPauseFrequencyPer100Chars]) explicitly reuses
 * [BehavioralAggregationEngine]'s own SUM/SUM*100 formula, just applied
 * across all sessions instead of grouped per day.
 *
 * Never writes to `keyboard_processing.db` and never touches
 * [com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingApi].
 */
class DeveloperDiagnosticsProductDataAdapter(context: Context) {

    private val database = KeyboardProcessingDatabase.getInstance(context)
    private val appContext = context.applicationContext

    suspend fun allSessions(): List<TypingSessionEntity> = database.typingSessionDao().getAllSessions()

    suspend fun allDailySummaries(): List<DailySummaryEntity> = database.dailySummaryDao().getAllSummaries()

    fun sessionAnalytics(sessions: List<TypingSessionEntity>): SessionAnalyticsSnapshot {
        val today = BehavioralAggregationEngine.todayEpochDay()
        val todaySessions = sessions.count { it.sessionEpochDay == today }
        val last7dSessions = sessions.count { it.sessionEpochDay != null && it.sessionEpochDay >= today - 6 }

        return SessionAnalyticsSnapshot(
            totalSessions = sessions.size,
            todaySessions = todaySessions,
            sessionsLast7d = last7dSessions,
            avgCharCount = DeveloperDiagnosticsSessionMath.averageIntOrNull(sessions.map { it.charCount }),
            avgTypingSpeedCpm = DeveloperDiagnosticsSessionMath.averageDoubleOrNull(sessions.map { it.typingSpeedCpm }),
            avgBackspaceRate = DeveloperDiagnosticsSessionMath.averageDoubleOrNull(sessions.map { it.backspaceRate }),
            avgIntervalStdDevMs = DeveloperDiagnosticsSessionMath.averageDoubleOrNull(sessions.map { it.intervalStdDevMs }),
            avgMicroPauseCount = DeveloperDiagnosticsSessionMath.averageIntOrNull(sessions.map { it.microPauseCount }),
            avgPauseFrequencyPer100Chars = totalPauseFrequencyPer100Chars(sessions),
            minCharCount = sessions.minOfOrNull { it.charCount },
            maxCharCount = sessions.maxOfOrNull { it.charCount },
            minTypingSpeedCpm = sessions.minOfOrNull { it.typingSpeedCpm },
            maxTypingSpeedCpm = sessions.maxOfOrNull { it.typingSpeedCpm }
        )
    }

    fun sessionDetail(sessions: List<TypingSessionEntity>): List<SessionDetailRow> =
        sessions.map {
            SessionDetailRow(
                id = it.id,
                dateEpochDay = it.sessionEpochDay,
                startTimeMs = it.startTimeMs,
                endTimeMs = it.endTimeMs,
                charCount = it.charCount,
                typingSpeedCpm = it.typingSpeedCpm,
                backspaceRate = it.backspaceRate,
                intervalStdDevMs = it.intervalStdDevMs,
                microPauseCount = it.microPauseCount
            )
        }

    fun dailyAnalytics(summaries: List<DailySummaryEntity>): DailyAnalyticsSnapshot {
        val today = BehavioralAggregationEngine.todayEpochDay()
        val rows = summaries.map { it.toRow() }
        return DailyAnalyticsSnapshot(
            today = rows.firstOrNull { it.dateEpochDay == today },
            latest = rows.maxByOrNull { it.dateEpochDay },
            previousDays = rows.filter { it.dateEpochDay != today }.sortedByDescending { it.dateEpochDay }
        )
    }

    fun databaseSnapshot(
        sessions: List<TypingSessionEntity>,
        summaries: List<DailySummaryEntity>
    ): DatabaseSnapshot {
        val today = BehavioralAggregationEngine.todayEpochDay()
        val dbFile = appContext.getDatabasePath(DATABASE_FILE_NAME)
        val walFile = appContext.getDatabasePath("$DATABASE_FILE_NAME-wal")
        val shmFile = appContext.getDatabasePath("$DATABASE_FILE_NAME-shm")

        return DatabaseSnapshot(
            databaseName = DATABASE_FILE_NAME,
            databaseSizeBytes = if (dbFile.exists()) dbFile.length() else 0L,
            walSizeBytes = if (walFile.exists()) walFile.length() else null,
            shmSizeBytes = if (shmFile.exists()) shmFile.length() else null,
            typingSessionRowCount = sessions.size,
            dailySummaryRowCount = summaries.size,
            oldestSessionEpochDay = sessions.mapNotNull { it.sessionEpochDay }.minOrNull(),
            newestSessionEpochDay = sessions.mapNotNull { it.sessionEpochDay }.maxOrNull(),
            todaySessionCount = sessions.count { it.sessionEpochDay == today },
            todaySummaryExists = summaries.any { it.dateEpochDay == today },
            nullSessionEpochDayRows = sessions.count { it.sessionEpochDay == null },
            lastRetentionDeletedRows = DeveloperDiagnosticsAggregationState.lastRetentionDeletedRows,
            lastAggregationEpochMs = DeveloperDiagnosticsAggregationState.lastSuccessEpochMs
                ?: DeveloperDiagnosticsAggregationState.lastFailureEpochMs,
            lastAggregationSucceeded = when {
                DeveloperDiagnosticsAggregationState.lastSuccessEpochMs == null &&
                    DeveloperDiagnosticsAggregationState.lastFailureEpochMs == null -> null
                else -> (DeveloperDiagnosticsAggregationState.lastSuccessEpochMs ?: -1L) >=
                    (DeveloperDiagnosticsAggregationState.lastFailureEpochMs ?: -1L)
            }
        )
    }

    /**
     * SUM(microPauseCount) / SUM(charCount) * 100 across ALL sessions —
     * exactly [BehavioralAggregationEngine]'s own per-day formula, just
     * applied globally for diagnostic purposes rather than grouped by day.
     * `null` (never a fabricated `0.0`) when there are no characters to
     * divide by — see [DeveloperDiagnosticsSessionMath].
     */
    private fun totalPauseFrequencyPer100Chars(sessions: List<TypingSessionEntity>): Double? {
        val totalChars = sessions.sumOf { it.charCount.toLong() }
        val totalMicroPauses = sessions.sumOf { it.microPauseCount.toLong() }
        return DeveloperDiagnosticsSessionMath.pauseFrequencyPer100Chars(totalChars, totalMicroPauses)
    }

    private fun DailySummaryEntity.toRow() = DailySummaryRow(
        dateEpochDay = dateEpochDay,
        avgTypingSpeedCpm = avgTypingSpeedCpm,
        avgBackspaceRate = avgBackspaceRate,
        intervalStdDevMs = intervalStdDevMs,
        pauseFrequencyPer100Chars = pauseFrequencyPer100Chars,
        sessionCount = sessionCount,
        riskContributionScore = riskContributionScore,
        isReliable = isReliable
    )

    private companion object {
        const val DATABASE_FILE_NAME = "keyboard_processing.db"
    }
}
