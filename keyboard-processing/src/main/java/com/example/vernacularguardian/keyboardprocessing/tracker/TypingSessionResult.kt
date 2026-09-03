package com.example.vernacularguardian.keyboardprocessing.tracker

/**
 * Result of a single completed typing session, as produced by [TypingSessionTracker].
 *
 * [startTimeMs] and [endTimeMs] are expected to be elapsed-realtime milliseconds
 * supplied by the caller; this type has no dependency on any Android clock API.
 */
data class TypingSessionResult(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val typingSpeedCpm: Double,
    val backspaceRate: Double,
    val intervalStdDevMs: Double,
    val microPauseCount: Int,
    val charCount: Int
)