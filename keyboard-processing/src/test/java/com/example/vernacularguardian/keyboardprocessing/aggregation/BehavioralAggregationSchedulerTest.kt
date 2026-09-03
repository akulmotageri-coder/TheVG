package com.example.vernacularguardian.keyboardprocessing.aggregation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Verifies the scheduling *definition* deterministically, without needing a
 * live WorkManager/Context: [androidx.work.PeriodicWorkRequest] and
 * [androidx.work.Constraints] are plain request/config objects, so their
 * built [androidx.work.WorkRequest.workSpec] can be inspected directly in a
 * JVM unit test. Actually enqueuing (and confirming
 * [androidx.work.ExistingPeriodicWorkPolicy.KEEP] prevents a duplicate on a
 * second call) requires a real WorkManager runtime and is documented as not
 * covered by this JVM-only test in `Sprint_4.md`.
 */
class BehavioralAggregationSchedulerTest {

    @Test
    fun `request repeats every 24 hours`() {
        val request = BehavioralAggregationScheduler.buildRequest()
        assertEquals(TimeUnit.HOURS.toMillis(24), request.workSpec.intervalDuration)
    }

    @Test
    fun `request requires battery not low`() {
        val request = BehavioralAggregationScheduler.buildRequest()
        assertTrue(request.workSpec.constraints.requiresBatteryNotLow())
    }

    @Test
    fun `request has no unrelated constraints`() {
        val request = BehavioralAggregationScheduler.buildRequest()
        val constraints = request.workSpec.constraints
        assertEquals(androidx.work.NetworkType.NOT_REQUIRED, constraints.requiredNetworkType)
        assertEquals(false, constraints.requiresCharging())
        assertEquals(false, constraints.requiresDeviceIdle())
        assertEquals(false, constraints.requiresStorageNotLow())
    }

    @Test
    fun `worker class is BehavioralAggregationWorker`() {
        val request = BehavioralAggregationScheduler.buildRequest()
        assertEquals(
            BehavioralAggregationWorker::class.java.name,
            request.workSpec.workerClassName
        )
    }
}
