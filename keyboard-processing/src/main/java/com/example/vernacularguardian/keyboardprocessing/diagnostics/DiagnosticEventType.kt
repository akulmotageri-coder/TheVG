package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Fixed, closed set of developer-diagnostic event codes (Sprint 8). Every
 * counter increment and every persisted `diagnostic_events` row is tagged
 * with one of these — never a free-text description, never typed content,
 * never a package/app name. New instrumentation points must add a new case
 * here rather than passing an ad-hoc string.
 */
enum class DiagnosticEventType {
    // Accessibility / text-change events (high-frequency; in-memory counters only)
    TEXT_CHANGED_TOTAL,
    TEXT_CHANGED_ADDED_CHARS,
    TEXT_CHANGED_REMOVED_CHARS,
    TEXT_CHANGED_ZERO_CHANGE,
    PASSWORD_SKIPPED,
    PASTE_DETECTED,
    EVENT_DROPPED_UNCONFIRMED,
    EVENT_FORWARDED_TO_TRACKER,
    WINDOW_STATE_CHANGED_TOTAL,
    NULL_PACKAGE_WINDOW_EVENT,
    SYSTEMUI_TRANSITION,

    // Session boundaries
    APP_SWITCH_BOUNDARY,
    INACTIVITY_BOUNDARY_CONFIRMED,
    INACTIVITY_BOUNDARY_UNCONFIRMED,
    TEARDOWN_BOUNDARY,

    // Device lock/unlock (Sprint 9/10): the owner-confirmation gate's session
    // boundary is now the device-unlock interval, not app-switch/inactivity.
    DEVICE_LOCKED,
    DEVICE_UNLOCKED,
    VALID_USAGE_SESSION,
    INVALID_USAGE_SESSION,

    // Owner gate
    OWNER_PROMPT_SHOWN,
    OWNER_CONFIRMED,
    OWNER_REVOKED,
    OWNER_RESET_UNLOCK_SESSION,
    OWNER_RESET_TEARDOWN,
    OWNER_RESET_RECONNECT,
    OWNER_PERMISSION_DENIED_FAIL_CLOSED,

    // Service lifecycle
    SERVICE_CONNECTED,
    SERVICE_DESTROYED,

    // Storage
    SESSION_PERSISTED,
    SESSION_PERSIST_FAILED,

    // Aggregation
    AGGREGATION_STARTED,
    AGGREGATION_SUCCESS,
    AGGREGATION_FAILURE,

    // WorkManager
    WORKER_EXECUTION,
    WORKER_SUCCESS,
    WORKER_FAILURE,
    SCHEDULE_CALLED,
    WORK_CANCELLED,

    // Errors (dashboard groups everything with this prefix as "Errors")
    ERROR_DATABASE,
    ERROR_WORKMANAGER,
    ERROR_NOTIFICATION,
    ERROR_DIAGNOSTICS_STORAGE,
    ERROR_INITIALIZATION;

    val isError: Boolean get() = name.startsWith("ERROR_")
}
