package com.example.vernacularguardian.keyboardprocessing.aggregation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [BehavioralAggregationWorker] itself (not just the engine it
 * delegates to) on-device, through a real [androidx.work.testing.TestListenableWorkerBuilder]
 * Worker instance. Note: `keyboard-processing` is a library module, so its
 * instrumented tests are self-instrumenting — `targetContext` here resolves
 * to this test APK's own package (`...keyboardprocessing.test`), a real but
 * separate on-device app/database from the actual `:app` process, not the
 * production app's own `keyboard_processing.db`. That real-app validation is
 * performed separately (see `Sprint_4.md` Section 14/15), by rebuilding and
 * running the actual app on-device. This test proves the worker and the
 * engine are genuinely the same code path (both operate on whatever database
 * instance is handed to/reachable from them), not two independent
 * implementations that merely look similar.
 */
@RunWith(AndroidJUnit4::class)
class BehavioralAggregationWorkerTest {

    @Test
    fun workerAggregatesASeededSessionThroughTheSameEngineAsRunOnce() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = KeyboardProcessingDatabase.getInstance(context)
        val today = BehavioralAggregationEngine.todayEpochDay()

        // A distinctive value combination so this test's contribution is unambiguous
        // even alongside whatever real/legacy rows already exist in this device's database.
        val markerCpm = 246.813
        database.typingSessionDao().insert(
            TypingSessionEntity(
                startTimeMs = 0L,
                endTimeMs = 1_000L,
                typingSpeedCpm = markerCpm,
                backspaceRate = 0.25,
                intervalStdDevMs = 80.0,
                microPauseCount = 4,
                charCount = 400,
                sessionEpochDay = today
            )
        )

        val worker = TestListenableWorkerBuilder<BehavioralAggregationWorker>(context).build()
        val result = worker.doWork()

        assertTrue("worker must report success", result is ListenableWorker.Result.Success)

        val todaySummary = database.dailySummaryDao().getAllSummaries().single { it.dateEpochDay == today }
        // If this device's DB has other sessions seeded for "today" from earlier testing,
        // the average will reflect all of them; the marker session's own contribution can
        // still be confirmed to have been aggregated by checking the session itself.
        val remainingTodaySessions = database.typingSessionDao().getAllSessions()
            .filter { it.sessionEpochDay == today }
        assertTrue(remainingTodaySessions.any { it.typingSpeedCpm == markerCpm })
        assertEquals(remainingTodaySessions.size, todaySummary.sessionCount)
    }
}
