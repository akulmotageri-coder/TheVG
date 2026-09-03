package com.example.vernacularguardian.keyboardprocessing.aggregation

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Idempotent WorkManager scheduling for [BehavioralAggregationWorker]: once
 * every 24 hours, only while the device is not battery-low. `buildRequest()`
 * is exposed separately from `schedule()` so the request's configuration
 * (interval, constraints) can be verified with a plain, deterministic unit
 * test that needs no WorkManager runtime/Context.
 */
object BehavioralAggregationScheduler {

    const val UNIQUE_WORK_NAME = "behavioral_aggregation_daily"

    fun buildRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<BehavioralAggregationWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

    /**
     * [ExistingPeriodicWorkPolicy.KEEP]: if the unique periodic work is
     * already scheduled, this call is a no-op — calling `schedule` more than
     * once never creates a duplicate job.
     */
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest()
        )
    }
}
