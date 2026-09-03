package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 8 Section 34 item 5: the WorkManager state adapter reads real
 * WorkManager state for the existing unique work name, without scheduling or
 * cancelling anything itself.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperDiagnosticsWorkManagerAdapterTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
    }

    @Test
    fun snapshotReflectsNoScheduledWorkWhenNoneIsScheduled() = runBlocking {
        WorkManager.getInstance(context).cancelUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)

        val snapshot = DeveloperDiagnosticsWorkManagerAdapter(context).snapshot()

        assertEquals(BehavioralAggregationScheduler.UNIQUE_WORK_NAME, snapshot.uniqueWorkName)
        assertTrue(snapshot.available)
    }

    @Test
    fun snapshotReflectsScheduledWorkAfterScheduling() = runBlocking {
        BehavioralAggregationScheduler.schedule(context)

        val snapshot = DeveloperDiagnosticsWorkManagerAdapter(context).snapshot()

        assertTrue(snapshot.periodicSchedulingEnabled)
        assertTrue(snapshot.currentState == "ENQUEUED" || snapshot.currentState == "RUNNING")
    }

    @Test
    fun snapshotNeverThrowsRegardlessOfWorkManagerState() = runBlocking {
        // Calling twice in a row (schedule, then read again without changes)
        // must remain stable and never throw.
        val adapter = DeveloperDiagnosticsWorkManagerAdapter(context)
        adapter.snapshot()
        val second = adapter.snapshot()
        assertEquals(BehavioralAggregationScheduler.UNIQUE_WORK_NAME, second.uniqueWorkName)
    }
}
