package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Pure data model for everything [DeveloperDiagnosticsScreen] displays.
 * Every field is a numeric metric, boolean, fixed-code string, or a nested
 * DTO of the same kind — never raw typed text, a package/app name, or any
 * other sensitive content (Sprint 8 Section 32/44 privacy requirement).
 * Deliberately separate from [com.example.vernacularguardian.keyboardprocessing.BehavioralDailySummary]
 * and the rest of [com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingApi] —
 * this type is never exposed through that public product surface.
 */
data class DeveloperDiagnosticsSnapshot(
    val generatedAtEpochMs: Long,
    val overview: OverviewSnapshot,
    val sessionAnalytics: SessionAnalyticsSnapshot,
    val sessionDetail: List<SessionDetailRow>,
    val dailyAnalytics: DailyAnalyticsSnapshot,
    val ownerGate: OwnerGateSnapshot,
    val accessibilityEvents: AccessibilityEventSnapshot,
    val sessionBoundaries: SessionBoundarySnapshot,
    val workManager: WorkManagerSnapshot,
    val database: DatabaseSnapshot,
    val resource: ResourceSnapshot,
    val battery: BatterySnapshot,
    val thermal: ThermalSnapshot,
    val serviceLifecycle: ServiceLifecycleSnapshot,
    val errors: ErrorSnapshot,
    val history: DiagnosticHistorySnapshot
)

data class OverviewSnapshot(
    val diagnosticsEnabled: Boolean,
    val processUptimeMs: Long?,
    val serviceConnected: Boolean,
    // Hardening-pass fix: null (not a fabricated "not granted") if the check
    // itself throws, consistent with notificationPermissionGranted below -
    // previously this defaulted to false on failure, which looked exactly
    // like a real denied-permission reading.
    val accessibilityAuthorized: Boolean?,
    val notificationPermissionGranted: Boolean?,
    val ownerConfirmed: Boolean,
    val aggregationScheduled: Boolean,
    val lastAggregationEpochMs: Long?,
    val lastDiagnosticSampleEpochMs: Long?,
    val databaseSizeBytes: Long,
    val totalSessionCount: Int,
    val todaySessionCount: Int,
    val todaySummaryAvailable: Boolean,
    // Hardening-pass fix: false only if the underlying typing_sessions/
    // daily_summaries read itself failed (e.g. SQLiteException) - previously
    // that failure was not caught at all and would crash the whole app; now
    // it degrades to this flag plus empty lists, surfaced honestly here
    // rather than presented as "genuinely zero sessions."
    val productDataReadable: Boolean,
    // Sprint 9 (master prompt Section 6): real android.os.PowerManager
    // .isIgnoringBatteryOptimizations() read. null only if the PowerManager
    // service itself is unavailable - never a fabricated true/false. OEM
    // battery-management (esp. MIUI) killing this app while backgrounded is
    // a documented, real cause of capture stopping on some devices; this is
    // the evidence needed to confirm/deny that on an actual affected device.
    val batteryOptimizationIgnored: Boolean?
)

data class SessionAnalyticsSnapshot(
    val totalSessions: Int,
    val todaySessions: Int,
    val sessionsLast7d: Int,
    val avgCharCount: Double?,
    val avgTypingSpeedCpm: Double?,
    val avgBackspaceRate: Double?,
    val avgIntervalStdDevMs: Double?,
    val avgMicroPauseCount: Double?,
    val avgPauseFrequencyPer100Chars: Double?,
    val minCharCount: Int?,
    val maxCharCount: Int?,
    val minTypingSpeedCpm: Double?,
    val maxTypingSpeedCpm: Double?
)

data class SessionDetailRow(
    val id: Long,
    val dateEpochDay: Long?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val charCount: Int,
    val typingSpeedCpm: Double,
    val backspaceRate: Double,
    val intervalStdDevMs: Double,
    val microPauseCount: Int
)

data class DailySummaryRow(
    val dateEpochDay: Long,
    val avgTypingSpeedCpm: Double,
    val avgBackspaceRate: Double,
    val intervalStdDevMs: Double,
    val pauseFrequencyPer100Chars: Double,
    val sessionCount: Int,
    val riskContributionScore: Double,
    val isReliable: Boolean
)

data class DailyAnalyticsSnapshot(
    val today: DailySummaryRow?,
    val latest: DailySummaryRow?,
    val previousDays: List<DailySummaryRow>
)

data class OwnerGateSnapshot(
    val currentState: String,
    val promptsShown: Long,
    val yesConfirmations: Long,
    val notMeActions: Long,
    val blockedEventsUnconfirmed: Long,
    val confirmedEventsForwarded: Long,
    val unlockSessionsStarted: Long,
    val validUsageSessions: Long,
    val invalidUsageSessions: Long,
    val resetsUnlockSession: Long,
    val resetsTeardown: Long,
    val resetsReconnect: Long,
    val systemUiTransitionEvents: Long,
    val permissionDeniedFailClosed: Long
)

data class AccessibilityEventSnapshot(
    val textChangedTotal: Long,
    val windowStateChangedTotal: Long,
    val passwordFieldEventsSkipped: Long,
    val pasteSizedEvents: Long,
    val eventsDroppedByOwnerGate: Long,
    val eventsForwardedToTracker: Long,
    val appSwitchBoundaries: Long,
    val inactivityBoundaries: Long,
    val teardownBoundaries: Long
)

data class BoundaryTypeStat(
    val totalCount: Long,
    val lastOccurrenceEpochMs: Long?,
    // Hardening-pass fix: null (not a fabricated real zero) if the DB query
    // itself failed.
    val countLast24h: Long?
)

data class SessionBoundarySnapshot(
    val appSwitch: BoundaryTypeStat,
    val inactivity: BoundaryTypeStat,
    val teardown: BoundaryTypeStat,
    val serviceReconnect: BoundaryTypeStat
)

data class WorkManagerSnapshot(
    val uniqueWorkName: String,
    val available: Boolean,
    val periodicSchedulingEnabled: Boolean,
    // Sprint 9 fix: renamed from intervalMs/requiresBatteryNotLow. WorkInfo's
    // public API does not expose a work request's interval or constraints,
    // so these are static literals mirroring BehavioralAggregationScheduler's
    // own configuration, NOT a live WorkManager read - the previous field
    // names implied otherwise. If the scheduler's configuration ever changes,
    // these must be updated to match (see DeveloperDiagnosticsWorkManagerAdapter).
    val configuredIntervalMs: Long,
    val configuredRequiresBatteryNotLow: Boolean,
    val currentState: String?,
    val runAttemptCount: Int,
    val observedExecutions: Long,
    val observedFailures: Long,
    val scheduleCalledCount: Long,
    val cancelCalledCount: Long
)

data class DatabaseSnapshot(
    val databaseName: String,
    val databaseSizeBytes: Long,
    val walSizeBytes: Long?,
    val shmSizeBytes: Long?,
    val typingSessionRowCount: Int,
    val dailySummaryRowCount: Int,
    val oldestSessionEpochDay: Long?,
    val newestSessionEpochDay: Long?,
    val todaySessionCount: Int,
    val todaySummaryExists: Boolean,
    val nullSessionEpochDayRows: Int,
    val lastRetentionDeletedRows: Int?,
    val lastAggregationEpochMs: Long?,
    val lastAggregationSucceeded: Boolean?
)

data class ResourceSnapshot(
    val timestampEpochMs: Long?,
    val processCpuTimeMs: Long?,
    val cpuUtilizationPercent: Double?,
    val cpuMeasurementNote: String,
    val totalPssKb: Int?,
    val javaHeapKb: Int?,
    val nativeHeapKb: Int?,
    val privateDirtyKb: Int?,
    // Sprint 9 fix: null (never a fabricated 0/0.0) until at least one valid
    // PSS reading has actually been captured - see DeveloperDiagnosticsSampler.
    val peakTotalPssKb: Int?,
    val averageTotalPssKb: Double?,
    val processUptimeMs: Long?
)

data class BatterySnapshot(
    val batteryPercent: Int?,
    val isCharging: Boolean?,
    val batteryTemperatureCelsius: Double?,
    val label: String
)

data class ThermalSnapshot(
    val thermalStatus: String?,
    val available: Boolean,
    val note: String
)

data class ServiceLifecycleSnapshot(
    val currentState: String,
    val connectionCount: Long,
    val destroyCount: Long,
    val lastConnectedEpochMs: Long?,
    val lastDisconnectedEpochMs: Long?
)

data class ErrorCountRow(
    val errorType: String,
    val count: Long,
    val lastOccurrenceEpochMs: Long?
)

data class ErrorSnapshot(
    val recentErrors: List<ErrorCountRow>,
    // Hardening-pass fix: false if the error-breakdown query itself failed -
    // previously that failure rendered identically to "no errors recorded,"
    // i.e. the one query whose job is surfacing errors could silently hide
    // its own failure as a clean bill of health.
    val available: Boolean
)

data class DiagnosticEventRow(
    val timestampEpochMs: Long,
    val eventType: String,
    val valueLong: Long,
    val valueDouble: Double,
    val durationMs: Long
)

data class DiagnosticHistorySnapshot(
    val latestEvents: List<DiagnosticEventRow>,
    val latestSamples: List<ResourceSnapshot>,
    val retentionDays: Long,
    // Hardening-pass fix: false if either underlying history query failed -
    // previously that failure rendered identically to "no history yet."
    val available: Boolean
)
