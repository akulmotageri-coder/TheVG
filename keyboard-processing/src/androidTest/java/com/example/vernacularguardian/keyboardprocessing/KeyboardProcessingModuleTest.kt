package com.example.vernacularguardian.keyboardprocessing

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationScheduler
import com.example.vernacularguardian.keyboardprocessing.owner.OwnerSessionManager
import com.example.vernacularguardian.keyboardprocessing.service.KeystrokeAccessibilityService
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [KeyboardProcessingModule] against real implementations: a real
 * in-memory Room database (built directly, not via the process-wide
 * `getInstance` singleton, so each test is isolated), the real device
 * `WorkManager`/`AccessibilityManager`, and the real [OwnerSessionManager]
 * singleton (reset per test). Covers master-prompt Section 27 items 1-12 and
 * the Section 28 end-to-end integration path.
 */
@RunWith(AndroidJUnit4::class)
class KeyboardProcessingModuleTest {

    private lateinit var appContext: Context
    private lateinit var database: KeyboardProcessingDatabase
    private lateinit var module: KeyboardProcessingModule
    private val today = BehavioralAggregationEngine.todayEpochDay()

    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(appContext, KeyboardProcessingDatabase::class.java).build()
        module = KeyboardProcessingModule(appContext, database)
        OwnerSessionManager.resetForNewSession()
    }

    @After
    fun tearDown() {
        database.close()
        OwnerSessionManager.resetForNewSession()
    }

    private fun session(
        sessionEpochDay: Long,
        typingSpeedCpm: Double = 60.0,
        backspaceRate: Double = 0.1,
        intervalStdDevMs: Double = 100.0,
        microPauseCount: Int = 2,
        charCount: Int = 100
    ) = TypingSessionEntity(
        startTimeMs = 0L,
        endTimeMs = 1_000L,
        typingSpeedCpm = typingSpeedCpm,
        backspaceRate = backspaceRate,
        intervalStdDevMs = intervalStdDevMs,
        microPauseCount = microPauseCount,
        charCount = charCount,
        sessionEpochDay = sessionEpochDay
    )

    // 1. module can be instantiated
    @Test
    fun moduleCanBeInstantiated() {
        assertNotNull(module)
    }

    // 2. startPassiveCapture delegates to scheduler
    @Test
    fun startPassiveCaptureSchedulesTheSameUniqueWorkAsTheScheduler() {
        module.startPassiveCapture()

        val workInfos = WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
            .get()

        assertTrue(workInfos.isNotEmpty())
        assertTrue(workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING })
    }

    // 3. stopPassiveCapture cancels the same unique work
    @Test
    fun stopPassiveCaptureCancelsTheSchedulerUniqueWork() {
        module.startPassiveCapture()
        module.stopPassiveCapture()

        val workInfos = WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWork(BehavioralAggregationScheduler.UNIQUE_WORK_NAME)
            .get()

        assertTrue(workInfos.isNotEmpty())
        assertTrue(workInfos.all { it.state == WorkInfo.State.CANCELLED })
    }

    // 4. isPassiveCaptureAuthorized reflects service state (and only that state)
    @Test
    fun isPassiveCaptureAuthorizedReflectsOnlyTheOsAccessibilityState() {
        val accessibilityManager =
            appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val targetId = ComponentName(appContext, KeystrokeAccessibilityService::class.java).flattenToShortString()
        val expected = accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id == targetId }

        assertEquals(expected, module.isPassiveCaptureAuthorized())

        // Owner-confirmation state must never influence this result.
        OwnerSessionManager.confirmCurrentSession()
        assertEquals(expected, module.isPassiveCaptureAuthorized())
        OwnerSessionManager.resetForNewSession()
        assertEquals(expected, module.isPassiveCaptureAuthorized())
    }

    // 5. forceAggregateNow delegates to the existing aggregation engine
    @Test
    fun forceAggregateNowRunsTheSameAggregationAsTheEngine() = runBlocking {
        database.typingSessionDao().insert(session(today, typingSpeedCpm = 90.0))

        module.forceAggregateNow()

        val summaries = database.dailySummaryDao().getAllSummaries()
        assertEquals(1, summaries.size)
        assertEquals(today, summaries.single().dateEpochDay)
    }

    // 6. needsOwnerConfirmationForCurrentSession delegates correctly
    @Test
    fun needsOwnerConfirmationForCurrentSessionDelegatesToOwnerSessionManager() {
        assertTrue(module.needsOwnerConfirmationForCurrentSession())
        OwnerSessionManager.confirmCurrentSession()
        assertFalse(module.needsOwnerConfirmationForCurrentSession())
    }

    // 7. confirmOwnerForCurrentSession delegates correctly
    @Test
    fun confirmOwnerForCurrentSessionDelegatesToOwnerSessionManager() {
        module.confirmOwnerForCurrentSession()
        assertTrue(OwnerSessionManager.isConfirmed())
        assertFalse(module.needsOwnerConfirmationForCurrentSession())
    }

    // 8. revokeOwnerForCurrentSession delegates correctly
    @Test
    fun revokeOwnerForCurrentSessionDelegatesToOwnerSessionManager() {
        OwnerSessionManager.confirmCurrentSession()
        module.revokeOwnerForCurrentSession()
        assertFalse(OwnerSessionManager.isConfirmed())
        assertTrue(module.needsOwnerConfirmationForCurrentSession())
    }

    // 9. BehavioralDailySummary mapping is exact
    @Test
    fun behavioralDailySummaryMappingMatchesHandComputedFormulas() = runBlocking {
        database.typingSessionDao().insert(
            session(today, typingSpeedCpm = 60.0, backspaceRate = 0.10, intervalStdDevMs = 100.0, microPauseCount = 2, charCount = 100)
        )
        database.typingSessionDao().insert(
            session(today, typingSpeedCpm = 120.0, backspaceRate = 0.20, intervalStdDevMs = 300.0, microPauseCount = 3, charCount = 200)
        )

        module.forceAggregateNow()

        val summary = module.observeDailyBehavioralSummary().first()
        assertEquals(today, summary.dateEpochDay)
        assertEquals(90.0, summary.avgTypingSpeedCpm, 1e-9)
        assertEquals(0.15, summary.avgBackspaceRate, 1e-9)
        assertEquals(200.0, summary.intervalStdDevMs, 1e-9)
        assertEquals(1.6666666666666667, summary.pauseFrequencyPer100Chars, 1e-9)
        assertEquals(2, summary.sessionCount)
        assertTrue(summary.isReliable)
    }

    // 10. no internal Room/entity type leaks through the API
    @Test
    fun observedSummaryIsThePublicTypeNotARoomEntity() = runBlocking {
        val summary = module.observeDailyBehavioralSummary().first()
        assertEquals(BehavioralDailySummary::class.java, summary.javaClass)
    }

    // 11. observeDailyBehavioralSummary returns the correct day's summary
    @Test
    fun observeDailyBehavioralSummaryIgnoresOtherDaysData() = runBlocking {
        val otherDay = today - 5
        database.typingSessionDao().insert(session(otherDay, typingSpeedCpm = 999.0, charCount = 500))
        module.forceAggregateNow()

        val summary = module.observeDailyBehavioralSummary().first()
        assertEquals(today, summary.dateEpochDay)
        assertEquals(0, summary.sessionCount)
        assertFalse(summary.isReliable)
    }

    // 12. Flow emits updated values when the underlying summary changes
    @Test
    fun observeDailyBehavioralSummaryEmitsAgainWhenTodaysSummaryChanges() = runBlocking {
        val emissions = mutableListOf<BehavioralDailySummary>()
        val collector = launch {
            module.observeDailyBehavioralSummary().take(2).toList(emissions)
        }

        // Let the collector attach and receive its initial (no-row-yet) emission.
        kotlinx.coroutines.delay(300)
        database.typingSessionDao().insert(session(today))
        module.forceAggregateNow()

        collector.join()

        assertEquals(2, emissions.size)
        assertEquals(0, emissions[0].sessionCount)
        assertEquals(1, emissions[1].sessionCount)
    }

    // Section 28: end-to-end integration — seed typing_sessions -> forceAggregateNow()
    // -> daily_summaries -> observeDailyBehavioralSummary() -> BehavioralDailySummary.
    @Test
    fun endToEndSeedAggregateAndObserve() = runBlocking {
        database.typingSessionDao().insert(
            session(today, typingSpeedCpm = 77.0, backspaceRate = 0.33, intervalStdDevMs = 150.0, microPauseCount = 4, charCount = 400)
        )

        module.forceAggregateNow()

        val rawSummaries = database.dailySummaryDao().getAllSummaries()
        assertEquals(1, rawSummaries.size)

        val publicSummary = module.observeDailyBehavioralSummary().first()
        assertEquals(rawSummaries.single().dateEpochDay, publicSummary.dateEpochDay)
        assertEquals(rawSummaries.single().avgTypingSpeedCpm, publicSummary.avgTypingSpeedCpm, 1e-9)
        assertEquals(rawSummaries.single().riskContributionScore, publicSummary.riskContributionScore, 1e-9)
        assertEquals(1, publicSummary.sessionCount)
        assertTrue(publicSummary.isReliable)
    }
}
