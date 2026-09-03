package com.example.vernacularguardian.keyboardprocessing.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingSessionTrackerTest {

    private val doubleTolerance = 1e-9

    @Test
    fun `simple session has correct char count, backspace rate and positive speed`() {
        val tracker = TypingSessionTracker()

        assertNull(tracker.onEdit(timestampMs = 0, charactersAdded = 10, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 1000, charactersAdded = 10, charactersRemoved = 2))
        assertNull(tracker.onEdit(timestampMs = 2000, charactersAdded = 10, charactersRemoved = 0))

        val result = tracker.endSession(timestampMs = 3000)

        assertNotNull(result)
        result!!
        assertEquals(0L, result.startTimeMs)
        assertEquals(3000L, result.endTimeMs)
        assertEquals(30, result.charCount)
        // 30 chars over 3000ms (0.05 min) = 600 cpm
        assertEquals(600.0, result.typingSpeedCpm, doubleTolerance)
        assertTrue(result.typingSpeedCpm > 0.0)
        // 1 of 3 qualifying edits included a deletion
        assertEquals(1.0 / 3.0, result.backspaceRate, doubleTolerance)
        // intervals [1000, 1000] -> sample stddev 0
        assertEquals(0.0, result.intervalStdDevMs, doubleTolerance)
        assertEquals(0, result.microPauseCount)
    }

    @Test
    fun `paste autofill events are excluded from character count`() {
        val tracker = TypingSessionTracker()

        assertNull(tracker.onEdit(timestampMs = 0, charactersAdded = 5, charactersRemoved = 0))
        // Paste/autofill: charactersAdded >= 15 (pasteCharThreshold)
        assertNull(tracker.onEdit(timestampMs = 1000, charactersAdded = 20, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 2000, charactersAdded = 5, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 3000, charactersAdded = 5, charactersRemoved = 0))

        val result = tracker.endSession(timestampMs = 4000)

        assertNotNull(result)
        result!!
        // Only the 3 qualifying edits' characters (5 + 5 + 5) count; the pasted 20 is excluded.
        assertEquals(15, result.charCount)
    }

    @Test
    fun `app switch ends the session even mid-typing`() {
        val tracker = TypingSessionTracker()

        assertNull(tracker.onEdit(timestampMs = 0, charactersAdded = 5, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 500, charactersAdded = 5, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 1000, charactersAdded = 5, charactersRemoved = 0))

        // Caller signals a foreground-app change mid-typing (Android-independent boundary signal).
        val result = tracker.endSession(timestampMs = 1200)

        assertNotNull(result)
        result!!
        assertEquals(0L, result.startTimeMs)
        assertEquals(1200L, result.endTimeMs)
        assertEquals(15, result.charCount)

        // The session was cleared; a further boundary call is a safe no-op.
        assertNull(tracker.endSession(timestampMs = 1300))
    }

    @Test
    fun `inactivity beyond timeout starts a new session automatically`() {
        val tracker = TypingSessionTracker()

        // Session 1: 3 qualifying edits.
        assertNull(tracker.onEdit(timestampMs = 0, charactersAdded = 10, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 1000, charactersAdded = 10, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 2000, charactersAdded = 10, charactersRemoved = 0))

        // Gap of exactly inactivityTimeoutMs (30000ms) since the last qualifying edit (2000)
        // ends session 1 automatically and starts session 2 with this same event -
        // no explicit endSession() call is made.
        val session1Result = tracker.onEdit(timestampMs = 32000, charactersAdded = 8, charactersRemoved = 0)

        assertNotNull(session1Result)
        session1Result!!
        assertEquals(0L, session1Result.startTimeMs)
        assertEquals(2000L, session1Result.endTimeMs)
        assertEquals(30, session1Result.charCount)

        // Session 2 continues from the boundary-triggering event.
        assertNull(tracker.onEdit(timestampMs = 33000, charactersAdded = 8, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 34000, charactersAdded = 8, charactersRemoved = 0))

        val session2Result = tracker.endSession(timestampMs = 35000)

        assertNotNull(session2Result)
        session2Result!!
        assertEquals(32000L, session2Result.startTimeMs)
        assertEquals(35000L, session2Result.endTimeMs)
        assertEquals(24, session2Result.charCount)
    }

    @Test
    fun `gap within session is counted as a micro-pause`() {
        val tracker = TypingSessionTracker()

        assertNull(tracker.onEdit(timestampMs = 0, charactersAdded = 5, charactersRemoved = 0))
        // Gap of 3000ms: >= microPauseMs (2500ms) but < inactivityTimeoutMs (30000ms).
        assertNull(tracker.onEdit(timestampMs = 3000, charactersAdded = 5, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 4000, charactersAdded = 5, charactersRemoved = 0))

        val result = tracker.endSession(timestampMs = 5000)

        assertNotNull(result)
        result!!
        assertEquals(1, result.microPauseCount)
    }

    @Test
    fun `session below minimum edit count is dropped`() {
        val tracker = TypingSessionTracker()

        assertNull(tracker.onEdit(timestampMs = 0, charactersAdded = 5, charactersRemoved = 0))
        assertNull(tracker.onEdit(timestampMs = 1000, charactersAdded = 5, charactersRemoved = 0))

        // Only 2 qualifying edits, below minEditsPerSession (3): dropped, not persisted.
        val result = tracker.endSession(timestampMs = 2000)

        assertNull(result)
    }

    @Test
    fun `calling endSession with no active session is a safe no-op`() {
        val tracker = TypingSessionTracker()

        assertNull(tracker.endSession(timestampMs = 1000))
    }
}
