package com.example.vernacularguardian.keyboardprocessing.owner

import com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTunables
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Deterministic tests for [OwnerSessionManager] (PRD master-prompt Section 26,
 * items 1-11). [OwnerSessionManager] is a process-wide singleton (it must be,
 * so the notification action [OwnerConfirmationActionReceiver] and the
 * accessibility service observe the same state) so each test resets it first
 * to avoid order-dependence between tests.
 *
 * Item 12 ("notification permission unavailable -> remains unconfirmed") is
 * inherently an Android-permission concern, not a pure state-machine concern:
 * [OwnerSessionManager] itself has no notion of notification permission at
 * all — it simply never receives a confirmCurrentSession() call when the
 * prompt can't be shown, so it stays UNCONFIRMED by construction. That is
 * validated end-to-end against the real permission API in the instrumented
 * OwnerConfirmationNotifierTest.
 */
class OwnerSessionManagerTest {

    @Before
    fun resetSingletonState() {
        OwnerSessionManager.resetForNewSession()
    }

    @Test
    fun `initial state is unconfirmed`() {
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `confirmCurrentSession changes state to confirmed`() {
        OwnerSessionManager.confirmCurrentSession()
        assertTrue(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `revokeCurrentSession changes state to unconfirmed`() {
        OwnerSessionManager.confirmCurrentSession()
        OwnerSessionManager.revokeCurrentSession()
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `resetForNewSession changes confirmed to unconfirmed`() {
        OwnerSessionManager.confirmCurrentSession()
        assertTrue(OwnerSessionManager.isConfirmed())
        OwnerSessionManager.resetForNewSession()
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `confirmation does not persist across reset into the next session`() {
        OwnerSessionManager.confirmCurrentSession()
        OwnerSessionManager.resetForNewSession()
        // Session 2 begins fully unconfirmed - no residual confirmation from session 1.
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `unconfirmed session grants no processing permission`() {
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `confirmed session grants processing permission`() {
        OwnerSessionManager.confirmCurrentSession()
        assertTrue(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `not me leaves the current session unconfirmed`() {
        // Simulates "Not me" tapped before any confirmation - session simply stays unconfirmed.
        OwnerSessionManager.revokeCurrentSession()
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `not me revokes a previously confirmed session immediately`() {
        OwnerSessionManager.confirmCurrentSession()
        assertTrue(OwnerSessionManager.isConfirmed())
        // Simulates "Not me" tapped after an earlier "Yes" for the same still-open session.
        OwnerSessionManager.revokeCurrentSession()
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `no response leaves the session unconfirmed indefinitely`() {
        OwnerSessionManager.shouldShowPromptOnce()
        // No confirmCurrentSession() call ever made - state must remain unconfirmed
        // no matter how many further qualifying events arrive.
        assertFalse(OwnerSessionManager.isConfirmed())
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `prompt is shown at most once per session`() {
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        // Further qualifying events in the same session must not re-trigger a prompt,
        // regardless of confirm/revoke calls in between.
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
        OwnerSessionManager.confirmCurrentSession()
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
        OwnerSessionManager.revokeCurrentSession()
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
    }

    @Test
    fun `prompt-shown state resets at the next session boundary`() {
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())

        OwnerSessionManager.resetForNewSession()

        // A fresh session must be able to show its own prompt again.
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
    }

    // Sprint 9: a "session" is now the device-unlock interval (see
    // OwnerSessionManager's class doc), not a typing-activity window, so
    // noteUnconfirmedActivity() no longer resets anything on a >=30s gap -
    // it is diagnostic-only. These tests replace the Sprint 5-8 versions that
    // asserted the opposite (inactivity used to begin a new unconfirmed
    // session and re-allow a prompt); only OwnerSessionManager.resetForNewSession()
    // (called by KeystrokeAccessibilityService on device lock/connect/teardown)
    // does that now.

    @Test
    fun `unconfirmed first activity allows exactly one prompt`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)

        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
    }

    @Test
    fun `repeated unconfirmed activity within thirty seconds does not allow a second prompt`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())

        // Gap of just under the inactivity threshold since the last unconfirmed activity.
        OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS - 1
        )

        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
    }

    @Test
    fun `unconfirmed inactivity of at least thirty seconds does not change state`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())

        // Gap of exactly the inactivity threshold since the last unconfirmed activity.
        OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS
        )

        // Still unconfirmed (never fabricates a confirmation) - unchanged from
        // Sprint 5-8, only the reason it stays unconfirmed has changed.
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `repeated long unconfirmed inactivity gaps never re-allow a prompt until an explicit session reset`() {
        // Redundant with the single-gap test below, matching the two independent
        // confirmed-side tests for the same guarantee: proves the "no reset on
        // inactivity" contract holds across MULTIPLE consecutive long gaps, not
        // just one, and that only an explicit resetForNewSession() (device lock)
        // - never another activity call, however many - re-allows a prompt.
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())

        var timestampMs = 0L
        repeat(5) {
            timestampMs += TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 3
            OwnerSessionManager.noteUnconfirmedActivity(timestampMs)
            assertFalse(OwnerSessionManager.shouldShowPromptOnce())
        }

        OwnerSessionManager.resetForNewSession()

        // Only the explicit reset - not any number of prior activity calls -
        // re-allows a fresh prompt.
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
    }

    @Test
    fun `no new prompt is allowed after a long unconfirmed inactivity gap within the same unlock-session`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())

        // A gap well beyond the inactivity threshold - e.g. the user paused,
        // switched apps, and came back much later, all within the same
        // still-unlocked device session.
        OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 10
        )

        // Sprint 9: the prompt-shown flag is NOT cleared by inactivity - only
        // resetForNewSession() (device lock) clears it. No notification spam
        // from repeated typing after an unanswered/ignored prompt.
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
    }

    @Test
    fun `isConfirmed stays false throughout repeated unconfirmed activity and across an inactivity boundary`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)
        assertFalse(OwnerSessionManager.isConfirmed())
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 5_000L)
        assertFalse(OwnerSessionManager.isConfirmed())
        OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = 5_000L + TypingSessionTunables.INACTIVITY_TIMEOUT_MS
        )
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `noteUnconfirmedActivity is a no-op while confirmed`() {
        assertTrue(OwnerSessionManager.shouldShowPromptOnce())
        OwnerSessionManager.confirmCurrentSession()

        // Even a gap far beyond the inactivity threshold must not touch a
        // confirmed session's state - TypingSessionTracker remains the sole
        // source of truth for a confirmed session's own inactivity boundary.
        OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 100
        )

        assertTrue(OwnerSessionManager.isConfirmed())
        assertFalse(OwnerSessionManager.shouldShowPromptOnce())
    }

    // Sprint 7 hardening correction: a confirmed session's own inactivity
    // boundary must be detected independently of TypingSessionTracker's return
    // value (see KeystrokeAccessibilityService.handleTextChanged). Mirrors the
    // noteUnconfirmedActivity test cases above for the opposite state.

    @Test
    fun `noteConfirmedActivity is a no-op while unconfirmed`() {
        OwnerSessionManager.noteConfirmedActivity(timestampMs = 0L)
        assertFalse(OwnerSessionManager.isConfirmed())

        // A gap far beyond the inactivity threshold must not confirm an
        // unconfirmed session - noteUnconfirmedActivity remains the sole source
        // of truth for that state's own bookkeeping.
        OwnerSessionManager.noteConfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 100
        )
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `repeated confirmed activity within thirty seconds keeps the session confirmed`() {
        OwnerSessionManager.confirmCurrentSession()

        OwnerSessionManager.noteConfirmedActivity(timestampMs = 0L)
        assertTrue(OwnerSessionManager.isConfirmed())

        OwnerSessionManager.noteConfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS - 1
        )
        assertTrue(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `confirmed inactivity of at least thirty seconds no longer revokes confirmation`() {
        // Sprint 9 semantic change (was "...resets the gate to unconfirmed" in
        // Sprint 7-8): confirmation now lasts the entire device-unlock
        // session regardless of typing pauses - e.g. the user confirms, reads
        // a long message before replying, or switches apps and comes back.
        // Only device lock (OwnerSessionManager.resetForNewSession(), called
        // from KeystrokeAccessibilityService on ACTION_SCREEN_OFF) ends
        // confirmation now.
        OwnerSessionManager.confirmCurrentSession()
        OwnerSessionManager.noteConfirmedActivity(timestampMs = 0L)
        assertTrue(OwnerSessionManager.isConfirmed())

        OwnerSessionManager.noteConfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 10
        )

        assertTrue(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `confirmed inactivity gap is reported for diagnostics without changing state`() {
        OwnerSessionManager.confirmCurrentSession()
        OwnerSessionManager.noteConfirmedActivity(timestampMs = 0L)

        val gapObserved = OwnerSessionManager.noteConfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS
        )

        assertTrue(gapObserved)
        assertTrue(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `confirmCurrentSession clears a stale confirmed-activity timestamp so the diagnostic gap reading is not spurious`() {
        // Confirm, generate activity, then revoke without a device-lock
        // boundary (e.g. "Not me") - lastConfirmedActivityTimestampMs is
        // intentionally left stale by revokeCurrentSession().
        OwnerSessionManager.confirmCurrentSession()
        OwnerSessionManager.noteConfirmedActivity(timestampMs = 0L)
        OwnerSessionManager.revokeCurrentSession()

        // Re-confirm much later (in real elapsed-realtime terms) than the stale
        // timestamp above - confirmCurrentSession() must clear it so this fresh
        // confirmation's own first activity does not spuriously report a
        // >=30s gap (diagnostic-only signal) against the earlier, superseded
        // confirmed period. State itself would stay CONFIRMED either way
        // under Sprint 9 semantics; this test is about the diagnostic gap
        // reading being accurate, not a state change.
        OwnerSessionManager.confirmCurrentSession()
        val gapObserved = OwnerSessionManager.noteConfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 10
        )

        assertFalse(gapObserved)
        assertTrue(OwnerSessionManager.isConfirmed())
    }

    // Sprint 9: noteUnconfirmedActivity()/noteConfirmedActivity() return
    // whether a >=30s gap was observed, purely as a diagnostic
    // observational hook (see KeystrokeAccessibilityService) - it no longer
    // implies any state-machine transition (see the tests above).

    @Test
    fun `noteUnconfirmedActivity returns false for the very first activity`() {
        assertFalse(OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L))
    }

    @Test
    fun `noteUnconfirmedActivity returns false when the gap is under the inactivity threshold`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)

        val gapObserved = OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS - 1
        )

        assertFalse(gapObserved)
    }

    @Test
    fun `noteUnconfirmedActivity returns true when a thirty-second-plus gap is observed, without changing state`() {
        OwnerSessionManager.noteUnconfirmedActivity(timestampMs = 0L)

        val gapObserved = OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS
        )

        assertTrue(gapObserved)
        assertFalse(OwnerSessionManager.isConfirmed())
    }

    @Test
    fun `noteUnconfirmedActivity returns false while confirmed`() {
        OwnerSessionManager.confirmCurrentSession()

        val boundaryFired = OwnerSessionManager.noteUnconfirmedActivity(
            timestampMs = TypingSessionTunables.INACTIVITY_TIMEOUT_MS * 100
        )

        assertFalse(boundaryFired)
    }
}
