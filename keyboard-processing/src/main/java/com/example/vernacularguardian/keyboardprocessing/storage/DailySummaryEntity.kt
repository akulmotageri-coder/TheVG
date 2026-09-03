package com.example.vernacularguardian.keyboardprocessing.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one day's aggregated behavioral summary. Written by
 * [com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine]
 * (Sprint 4); Sprint 3 only created this table's schema.
 */
@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val dateEpochDay: Long,
    val avgTypingSpeedCpm: Double,
    val avgBackspaceRate: Double,
    val intervalStdDevMs: Double,
    val pauseFrequencyPer100Chars: Double,
    val sessionCount: Int,
    val riskContributionScore: Double,
    val isReliable: Boolean
)
