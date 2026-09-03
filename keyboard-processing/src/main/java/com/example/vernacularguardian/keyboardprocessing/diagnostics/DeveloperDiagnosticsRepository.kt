package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.content.Context
import android.os.PowerManager
import com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingModule
import com.example.vernacularguardian.keyboardprocessing.owner.OwnerConfirmationNotifier
import com.example.vernacularguardian.keyboardprocessing.owner.OwnerSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central Sprint 8 developer-diagnostics orchestrator. The only class
 * [DeveloperDiagnosticsScreen] talks to.
 *
 * Reads product data only through [DeveloperDiagnosticsProductDataAdapter]
 * (existing DAOs) and [KeyboardProcessingModule]'s own already-public
 * methods, and only ever observes [OwnerSessionManager] — it never controls
 * owner-gate state, aggregation, capture, or the public
 * [com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingApi]
 * contract. All product-affecting dashboard controls (start/stop capture,
 * force-aggregate) go through that API directly from the screen, not
 * through this repository.
 *
 * Persists to a database entirely separate from
 * [com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase]
 * (Section 16/36): [clearHistory] can never delete a `typing_sessions` or
 * `daily_summaries` row.
 */
class DeveloperDiagnosticsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = DeveloperDiagnosticsDatabase.getInstance(appContext)
    private val dao = database.diagnosticsDao()
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val resourceMonitor = DeveloperDiagnosticsResourceMonitor(appContext)
    private val sampler = DeveloperDiagnosticsSampler(resourceMonitor) { sample -> persistSampleIfEnabled(sample) }
    private val productAdapter = DeveloperDiagnosticsProductDataAdapter(appContext)
    private val workManagerAdapter = DeveloperDiagnosticsWorkManagerAdapter(appContext)
    private val keyboardModule by lazy { KeyboardProcessingModule.getInstance(appContext) }

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** Live latest resource sample, for a screen that stays open to observe without waiting for a full snapshot rebuild. */
    val latestSample: StateFlow<DeveloperDiagnosticSampleEntity?> get() = sampler.latest

    init {
        if (_enabled.value) {
            sampler.start()
        }
        writerScope.launch { runRetention() }
    }

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
        if (value) sampler.start() else sampler.stop()
    }

    /**
     * Always-safe, unconditional in-memory counter increment. Use for
     * high-frequency events (every accessibility text-change) that must
     * never trigger a per-event database write regardless of the enabled
     * state.
     */
    fun count(type: DiagnosticEventType, amount: Long = 1L) {
        DeveloperDiagnosticCounters.increment(type, amount)
    }

    /**
     * Increments the in-memory counter and, only while diagnostics are
     * enabled, persists one compact durable event row. Reserved for the
     * sparser boundary/lifecycle/error/aggregation event types (Section 31)
     * — never called once per raw accessibility event.
     */
    fun record(type: DiagnosticEventType, valueLong: Long = 0L, valueDouble: Double = 0.0, durationMs: Long = 0L) {
        DeveloperDiagnosticCounters.increment(type)
        if (!_enabled.value) return
        writerScope.launch {
            try {
                dao.insertEvent(
                    DeveloperDiagnosticEventEntity(
                        timestampEpochMs = System.currentTimeMillis(),
                        eventType = type.name,
                        valueLong = valueLong,
                        valueDouble = valueDouble,
                        durationMs = durationMs
                    )
                )
            } catch (e: Exception) {
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.ERROR_DIAGNOSTICS_STORAGE)
            }
        }
    }

    suspend fun sampleNow(): DeveloperDiagnosticSampleEntity = sampler.sampleNow()

    /** Clears ONLY `developer_diagnostics.db` and in-memory diagnostic state; never touches product data. */
    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            try {
                dao.clearEvents()
                dao.clearSamples()
            } catch (e: Exception) {
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.ERROR_DIAGNOSTICS_STORAGE)
            }
        }
        DeveloperDiagnosticCounters.reset()
        DeveloperDiagnosticsAggregationState.reset()
        sampler.resetStats()
    }

    suspend fun buildSnapshot(): DeveloperDiagnosticsSnapshot = withContext(Dispatchers.IO) {
        runRetention()

        // Hardening-pass fix: these two reads were previously unguarded - a
        // genuine SQLiteException/IOException against the actively-written
        // product database (disk full, locked by a concurrent aggregation
        // write, mid-migration) would propagate out of this function, out of
        // the screen's LaunchedEffect/DisposableEffect coroutines (neither
        // catches), and crash the whole app, not just this dashboard.
        val sessionsResult = runCatching { productAdapter.allSessions() }
        val summariesResult = runCatching { productAdapter.allDailySummaries() }
        val sessions = sessionsResult.getOrDefault(emptyList())
        val summaries = summariesResult.getOrDefault(emptyList())
        val productDataReadable = sessionsResult.isSuccess && summariesResult.isSuccess
        val workManager = workManagerAdapter.snapshot()
        val currentSample = sampler.latest.value ?: resourceMonitor.captureSample()

        val counters = DeveloperDiagnosticCounters.snapshot()
        fun c(type: DiagnosticEventType) = counters[type] ?: 0L

        val nowEpochMs = System.currentTimeMillis()
        val since24h = nowEpochMs - ONE_DAY_MS

        suspend fun boundaryStat(type: DiagnosticEventType) = BoundaryTypeStat(
            totalCount = c(type),
            lastOccurrenceEpochMs = runCatching { dao.latestTimestampByType(type.name) }.getOrNull(),
            // Hardening-pass fix: null (not a fabricated real zero) on
            // failure - a query failure previously looked identical to
            // "confirmed zero events in the last 24h."
            countLast24h = runCatching { dao.countEventsByTypeSince(type.name, since24h) }.getOrNull()
        )

        val errorBreakdownResult = runCatching { dao.errorBreakdown() }
        val latestEventsResult = runCatching { dao.latestEvents(HISTORY_LIMIT) }
        val latestSamplesResult = runCatching { dao.latestSamples(HISTORY_LIMIT) }
        val errorBreakdown = errorBreakdownResult.getOrDefault(emptyList())
        val latestEvents = latestEventsResult.getOrDefault(emptyList())
        val latestSamples = latestSamplesResult.getOrDefault(emptyList())

        // Hardening-pass fix: null (not a fabricated "not granted") on
        // failure, consistent with notificationPermissionGranted below -
        // previously a check failure looked identical to a real denied
        // reading for exactly the permission a developer would be
        // diagnosing.
        val accessibilityAuthorized = runCatching { keyboardModule.isPassiveCaptureAuthorized() }.getOrNull()
        val ownerConfirmed = OwnerSessionManager.isConfirmed()
        val databaseSnapshot = productAdapter.databaseSnapshot(sessions, summaries)

        DeveloperDiagnosticsSnapshot(
            generatedAtEpochMs = nowEpochMs,
            overview = OverviewSnapshot(
                diagnosticsEnabled = _enabled.value,
                processUptimeMs = currentSample.processUptimeMs,
                serviceConnected = DeveloperDiagnosticsServiceState.state == DeveloperDiagnosticsServiceState.State.CONNECTED,
                accessibilityAuthorized = accessibilityAuthorized,
                notificationPermissionGranted = runCatching { OwnerConfirmationNotifier(appContext).canNotify() }.getOrNull(),
                ownerConfirmed = ownerConfirmed,
                aggregationScheduled = workManager.periodicSchedulingEnabled,
                lastAggregationEpochMs = DeveloperDiagnosticsAggregationState.lastSuccessEpochMs
                    ?: DeveloperDiagnosticsAggregationState.lastFailureEpochMs,
                lastDiagnosticSampleEpochMs = currentSample.timestampEpochMs,
                databaseSizeBytes = databaseSnapshot.databaseSizeBytes,
                totalSessionCount = sessions.size,
                todaySessionCount = databaseSnapshot.todaySessionCount,
                todaySummaryAvailable = databaseSnapshot.todaySummaryExists,
                batteryOptimizationIgnored = resourceMonitor.isIgnoringBatteryOptimizations(),
                productDataReadable = productDataReadable
            ),
            sessionAnalytics = productAdapter.sessionAnalytics(sessions),
            sessionDetail = productAdapter.sessionDetail(sessions),
            dailyAnalytics = productAdapter.dailyAnalytics(summaries),
            ownerGate = OwnerGateSnapshot(
                currentState = if (ownerConfirmed) "CONFIRMED" else "UNCONFIRMED",
                promptsShown = c(DiagnosticEventType.OWNER_PROMPT_SHOWN),
                yesConfirmations = c(DiagnosticEventType.OWNER_CONFIRMED),
                notMeActions = c(DiagnosticEventType.OWNER_REVOKED),
                blockedEventsUnconfirmed = c(DiagnosticEventType.EVENT_DROPPED_UNCONFIRMED),
                confirmedEventsForwarded = c(DiagnosticEventType.EVENT_FORWARDED_TO_TRACKER),
                unlockSessionsStarted = c(DiagnosticEventType.DEVICE_UNLOCKED),
                validUsageSessions = c(DiagnosticEventType.VALID_USAGE_SESSION),
                invalidUsageSessions = c(DiagnosticEventType.INVALID_USAGE_SESSION),
                resetsUnlockSession = c(DiagnosticEventType.OWNER_RESET_UNLOCK_SESSION),
                resetsTeardown = c(DiagnosticEventType.OWNER_RESET_TEARDOWN),
                resetsReconnect = c(DiagnosticEventType.OWNER_RESET_RECONNECT),
                systemUiTransitionEvents = c(DiagnosticEventType.SYSTEMUI_TRANSITION),
                permissionDeniedFailClosed = c(DiagnosticEventType.OWNER_PERMISSION_DENIED_FAIL_CLOSED)
            ),
            accessibilityEvents = AccessibilityEventSnapshot(
                textChangedTotal = c(DiagnosticEventType.TEXT_CHANGED_TOTAL),
                windowStateChangedTotal = c(DiagnosticEventType.WINDOW_STATE_CHANGED_TOTAL),
                passwordFieldEventsSkipped = c(DiagnosticEventType.PASSWORD_SKIPPED),
                pasteSizedEvents = c(DiagnosticEventType.PASTE_DETECTED),
                eventsDroppedByOwnerGate = c(DiagnosticEventType.EVENT_DROPPED_UNCONFIRMED),
                eventsForwardedToTracker = c(DiagnosticEventType.EVENT_FORWARDED_TO_TRACKER),
                appSwitchBoundaries = c(DiagnosticEventType.APP_SWITCH_BOUNDARY),
                inactivityBoundaries = c(DiagnosticEventType.INACTIVITY_BOUNDARY_CONFIRMED) +
                    c(DiagnosticEventType.INACTIVITY_BOUNDARY_UNCONFIRMED),
                teardownBoundaries = c(DiagnosticEventType.TEARDOWN_BOUNDARY)
            ),
            sessionBoundaries = SessionBoundarySnapshot(
                appSwitch = boundaryStat(DiagnosticEventType.APP_SWITCH_BOUNDARY),
                inactivity = boundaryStat(DiagnosticEventType.INACTIVITY_BOUNDARY_CONFIRMED),
                teardown = boundaryStat(DiagnosticEventType.TEARDOWN_BOUNDARY),
                serviceReconnect = boundaryStat(DiagnosticEventType.SERVICE_CONNECTED)
            ),
            workManager = workManager,
            database = databaseSnapshot,
            resource = currentSample.toResourceSnapshot(sampler.peakTotalPssKb(), sampler.averageTotalPssKb()),
            battery = BatterySnapshot(
                batteryPercent = currentSample.batteryPercent,
                isCharging = currentSample.isCharging,
                batteryTemperatureCelsius = currentSample.batteryTemperatureTenthsC?.let { it / 10.0 },
                label = "Device battery"
            ),
            thermal = ThermalSnapshot(
                thermalStatus = currentSample.thermalStatus?.let(::thermalStatusLabel),
                available = currentSample.thermalStatus != null,
                note = if (currentSample.thermalStatus != null) {
                    "Reported by android.os.PowerManager.getCurrentThermalStatus()."
                } else {
                    "Thermal status unavailable on this device/API."
                }
            ),
            serviceLifecycle = ServiceLifecycleSnapshot(
                currentState = DeveloperDiagnosticsServiceState.state.name,
                connectionCount = DeveloperDiagnosticsServiceState.connectionCount,
                destroyCount = DeveloperDiagnosticsServiceState.destroyCount,
                lastConnectedEpochMs = DeveloperDiagnosticsServiceState.lastConnectedEpochMs,
                lastDisconnectedEpochMs = DeveloperDiagnosticsServiceState.lastDisconnectedEpochMs
            ),
            errors = ErrorSnapshot(
                recentErrors = errorBreakdown.map {
                    ErrorCountRow(errorType = it.eventType, count = it.count, lastOccurrenceEpochMs = it.lastOccurrence)
                },
                available = errorBreakdownResult.isSuccess
            ),
            history = DiagnosticHistorySnapshot(
                latestEvents = latestEvents.map { it.toDiagnosticEventRow() },
                latestSamples = latestSamples.map { it.toResourceSnapshot(sampler.peakTotalPssKb(), sampler.averageTotalPssKb()) },
                retentionDays = DeveloperDiagnosticsRetention.RETENTION_DAYS,
                available = latestEventsResult.isSuccess && latestSamplesResult.isSuccess
            )
        )
    }

    private suspend fun persistSampleIfEnabled(sample: DeveloperDiagnosticSampleEntity) {
        if (!_enabled.value) return
        try {
            dao.insertSample(sample)
        } catch (e: Exception) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.ERROR_DIAGNOSTICS_STORAGE)
        }
    }

    private suspend fun runRetention() {
        try {
            val cutoff = DeveloperDiagnosticsRetention.cutoffEpochMs(System.currentTimeMillis())
            dao.deleteEventsOlderThan(cutoff)
            dao.deleteSamplesOlderThan(cutoff)
        } catch (e: Exception) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.ERROR_DIAGNOSTICS_STORAGE)
        }
    }

    private fun thermalStatusLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "NONE"
        PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
        PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
        PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
        PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
        else -> "UNKNOWN($status)"
    }

    companion object {
        private const val PREFS_NAME = "developer_diagnostics_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val HISTORY_LIMIT = 50
        private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

        @Volatile
        private var instance: DeveloperDiagnosticsRepository? = null

        fun getInstance(context: Context): DeveloperDiagnosticsRepository =
            instance ?: synchronized(this) {
                instance ?: DeveloperDiagnosticsRepository(context).also { instance = it }
            }
    }
}
