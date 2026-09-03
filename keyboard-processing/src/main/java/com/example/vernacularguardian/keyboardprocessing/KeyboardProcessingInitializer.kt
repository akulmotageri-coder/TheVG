package com.example.vernacularguardian.keyboardprocessing

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationScheduler

/**
 * Schedules [BehavioralAggregationScheduler]'s daily aggregation work once,
 * automatically, at app process startup — using `androidx.startup`'s
 * standard App Startup mechanism rather than any change to `:app`'s own
 * code or a public product API. Declares an explicit startup-order
 * dependency on [WorkManagerInitializer] so `WorkManager.getInstance(...)`
 * is guaranteed already initialized when [create] runs, rather than relying
 * on incidental manifest-merge ordering.
 *
 * `BehavioralAggregationScheduler.schedule` itself is idempotent
 * ([androidx.work.ExistingPeriodicWorkPolicy.KEEP]), so this being invoked
 * on every process start is safe and never creates duplicate periodic work.
 */
class KeyboardProcessingInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        BehavioralAggregationScheduler.schedule(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(WorkManagerInitializer::class.java)
}