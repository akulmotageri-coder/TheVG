package com.example.vernacularguardian.keyboardprocessing.tracker

/**
 * Tunable parameters for [TypingSessionTracker], sourced from the Keyboard
 * Processing PRD, Appendix A.
 */
object TypingSessionTunables {

    /** Characters added in a single edit at/above this count is treated as paste/autofill. */
    const val PASTE_CHAR_THRESHOLD: Int = 15

    /** Gap between consecutive qualifying edits at/above this duration counts as a micro-pause. */
    const val MICRO_PAUSE_MS: Long = 2500L

    /** Minimum number of qualifying edits a session must contain to produce a completed result. */
    const val MIN_EDITS_PER_SESSION: Int = 3

    /** Gap since the last qualifying edit at/above this duration ends the active session. */
    const val INACTIVITY_TIMEOUT_MS: Long = 30000L
}
