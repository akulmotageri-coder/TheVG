package com.example.vernacularguardian.keyboardprocessing.aggregation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase

/**
 * Scheduled (every 24h, battery-not-low) entry point for daily aggregation +
 * retention. Runs on WorkManager's own background dispatcher (never the
 * AccessibilityService's main thread) and delegates entirely to
 * [BehavioralAggregationEngine.runOnce] — the same implementation any
 * internal on-demand/demo trigger uses, so the two paths cannot drift apart.
 */
class BehavioralAggregationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        DeveloperDiagnosticCounters.increment(DiagnosticEventType.WORKER_EXECUTION)
        val database = KeyboardProcessingDatabase.getInstance(applicationContext)
        val succeeded = BehavioralAggregationEngine(database).runOnce()
        DeveloperDiagnosticCounters.increment(
            if (succeeded) DiagnosticEventType.WORKER_SUCCESS else DiagnosticEventType.WORKER_FAILURE
        )
        return if (succeeded) Result.success() else Result.failure()
    }
}
