package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

data class ErrorBreakdownRow(
    val eventType: String,
    val count: Long,
    val lastOccurrence: Long?
)

@Dao
interface DeveloperDiagnosticsDao {

    @Insert
    suspend fun insertEvent(event: DeveloperDiagnosticEventEntity): Long

    @Insert
    suspend fun insertSample(sample: DeveloperDiagnosticSampleEntity): Long

    @Query("SELECT COUNT(*) FROM diagnostic_events WHERE eventType = :eventType")
    suspend fun countEventsByType(eventType: String): Long

    @Query("SELECT COUNT(*) FROM diagnostic_events WHERE eventType = :eventType AND timestampEpochMs >= :sinceEpochMs")
    suspend fun countEventsByTypeSince(eventType: String, sinceEpochMs: Long): Long

    @Query("SELECT MAX(timestampEpochMs) FROM diagnostic_events WHERE eventType = :eventType")
    suspend fun latestTimestampByType(eventType: String): Long?

    @Query("SELECT * FROM diagnostic_events ORDER BY timestampEpochMs DESC LIMIT :limit")
    suspend fun latestEvents(limit: Int): List<DeveloperDiagnosticEventEntity>

    @Query("SELECT * FROM diagnostic_samples ORDER BY timestampEpochMs DESC LIMIT :limit")
    suspend fun latestSamples(limit: Int): List<DeveloperDiagnosticSampleEntity>

    @Query(
        """
        SELECT eventType, COUNT(*) AS count, MAX(timestampEpochMs) AS lastOccurrence
        FROM diagnostic_events
        WHERE eventType LIKE 'ERROR\_%' ESCAPE '\'
        GROUP BY eventType
        """
    )
    suspend fun errorBreakdown(): List<ErrorBreakdownRow>

    @Query("SELECT COUNT(*) FROM diagnostic_events")
    suspend fun totalEventCount(): Long

    @Query("SELECT COUNT(*) FROM diagnostic_samples")
    suspend fun totalSampleCount(): Long

    @Query("DELETE FROM diagnostic_events WHERE timestampEpochMs < :cutoffEpochMs")
    suspend fun deleteEventsOlderThan(cutoffEpochMs: Long): Int

    @Query("DELETE FROM diagnostic_samples WHERE timestampEpochMs < :cutoffEpochMs")
    suspend fun deleteSamplesOlderThan(cutoffEpochMs: Long): Int

    @Query("DELETE FROM diagnostic_events")
    suspend fun clearEvents()

    @Query("DELETE FROM diagnostic_samples")
    suspend fun clearSamples()
}
