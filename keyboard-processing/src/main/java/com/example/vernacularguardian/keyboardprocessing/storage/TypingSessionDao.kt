package com.example.vernacularguardian.keyboardprocessing.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * One calendar day's SQL-side aggregate over qualifying sessions, before any
 * Sprint 4 derived-metric calculation (pause frequency, risk score,
 * reliability) is applied in Kotlin.
 */
data class SessionDailyAggregate(
    val sessionEpochDay: Long,
    val avgTypingSpeedCpm: Double,
    val avgBackspaceRate: Double,
    val avgIntervalStdDevMs: Double,
    val totalMicroPauseCount: Long,
    val totalCharCount: Long,
    val sessionCount: Int
)

@Dao
interface TypingSessionDao {

    @Insert
    suspend fun insert(session: TypingSessionEntity): Long

    @Query("SELECT * FROM typing_sessions ORDER BY id ASC")
    suspend fun getAllSessions(): List<TypingSessionEntity>

    /**
     * Database-side aggregation, grouped by calendar day. Legacy Sprint 3
     * rows with a NULL sessionEpochDay are excluded (`IS NOT NULL`) — they
     * predate this field and must not be guessed into a day.
     */
    @Query(
        """
        SELECT sessionEpochDay,
               AVG(typingSpeedCpm) AS avgTypingSpeedCpm,
               AVG(backspaceRate) AS avgBackspaceRate,
               AVG(intervalStdDevMs) AS avgIntervalStdDevMs,
               SUM(microPauseCount) AS totalMicroPauseCount,
               SUM(charCount) AS totalCharCount,
               COUNT(*) AS sessionCount
        FROM typing_sessions
        WHERE sessionEpochDay IS NOT NULL
        GROUP BY sessionEpochDay
        """
    )
    suspend fun getDailyAggregates(): List<SessionDailyAggregate>

    /**
     * Retention: deletes sessions strictly older than [cutoffEpochDayExclusive]
     * (i.e. `sessionEpochDay < cutoffEpochDayExclusive`). Rows with a NULL
     * sessionEpochDay are never matched by this comparison (standard SQL NULL
     * semantics), so legacy Sprint 3 rows are left untouched rather than
     * guessed at.
     */
    @Query("DELETE FROM typing_sessions WHERE sessionEpochDay < :cutoffEpochDayExclusive")
    suspend fun deleteSessionsOlderThan(cutoffEpochDayExclusive: Long): Int
}
