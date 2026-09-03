package com.example.vernacularguardian.keyboardprocessing.owner

import com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTunables

/**
 * Per-session owner-confirmation gate. A process-wide singleton (not tied to
 * any one [android.accessibilityservice.AccessibilityService] instance)
 * because the notification "Yes"/"Not me" actions are handled by a
 * short-lived [android.content.BroadcastReceiver] that must observe/mutate
 * the same state the accessibility service reads.
 *
 * No Android dependency and no persistence: state lives only in memory for
 * the current process, and is reset at every session boundary — exactly the
 * PRD's "confirmation does not carry into the next session" and "does not
 * persist across process restarts" requirements.
 *
 * Sprint 9: "session" here means the current device *unlock session* — the
 * interval from one device unlock to the next device lock — not a
 * foreground-app window and not a typing-activity gap. [resetForNewSession]
 * is therefore called only by [com.example.vernacularguardian.keyboardprocessing.service.KeystrokeAccessibilityService]
 * on an observed device-lock signal (`ACTION_SCREEN_OFF`), on service
 * connect (fail-closed default, in case an unknown gap in service coverage
 * occurred), and on service teardown — never on an app switch and never on a
 * typing-activity gap. See [noteUnconfirmedActivity]/[noteConfirmedActivity]
 * for what those inactivity signals mean now (observational only).
 */
object OwnerSessionManager {

    enum class State { UNCONFIRMED, CONFIRMED }

    @Volatile
    private var state: State = State.UNCONFIRMED

    @Volatile
    private var promptShownForCurrentSession: Boolean = false

    // Gating state only for an UNCONFIRMED session: the caller-supplied
    // elapsed-realtime timestamp of the most recent unconfirmed text-change
    // activity. Never persisted, never logged as a behavioral metric, never
    // passed to TypingSessionTracker, never stored in typing_sessions.
    @Volatile
    private var lastUnconfirmedActivityTimestampMs: Long? = null

    // Sprint 7 hardening: the CONFIRMED-side mirror of
    // [lastUnconfirmedActivityTimestampMs], used by [noteConfirmedActivity] to
    // detect a confirmed session's own inactivity boundary independently of
    // TypingSessionTracker's return value. Same privacy properties: never
    // persisted, never logged, never passed to the tracker.
    @Volatile
    private var lastConfirmedActivityTimestampMs: Long? = null

    fun isConfirmed(): Boolean = state == State.CONFIRMED

    fun confirmCurrentSession() {
        state = State.CONFIRMED
        // Clears any stale timestamp from a prior confirmed period in this same
        // still-open app session (e.g. an earlier confirm/"Not me"/re-confirm
        // cycle) so the next noteConfirmedActivity call is always measured from
        // this fresh confirmation, never from a stale, possibly-old timestamp.
        lastConfirmedActivityTimestampMs = null
    }

    fun revokeCurrentSession() {
        state = State.UNCONFIRMED
    }

    /**
     * Resets to UNCONFIRMED for a new unlock-session boundary (device lock,
     * service connect, or service teardown — see the class doc) and clears
     * the prompt-shown flag so the next unlock-session's first qualifying
     * event can surface a fresh prompt.
     */
    fun resetForNewSession() {
        state = State.UNCONFIRMED
        promptShownForCurrentSession = false
        lastUnconfirmedActivityTimestampMs = null
        lastConfirmedActivityTimestampMs = null
    }

    /**
     * Returns true exactly once per session (the first call after the most
     * recent [resetForNewSession]); every subsequent call returns false until
     * the next reset. Callers use this to show at most one confirmation
     * prompt per session regardless of how many events arrive while
     * unconfirmed — this is independent of confirm/revoke, so a "Not me" tap
     * does not cause a second prompt for the same session.
     */
    fun shouldShowPromptOnce(): Boolean {
        if (promptShownForCurrentSession) return false
        promptShownForCurrentSession = true
        return true
    }

    /**
     * Must be called by the caller for every qualifying text-change event
     * observed while UNCONFIRMED. Tracks a >= [TypingSessionTunables.INACTIVITY_TIMEOUT_MS]
     * gap since the last unconfirmed activity, purely for diagnostic
     * observability (e.g. "how often does the user pause mid-unconfirmed").
     *
     * Sprint 9 semantic change: this NO LONGER resets the gate or the
     * prompt-shown flag. Before Sprint 9, a "session" was bounded by
     * typing-activity gaps as well as app switches, so an inactivity gap
     * meant a new session — and therefore a new prompt. Now that a session is
     * the device-unlock interval (see the class doc), pausing between typing
     * bursts within the same unlock session (e.g. switching from WhatsApp to
     * Chrome and reading for a minute before typing again) must NOT force a
     * second confirmation prompt. Only [resetForNewSession] (called on device
     * lock/service connect/service teardown) starts a new session now.
     *
     * A no-op while CONFIRMED, same as before.
     *
     * Returns whether a >= [TypingSessionTunables.INACTIVITY_TIMEOUT_MS] gap
     * was observed since the last unconfirmed activity — diagnostic-only; the
     * caller does not need to (and must not) treat `true` as a boundary.
     */
    fun noteUnconfirmedActivity(timestampMs: Long): Boolean {
        if (state == State.CONFIRMED) return false
        val lastActivity = lastUnconfirmedActivityTimestampMs
        val gapObserved = lastActivity != null &&
            timestampMs - lastActivity >= TypingSessionTunables.INACTIVITY_TIMEOUT_MS
        lastUnconfirmedActivityTimestampMs = timestampMs
        return gapObserved
    }

    /**
     * The CONFIRMED-side mirror of [noteUnconfirmedActivity], same Sprint 9
     * semantic change: a >= [TypingSessionTunables.INACTIVITY_TIMEOUT_MS] gap
     * between qualifying edits while CONFIRMED is tracked and reported for
     * diagnostics only and no longer revokes confirmation. Before Sprint 9
     * this was the Sprint 7 hardening correction that reset the gate on a
     * confirmed session's own inactivity boundary; that boundary no longer
     * exists as a gate-reset cause — confirmation now lasts the entire
     * unlock session regardless of typing pauses. `TypingSessionTracker`'s
     * own, unrelated internal inactivity boundary (which splits the metrics
     * session) is completely untouched by this and keeps working exactly as
     * before.
     *
     * A no-op while UNCONFIRMED, same as before.
     */
    fun noteConfirmedActivity(timestampMs: Long): Boolean {
        if (state == State.UNCONFIRMED) return false
        val lastActivity = lastConfirmedActivityTimestampMs
        val gapObserved = lastActivity != null &&
            timestampMs - lastActivity >= TypingSessionTunables.INACTIVITY_TIMEOUT_MS
        lastConfirmedActivityTimestampMs = timestampMs
        return gapObserved
    }
}
