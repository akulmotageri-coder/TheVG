package com.example.vernacularguardian.keyboardprocessing.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for one completed, qualifying typing session. Fields map directly
 * from [com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionResult] —
 * no metric is recalculated or reinterpreted here.
 *
 * [sessionEpochDay] is a Sprint 4 addition: [startTimeMs]/[endTimeMs] are
 * elapsedRealtime values (Sprint 1/2, unchanged) and cannot be converted to a
 * calendar date after a device reboot, so the calendar day is captured
 * separately, once, at persist time. Nullable because Sprint 3 rows migrated
 * into this schema have no such value and must not have one invented for
 * them — see [KeyboardProcessingDatabase]'s migration.
 */
@Entity(tableName = "typing_sessions")
data class TypingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val typingSpeedCpm: Double,
    val backspaceRate: Double,
    val intervalStdDevMs: Double,
    val microPauseCount: Int,
    val charCount: Int,
    val sessionEpochDay: Long?
)
