package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.content.Context
import androidx.work.WorkManager
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Developer-only, read-only adapter over WorkManager's own state for
 * [BehavioralAggregationScheduler.UNIQUE_WORK_NAME] (Sprint 8 Section 15).
 * Never schedules, cancels, or otherwise mutates work — only reads
 * `getWorkInfosForUniqueWork`, the same unique work name the existing
 * scheduler already uses. No new scheduler is created here.
 *
 * [WorkManagerSnapshot.configuredIntervalMs]/[WorkManagerSnapshot.configuredRequiresBatteryNotLow]
 * are a documented exception: `WorkInfo`'s public API does not expose a work
 * request's interval or constraints, so these are static literals mirroring
 * [BehavioralAggregationScheduler.buildRequest] rather than a live read —
 * named accordingly (Sprint 9 fabrication-audit fix) rather than presented
 * as if queried from `WorkInfo`.
 */
class DeveloperDiagnosticsWorkManagerAdapter(private val context: Context) {

    suspend fun snapshot(): WorkManagerSnapshot = withContext(Dispatchers.IO) {
        try {
            val infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
                .get()
            val info = infos.firstOrNull()
            WorkManagerSnapshot(
                uniqueWorkName = BehavioralAggregationScheduler.UNIQUE_WORK_NAME,
                available = true,
                periodicSchedulingEnabled = info != null,
                configuredIntervalMs = TimeUnit.HOURS.toMillis(24),
                configuredRequiresBatteryNotLow = true,
                currentState = info?.state?.name,
                runAttemptCount = info?.runAttemptCount ?: 0,
                observedExecutions = DeveloperDiagnosticCounters.get(DiagnosticEventType.WORKER_EXECUTION),
                observedFailures = DeveloperDiagnosticCounters.get(DiagnosticEventType.WORKER_FAILURE),
                scheduleCalledCount = DeveloperDiagnosticCounters.get(DiagnosticEventType.SCHEDULE_CALLED),
                cancelCalledCount = DeveloperDiagnosticCounters.get(DiagnosticEventType.WORK_CANCELLED)
            )
        } catch (e: Exception) {
            DeveloperDiagnosticCounters.increment(DiagnosticEventType.ERROR_WORKMANAGER)
            WorkManagerSnapshot(
                uniqueWorkName = BehavioralAggregationScheduler.UNIQUE_WORK_NAME,
                available = false,
                periodicSchedulingEnabled = false,
                configuredIntervalMs = TimeUnit.HOURS.toMillis(24),
                configuredRequiresBatteryNotLow = true,
                currentState = null,
                runAttemptCount = 0,
                observedExecutions = DeveloperDiagnosticCounters.get(DiagnosticEventType.WORKER_EXECUTION),
                observedFailures = DeveloperDiagnosticCounters.get(DiagnosticEventType.WORKER_FAILURE),
                scheduleCalledCount = DeveloperDiagnosticCounters.get(DiagnosticEventType.SCHEDULE_CALLED),
                cancelCalledCount = DeveloperDiagnosticCounters.get(DiagnosticEventType.WORK_CANCELLED)
            )
        }
    }
}
