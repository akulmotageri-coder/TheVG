package com.example.vernacularguardian.keyboardprocessing.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    /**
     * Insert-or-replace on the `dateEpochDay` primary key: re-running
     * aggregation for an already-summarized date overwrites that date's row
     * rather than creating a duplicate, making repeated aggregation
     * idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries ORDER BY dateEpochDay ASC")
    suspend fun getAllSummaries(): List<DailySummaryEntity>

    /**
     * Sprint 6: the single reactive query backing
     * `KeyboardProcessingApi.observeDailyBehavioralSummary()`. Room emits a
     * fresh value on every write to `daily_summaries` for this exact
     * `dateEpochDay` (i.e. every time aggregation upserts today's row) with
     * no polling. Emits `null` until today's row is first created.
     */
    @Query("SELECT * FROM daily_summaries WHERE dateEpochDay = :epochDay")
    fun observeByEpochDay(epochDay: Long): Flow<DailySummaryEntity?>
}
