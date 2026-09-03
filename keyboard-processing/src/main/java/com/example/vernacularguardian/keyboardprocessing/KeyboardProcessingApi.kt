package com.example.vernacularguardian.keyboardprocessing

import kotlinx.coroutines.flow.Flow

/**
 * The single public integration surface for the `keyboard-processing`
 * module. Other modules (e.g. `:app`'s demo screen) must depend only on this
 * interface, [KeyboardProcessingModule], and [BehavioralDailySummary] — never
 * on any internal class (`OwnerSessionManager`, the aggregation
 * engine/scheduler/worker, Room entities/DAOs, or
 * `KeystrokeAccessibilityService`).
 */
interface KeyboardProcessingApi {

    /**
     * Schedules the daily aggregation job via the existing Sprint 4
     * scheduler. Idempotent: calling this any number of times (including
     * when the job is already scheduled) never creates a duplicate
     * WorkManager job and never changes the existing 24-hour/BatteryNotLow
     * configuration. Does not enable `AccessibilityService` — that remains a
     * user action performed through system Settings.
     */
    fun startPassiveCapture()

    /**
     * Cancels the scheduled daily aggregation job (the same unique
     * WorkManager work started by [startPassiveCapture]). Does not delete
     * any stored data, disable `AccessibilityService`, or revoke the
     * notification permission.
     */
    fun stopPassiveCapture()

    /**
     * Whether the OS-level Accessibility permission for this app's
     * `KeystrokeAccessibilityService` is currently granted, reflecting the
     * real Android accessibility-service-enabled state. Never enables the
     * service and never touches secure settings.
     */
    fun isPassiveCaptureAuthorized(): Boolean

    /**
     * A live [Flow] of today's [BehavioralDailySummary], updating as new
     * sessions are aggregated for the current device-local calendar day. No
     * raw session rows, Room entities, or DAOs are ever exposed.
     */
    fun observeDailyBehavioralSummary(): Flow<BehavioralDailySummary>

    /**
     * Runs the daily rollup and retention cleanup immediately, bypassing the
     * 24-hour schedule (for demos/manual testing). Delegates to the same
     * Sprint 4 aggregation engine the scheduled worker uses.
     */
    suspend fun forceAggregateNow()

    /** Whether the current typing session's owner-confirmation gate is unconfirmed. */
    fun needsOwnerConfirmationForCurrentSession(): Boolean

    /** Confirms the current session only ("Yes"). */
    fun confirmOwnerForCurrentSession()

    /** Reverts/keeps the current session unconfirmed ("Not me"). */
    fun revokeOwnerForCurrentSession()
}

/**
 * Public, immutable snapshot of one calendar day's aggregated typing
 * behavior. Contains only derived metrics — never raw text, package/app
 * names, owner/device identity, raw session rows, or timestamps lists.
 */
data class BehavioralDailySummary(
    val dateEpochDay: Long,
    val avgTypingSpeedCpm: Double,
    val avgBackspaceRate: Double,
    val intervalStdDevMs: Double,
    val pauseFrequencyPer100Chars: Double,
    val sessionCount: Int,
    val riskContributionScore: Double,
    val isReliable: Boolean
)