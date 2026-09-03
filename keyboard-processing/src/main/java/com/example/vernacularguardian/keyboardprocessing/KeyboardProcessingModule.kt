package com.example.vernacularguardian.keyboardprocessing

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.work.WorkManager
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationScheduler
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType
import com.example.vernacularguardian.keyboardprocessing.owner.OwnerSessionManager
import com.example.vernacularguardian.keyboardprocessing.service.KeystrokeAccessibilityService
import com.example.vernacularguardian.keyboardprocessing.storage.DailySummaryEntity
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Owns/wires the module's existing internal implementations (Room database,
 * [BehavioralAggregationEngine], [BehavioralAggregationScheduler],
 * [OwnerSessionManager]) and implements [KeyboardProcessingApi] as the only
 * class external callers need to reference. Every method below delegates to
 * an already-existing Sprint 1-5 implementation; none is reimplemented here.
 */
class KeyboardProcessingModule internal constructor(
    private val appContext: Context,
    private val database: KeyboardProcessingDatabase
) : KeyboardProcessingApi {

    constructor(context: Context) : this(
        context.applicationContext,
        KeyboardProcessingDatabase.getInstance(context.applicationContext)
    )

    private val aggregationEngine = BehavioralAggregationEngine(database)

    private val accessibilityManager =
        appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val keystrokeServiceComponent =
        ComponentName(appContext, KeystrokeAccessibilityService::class.java)

    override fun startPassiveCapture() {
        BehavioralAggregationScheduler.schedule(appContext)
        // Sprint 8 observational hook: counts every call, including no-op
        // repeats the scheduler's own ExistingPeriodicWorkPolicy.KEEP absorbs.
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.SCHEDULE_CALLED)
    }

    override fun stopPassiveCapture() {
        WorkManager.getInstance(appContext)
            .cancelUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.WORK_CANCELLED)
    }

    override fun isPassiveCaptureAuthorized(): Boolean {
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val targetId = keystrokeServiceComponent.flattenToShortString()
        return enabledServices.any { it.id == targetId }
    }

    override fun observeDailyBehavioralSummary(): Flow<BehavioralDailySummary> {
        val today = BehavioralAggregationEngine.todayEpochDay()
        return database.dailySummaryDao().observeByEpochDay(today)
            .map { entity -> entity?.toPublic() ?: emptySummaryFor(today) }
    }

    override suspend fun forceAggregateNow() {
        aggregationEngine.runOnce()
    }

    override fun needsOwnerConfirmationForCurrentSession(): Boolean =
        !OwnerSessionManager.isConfirmed()

    override fun confirmOwnerForCurrentSession() {
        OwnerSessionManager.confirmCurrentSession()
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_CONFIRMED)
    }

    override fun revokeOwnerForCurrentSession() {
        OwnerSessionManager.revokeCurrentSession()
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.OWNER_REVOKED)
    }

    companion object {
        @Volatile
        private var instance: KeyboardProcessingModule? = null

        /** Process-wide singleton, mirroring [KeyboardProcessingDatabase.getInstance]'s convention. */
        fun getInstance(context: Context): KeyboardProcessingModule =
            instance ?: synchronized(this) {
                instance ?: KeyboardProcessingModule(context).also { instance = it }
            }
    }
}

private fun DailySummaryEntity.toPublic() = BehavioralDailySummary(
    dateEpochDay = dateEpochDay,
    avgTypingSpeedCpm = avgTypingSpeedCpm,
    avgBackspaceRate = avgBackspaceRate,
    intervalStdDevMs = intervalStdDevMs,
    pauseFrequencyPer100Chars = pauseFrequencyPer100Chars,
    sessionCount = sessionCount,
    riskContributionScore = riskContributionScore,
    isReliable = isReliable
)

/**
 * PRD-consistent representation for "no daily summary row exists yet for
 * today" (implementation decision — the PRD is silent on this exact mapping):
 * a zero-metric, zero-session, unreliable summary for today's date, rather
 * than exposing Room's nullability directly through the public API.
 */
private fun emptySummaryFor(dateEpochDay: Long) = BehavioralDailySummary(
    dateEpochDay = dateEpochDay,
    avgTypingSpeedCpm = 0.0,
    avgBackspaceRate = 0.0,
    intervalStdDevMs = 0.0,
    pauseFrequencyPer100Chars = 0.0,
    sessionCount = 0,
    riskContributionScore = 0.0,
    isReliable = false
)