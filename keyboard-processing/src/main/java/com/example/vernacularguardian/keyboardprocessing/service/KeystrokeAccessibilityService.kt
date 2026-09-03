package com.example.vernacularguardian.keyboardprocessing.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticsRepository
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticsServiceState
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType
import com.example.vernacularguardian.keyboardprocessing.owner.OwnerConfirmationNotifier
import com.example.vernacularguardian.keyboardprocessing.owner.OwnerSessionManager
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingStorage
import com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionResult
import com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTracker
import com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTunables

/**
 * Adapts system accessibility events into calls on the pure-Kotlin
 * [TypingSessionTracker]. This class owns no session logic itself — it only
 * extracts event-derived counts/timestamps and the foreground-app-change
 * signal, and forwards them to the tracker, which remains the single source
 * of truth for session state.
 *
 * Every completed session is logged (Sprint 2) and persisted to Room
 * (Sprint 3); the tracker output itself is unchanged and unrecalculated.
 *
 * Sprint 5 inserts [OwnerSessionManager] as a gate strictly BEFORE the
 * tracker: a text-change event only reaches [tracker] when the current
 * session is confirmed. Unconfirmed events are dropped here and never cause
 * tracker computation or persistence.
 *
 * Sprint 9: the owner-confirmation gate's "session" is now the device-unlock
 * interval, detected via a runtime-registered [BroadcastReceiver] for
 * `ACTION_SCREEN_OFF` (device lock: ends the unlock session, see
 * [handleDeviceLocked]) and `ACTION_USER_PRESENT` (device unlock: begins a
 * new unconfirmed unlock session, see [handleDeviceUnlocked]) — the standard
 * Android signals for this, appropriate for this module's minSdk 24. Neither
 * action can be manifest-declared (both require a runtime-registered
 * receiver on every supported API level), so registration happens here, tied
 * to this service's own connect/destroy lifecycle, exactly like the rest of
 * this class's background-capture responsibilities (master prompt Section 5:
 * this mechanism must live in the AccessibilityService layer, not depend on
 * an Activity).
 */
class KeystrokeAccessibilityService : AccessibilityService() {

    private val tracker = TypingSessionTracker()
    private val storage by lazy { KeyboardProcessingStorage(applicationContext) }
    private val ownerConfirmationNotifier by lazy { OwnerConfirmationNotifier(applicationContext) }

    // Sprint 8: developer-diagnostics observation only (Section 12/41) - never
    // read by any tracker/owner-gate/persistence logic below.
    private val diagnostics by lazy { DeveloperDiagnosticsRepository.getInstance(applicationContext) }

    // Used only to detect that the foreground app actually changed; never
    // stored as tracker/session state and never logged.
    private var currentPackageName: CharSequence? = null

    // Sprint 9: registered in onServiceConnected(), unregistered in
    // onDestroy(). Null only outside that window.
    private var lockStateReceiver: BroadcastReceiver? = null

    // Sprint 10: tracking for unlock/usage session validation.
    private var currentUnlockStartTimeMs: Long? = null
    private var typingOccurredInCurrentUnlockSession: Boolean = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        // The declarative config in accessibility_service_config.xml has been verified
        // correct (source, packaged resource, and manifest meta-data all match), but
        // dumpsys accessibility reported the bound service's runtime eventTypes/feedbackType
        // as empty. Applying the same configuration explicitly via setServiceInfo at
        // connection time is the standard AccessibilityService mechanism for this and
        // does not change what the service is configured to do.
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
        }
        // PRD: OwnerSessionManager must be UNCONFIRMED whenever the service starts,
        // regardless of whatever state the process-wide singleton happened to be in
        // (e.g. the service being restarted without a full process restart). This
        // remains a fail-closed default even under Sprint 9 semantics: a service
        // (re)connect is itself a coverage gap of unknown length (the OS may have
        // killed and restarted the service after a lock/unlock this instance never
        // observed), so it is never safe to assume a prior confirmation still
        // applies.
        OwnerSessionManager.resetForNewSession()
        registerLockStateReceiver()

        // If the service connects while the device is already unlocked, initialize the current
        // unlock-session start timestamp so that a subsequent device-lock correctly records
        // the usage session duration.
        val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
        if (keyguardManager != null && !keyguardManager.isDeviceLocked) {
            if (currentUnlockStartTimeMs == null) {
                currentUnlockStartTimeMs = SystemClock.elapsedRealtime()
                typingOccurredInCurrentUnlockSession = false
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.DEVICE_UNLOCKED)
                diagnostics.record(DiagnosticEventType.DEVICE_UNLOCKED)
            }
        }

        // Sprint 8 observational hooks: this reset is unconditional on every
        // connect (including the very first one, not only a genuine
        // reconnect), so it is counted as such here rather than guessed at.
        // Sprint 9 fix: this diagnostics-only timestamp must be wall-clock
        // (System.currentTimeMillis()), not elapsedRealtime() - it is
        // rendered on-screen as a calendar date/time
        // (DeveloperDiagnosticsScreen.formatEpochMs), and elapsedRealtime()
        // (boot-relative) produced a bogus near-1970 date there. This is the
        // only use of this timestamp in this method, so no tracker/session
        // logic is affected.
        val nowEpochMs = System.currentTimeMillis()
        DeveloperDiagnosticsServiceState.recordConnected(nowEpochMs)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_RESET_RECONNECT)
        diagnostics.record(DiagnosticEventType.SERVICE_CONNECTED)
    }

    /**
     * Registers the device lock/unlock receiver, defensively unregistering
     * any prior instance first (onServiceConnected can fire again on a
     * genuine reconnect without an intervening onDestroy on some OEMs/OS
     * versions). `RECEIVER_NOT_EXPORTED` is required on API 33+ and safe on
     * every supported API level via [ContextCompat].
     */
    private fun registerLockStateReceiver() {
        lockStateReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Not currently registered; nothing to clean up.
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> handleDeviceLocked()
                    Intent.ACTION_USER_PRESENT -> handleDeviceUnlocked()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        // System broadcasts (ACTION_SCREEN_OFF, ACTION_USER_PRESENT) originate from the system OS ('android')
        // and require RECEIVER_EXPORTED when dynamically registered on API 33+/34+.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        lockStateReceiver = receiver
    }

    /**
     * Device lock (`ACTION_SCREEN_OFF`) ends the current owner-unlock
     * session (master prompt Section 4). This is the ONLY app-switch/
     * inactivity-independent boundary that resets [OwnerSessionManager] for
     * a fresh confirmation requirement — see the class doc on
     * [OwnerSessionManager].
     */
    private fun handleDeviceLocked() {
        val elapsedMs = SystemClock.elapsedRealtime()
        Log.d(TAG, "device locked: ending current unlock-session")
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.DEVICE_LOCKED)
        diagnostics.record(DiagnosticEventType.DEVICE_LOCKED)

        val unlockStartMs = currentUnlockStartTimeMs
        if (unlockStartMs != null) {
            val durationMs = elapsedMs - unlockStartMs
            if (durationMs >= USAGE_SESSION_MIN_DURATION_MS && typingOccurredInCurrentUnlockSession) {
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.VALID_USAGE_SESSION)
                diagnostics.record(DiagnosticEventType.VALID_USAGE_SESSION, valueLong = durationMs)
            } else {
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.INVALID_USAGE_SESSION)
                diagnostics.record(DiagnosticEventType.INVALID_USAGE_SESSION, valueLong = durationMs)
            }
        }
        currentUnlockStartTimeMs = null
        typingOccurredInCurrentUnlockSession = false

        // Same wasConfirmed-before-reset persistence gating as the app-switch/
        // teardown boundaries (Sprint 7 hardening correction): a session
        // already revoked ("Not me") before lock must never be persisted just
        // because lock happens to flush it out of the tracker.
        val wasConfirmed = OwnerSessionManager.isConfirmed()
        val completedSession = tracker.endSession(elapsedMs)
        logCompletedSession(boundary = "device lock", result = completedSession)
        if (wasConfirmed) {
            completedSession?.let { storage.persistCompletedSession(it) }
        }

        OwnerSessionManager.resetForNewSession()
        // A prompt still showing when the phone locks must not linger into
        // the next unlock session.
        ownerConfirmationNotifier.cancelPrompt()
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_RESET_UNLOCK_SESSION)
    }

    /**
     * Device unlock (`ACTION_USER_PRESENT`) begins a new unconfirmed unlock
     * session. No state change is needed here: [handleDeviceLocked] (or
     * [onServiceConnected], for the very first unlock this process instance
     * observes) already left the gate UNCONFIRMED with the prompt-shown flag
     * cleared - exactly "wait until qualifying typing before showing a
     * prompt" (master prompt Section 4). This call exists so the unlock
     * signal itself is independently observable in diagnostics, e.g. to
     * verify on a real device that this receiver is actually firing.
     */
    private fun handleDeviceUnlocked() {
        Log.d(TAG, "device unlocked: new unlock-session begins unconfirmed")
        currentUnlockStartTimeMs = SystemClock.elapsedRealtime()
        typingOccurredInCurrentUnlockSession = false
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.DEVICE_UNLOCKED)
        diagnostics.record(DiagnosticEventType.DEVICE_UNLOCKED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTextChanged(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowStateChanged(event)
        }
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        // Sprint 8 observational hook: counts every TYPE_VIEW_TEXT_CHANGED
        // event this service receives, including the password-skipped ones
        // below - it does not change which branch runs.
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEXT_CHANGED_TOTAL)

        // PRD: fields flagged isPassword are explicitly skipped and never processed.
        if (event.isPassword) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.PASSWORD_SKIPPED)
            diagnostics.record(DiagnosticEventType.PASSWORD_SKIPPED)
            Log.d(TAG, "text-changed event skipped: password field")
            return
        }

        typingOccurredInCurrentUnlockSession = true
        val timestampMs = SystemClock.elapsedRealtime()

        // Sprint 5 owner-confirmation gate: sits strictly before the tracker. While
        // unconfirmed, events are dropped here — no tracker call, no metrics, no
        // persistence — and at most one prompt is shown per unlock-session (Sprint 9).
        if (!OwnerSessionManager.isConfirmed()) {
            // Sprint 9: this gap is now purely observational (diagnostic
            // counter only) - it no longer resets the gate or the
            // prompt-shown flag. The "one prompt per unlock-session"
            // guarantee now comes entirely from OwnerSessionManager only
            // being reset by handleDeviceLocked()/onServiceConnected()/onDestroy().
            if (OwnerSessionManager.noteUnconfirmedActivity(timestampMs)) {
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.INACTIVITY_BOUNDARY_UNCONFIRMED)
            }
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.EVENT_DROPPED_UNCONFIRMED)
            if (OwnerSessionManager.shouldShowPromptOnce()) {
                ownerConfirmationNotifier.showConfirmationPrompt()
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_PROMPT_SHOWN)
                diagnostics.record(DiagnosticEventType.OWNER_PROMPT_SHOWN)
            }
            Log.d(TAG, "text-changed event dropped: owner session unconfirmed")
            return
        }

        // Sprint 9: a confirmed unlock-session's own inactivity is now
        // observational only (diagnostic counter) - it no longer revokes
        // confirmation (see OwnerSessionManager.noteConfirmedActivity's
        // updated doc). TypingSessionTracker's own, unrelated internal 30s
        // inactivity boundary still splits the metrics session exactly as
        // before (Sprint 1, unchanged) - it just no longer implies anything
        // about owner confirmation, so unlike Sprint 5-8 there is no second
        // "dropped, re-prompt" branch here.
        if (OwnerSessionManager.noteConfirmedActivity(timestampMs)) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.INACTIVITY_BOUNDARY_CONFIRMED)
        }

        val charactersAdded = event.addedCount.coerceAtLeast(0)
        val charactersRemoved = event.removedCount.coerceAtLeast(0)
        recordTextChangeShapeDiagnostics(charactersAdded, charactersRemoved)

        Log.d(
            TAG,
            "text-changed timestampMs=$timestampMs charactersAdded=$charactersAdded " +
                "charactersRemoved=$charactersRemoved"
        )

        DeveloperDiagnosticCounters.increment(DiagnosticEventType.EVENT_FORWARDED_TO_TRACKER)
        val completedSession = tracker.onEdit(timestampMs, charactersAdded, charactersRemoved)
        logCompletedSession(boundary = "inactivity timeout", result = completedSession)
        completedSession?.let { storage.persistCompletedSession(it) }
    }

    /**
     * Sprint 8 observational hook, called only for events that reach the
     * tracker (password-skipped and owner-gate-dropped events never get
     * here). Mirrors [com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionTracker.onEdit]'s
     * own paste-threshold constant read-only, purely to count it — it does
     * not change what the tracker itself does with this edit.
     */
    private fun recordTextChangeShapeDiagnostics(charactersAdded: Int, charactersRemoved: Int) {
        if (charactersAdded >= TypingSessionTunables.PASTE_CHAR_THRESHOLD) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.PASTE_DETECTED)
        }
        if (charactersAdded > 0) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEXT_CHANGED_ADDED_CHARS)
        }
        if (charactersRemoved > 0) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEXT_CHANGED_REMOVED_CHARS)
        }
        if (charactersAdded == 0 && charactersRemoved == 0) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEXT_CHANGED_ZERO_CHANGE)
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val newPackageName = event.packageName
        // Sprint 8 observational hooks: counts every TYPE_WINDOW_STATE_CHANGED
        // event and, separately, ones with a null package - never changes the
        // same-app early-return below.
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.WINDOW_STATE_CHANGED_TOTAL)
        if (newPackageName == null) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.NULL_PACKAGE_WINDOW_EVENT)
        }
        if (newPackageName == currentPackageName) {
            // Same foreground app re-firing a window-state event; not an app switch.
            return
        }
        val previousPackageName = currentPackageName
        currentPackageName = newPackageName
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.APP_SWITCH_BOUNDARY)

        val timestampMs = SystemClock.elapsedRealtime()
        Log.d(TAG, "window-state-changed: foreground app change detected, timestampMs=$timestampMs")

        // Sprint 7 hardening correction (unchanged by Sprint 9): capture the
        // owner-confirmation state BEFORE the tracker is flushed below, so a
        // session that was revoked ("Not me") earlier in this same
        // still-open unlock-session is never persisted just because a later
        // app-switch boundary happens to flush it out of the tracker.
        val wasConfirmed = OwnerSessionManager.isConfirmed()

        // Sprint 2's tracker app-switch/session-ending behavior is unconditional and
        // unchanged: every detected foreground-window change still ends/flushes an
        // in-flight session exactly as before.
        val completedSession = tracker.endSession(timestampMs)
        logCompletedSession(boundary = "app switch", result = completedSession)
        if (wasConfirmed) {
            completedSession?.let { storage.persistCompletedSession(it) }
        }

        // Sprint 9: app switch no longer resets the owner-confirmation gate -
        // confirmation now lasts the entire device-unlock session (see
        // handleDeviceLocked), independent of which app is in the
        // foreground. The systemui transition is still tracked below, purely
        // for observability (e.g. notification-shade open/close frequency);
        // it no longer needs to gate anything, since there is no owner-gate
        // reset here anymore, systemui or otherwise.
        val isSystemUiTransition = newPackageName == SYSTEM_UI_PACKAGE || previousPackageName == SYSTEM_UI_PACKAGE
        if (isSystemUiTransition) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.SYSTEMUI_TRANSITION)
        }
        diagnostics.record(DiagnosticEventType.APP_SWITCH_BOUNDARY)
    }

    override fun onDestroy() {
        // PRD: an in-flight session must be flushed when the service is torn down by the OS.
        val elapsedMs = SystemClock.elapsedRealtime()
        // Sprint 7 hardening correction: same reasoning as handleWindowStateChanged
        // - capture confirmation state before resetForNewSession() below so a
        // revoked ("Not me") in-flight session is never persisted on teardown.
        val wasConfirmed = OwnerSessionManager.isConfirmed()
        val completedSession = tracker.endSession(elapsedMs)
        logCompletedSession(boundary = "service teardown", result = completedSession)
        if (wasConfirmed) {
            completedSession?.let { storage.persistCompletedSession(it) }
        }
        OwnerSessionManager.resetForNewSession()
        // Dismiss any pending prompt so a stale notification does not outlive the
        // service that would have processed a subsequent confirmed session.
        ownerConfirmationNotifier.cancelPrompt()

        lockStateReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered; nothing to clean up.
            }
        }
        lockStateReceiver = null

        // Sprint 8 observational hooks. Sprint 9 fix: this diagnostics-only
        // timestamp must be wall-clock, not elapsedRealtime() - see the
        // matching fix/comment in onServiceConnected().
        val nowEpochMs = System.currentTimeMillis()
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.TEARDOWN_BOUNDARY)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_RESET_TEARDOWN)
        DeveloperDiagnosticsServiceState.recordDestroyed(nowEpochMs)
        diagnostics.record(DiagnosticEventType.SERVICE_DESTROYED)
        diagnostics.record(DiagnosticEventType.TEARDOWN_BOUNDARY)

        super.onDestroy()
    }

    override fun onInterrupt() {
        // Required AccessibilityService override; no tracker state depends on it.
    }

    private fun logCompletedSession(boundary: String, result: TypingSessionResult?) {
        if (result == null) return
        Log.i(
            TAG,
            "session completed boundary=$boundary startTimeMs=${result.startTimeMs} " +
                "endTimeMs=${result.endTimeMs} charCount=${result.charCount} " +
                "typingSpeedCpm=${result.typingSpeedCpm} backspaceRate=${result.backspaceRate} " +
                "intervalStdDevMs=${result.intervalStdDevMs} microPauseCount=${result.microPauseCount}"
        )
    }

    private companion object {
        const val TAG = "KeystrokeAccessibility"
        const val USAGE_SESSION_MIN_DURATION_MS = 30_000L // 30 seconds required for a valid usage session

        // The Android System UI package hosts the notification shade/heads-up surface.
        // Diagnostics-only since Sprint 9: this no longer gates any owner-confirmation
        // reset decision (app-switch never resets the gate at all now, systemui or
        // otherwise - see handleWindowStateChanged) - it only feeds the
        // SYSTEMUI_TRANSITION observability counter.
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
