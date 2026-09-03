package com.example.vernacularguardian.keyboardprocessing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Verifies the live scheduler-wiring fix against this device's real,
 * androidx.startup-auto-initialized [WorkManager] instance (this test
 * process has the same merged `androidx.startup.InitializationProvider`
 * entry as the real app, so `WorkManager` is already genuinely initialized
 * by the time these tests run - not a test-only substitute instance).
 */
@RunWith(AndroidJUnit4::class)
class KeyboardProcessingInitializerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun initializerCreateSchedulesTheUniquePeriodicWork() {
        KeyboardProcessingInitializer().create(context)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
        val workInfo = workInfos.single()
        assertTrue(
            "expected ENQUEUED or RUNNING, was ${workInfo.state}",
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        )
    }

    @Test
    fun callingScheduleDirectlyTwiceDoesNotCreateDuplicateWork() {
        BehavioralAggregationScheduler.schedule(context)
        BehavioralAggregationScheduler.schedule(context)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
    }

    @Test
    fun invokingTheInitializerTwiceAlsoDoesNotCreateDuplicateWork() {
        KeyboardProcessingInitializer().create(context)
        KeyboardProcessingInitializer().create(context)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
            .get()

        assertEquals(1, workInfos.size)
        assertEquals(
            TimeUnit.HOURS.toMillis(24),
            BehavioralAggregationScheduler.buildRequest().workSpec.intervalDuration
        )
    }
}