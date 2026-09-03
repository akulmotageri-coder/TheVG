package com.example.vernacularguardian.keyboardprocessing.tracker

import kotlin.math.sqrt

/**
 * Pure-Kotlin, in-memory tracker for a single active typing session.
 *
 * Callers feed qualifying edit events via [onEdit] and signal explicit session
 * boundaries (foreground app change, service teardown) via [endSession]. All
 * timing is driven by caller-supplied timestamps (elapsed-realtime milliseconds
 * in production use) — this class never reads a clock itself, so it has no
 * Android dependency.
 *
 * Memory use is O(1) per active session: no raw event list, interval list, or
 * typed-text buffer is retained. Inter-edit interval statistics are maintained
 * with Welford's online algorithm.
 */
class TypingSessionTracker {

    private class ActiveSession(val startTimeMs: Long) {
        var lastQualifyingEditTimeMs: Long = startTimeMs
        var charCount: Int = 0
        var qualifyingEditCount: Int = 0
        var deletionEditCount: Int = 0
        var microPauseCount: Int = 0

        // Welford's online algorithm running state for inter-edit interval variance.
        var intervalCount: Long = 0
        var intervalMean: Double = 0.0
        var intervalM2: Double = 0.0

        fun recordInterval(intervalMs: Long) {
            intervalCount++
            val delta = intervalMs - intervalMean
            intervalMean += delta / intervalCount
            val delta2 = intervalMs - intervalMean
            intervalM2 += delta * delta2
        }

        val intervalStdDevMs: Double
            get() = if (intervalCount < 2) 0.0 else sqrt(intervalM2 / (intervalCount - 1))
    }

    private var active: ActiveSession? = null

    /**
     * Processes one edit event: [charactersAdded]/[charactersRemoved] are the
     * event-derived counts for that single edit, [timestampMs] its caller-supplied
     * elapsed-realtime timestamp.
     *
     * Returns the completed result for the previous session if this event's gap
     * since the last qualifying edit reached [TypingSessionTunables.INACTIVITY_TIMEOUT_MS]
     * (and that session met [TypingSessionTunables.MIN_EDITS_PER_SESSION]); otherwise null.
     * This event itself always starts/continues the new-or-current session.
     */
    fun onEdit(timestampMs: Long, charactersAdded: Int, charactersRemoved: Int): TypingSessionResult? {
        // Paste/autofill events never start a session and are excluded from every
        // qualifying-edit statistic; they are otherwise invisible to the tracker
        // (they do not reset the inactivity clock or break an interval gap).
        if (charactersAdded >= TypingSessionTunables.PASTE_CHAR_THRESHOLD) {
            return null
        }

        val current = active
        if (current == null) {
            active = ActiveSession(timestampMs)
            applyQualifyingEdit(active!!, timestampMs, charactersAdded, charactersRemoved)
            return null
        }

        val gapMs = timestampMs - current.lastQualifyingEditTimeMs
        if (gapMs >= TypingSessionTunables.INACTIVITY_TIMEOUT_MS) {
            val completed = finishSession(current, current.lastQualifyingEditTimeMs)
            active = ActiveSession(timestampMs)
            applyQualifyingEdit(active!!, timestampMs, charactersAdded, charactersRemoved)
            return completed
        }

        if (gapMs >= TypingSessionTunables.MICRO_PAUSE_MS) {
            current.microPauseCount++
        }
        current.recordInterval(gapMs)
        applyQualifyingEdit(current, timestampMs, charactersAdded, charactersRemoved)
        return null
    }

    /**
     * Ends the active session, if any, using [timestampMs] as its end time. Used
     * for both the app-switch boundary and service-teardown flush — both are
     * externally observed "this happened now" signals. Safe no-op if there is no
     * active session.
     */
    fun endSession(timestampMs: Long): TypingSessionResult? {
        val current = active ?: return null
        active = null
        return finishSession(current, timestampMs)
    }

    private fun applyQualifyingEdit(
        session: ActiveSession,
        timestampMs: Long,
        charactersAdded: Int,
        charactersRemoved: Int
    ) {
        session.lastQualifyingEditTimeMs = timestampMs
        session.charCount += charactersAdded
        session.qualifyingEditCount++
        if (charactersRemoved > 0) {
            session.deletionEditCount++
        }
    }

    private fun finishSession(session: ActiveSession, endTimeMs: Long): TypingSessionResult? {
        if (session.qualifyingEditCount < TypingSessionTunables.MIN_EDITS_PER_SESSION) {
            return null
        }

        val durationMinutes = (endTimeMs - session.startTimeMs) / 60000.0
        val typingSpeedCpm = if (durationMinutes > 0.0) session.charCount / durationMinutes else 0.0
        val backspaceRate = session.deletionEditCount.toDouble() / session.qualifyingEditCount

        return TypingSessionResult(
            startTimeMs = session.startTimeMs,
            endTimeMs = endTimeMs,
            typingSpeedCpm = typingSpeedCpm,
            backspaceRate = backspaceRate,
            intervalStdDevMs = session.intervalStdDevMs,
            microPauseCount = session.microPauseCount,
            charCount = session.charCount
        )
    }
}
