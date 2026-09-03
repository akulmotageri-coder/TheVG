package com.example.vernacularguardian.keyboardprocessing.aggregation

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real Room aggregation/retention logic against a fresh in-memory
 * database on the connected device/emulator — real Room and real SQLite
 * code executing on a real Android runtime, isolated per test so seeded
 * data and assertions are exact and repeatable. Every expected value below
 * is hand-computed from the PRD-exact formulas in the Sprint 4 brief, not
 * merely "a row exists" checks.
 */
@RunWith(AndroidJUnit4::class)
class BehavioralAggregationEngineTest {

    private lateinit var database: KeyboardProcessingDatabase
    private lateinit var engine: BehavioralAggregationEngine
    private val today = BehavioralAggregationEngine.todayEpochDay()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, KeyboardProcessingDatabase::class.java).build()
        engine = BehavioralAggregationEngine(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun session(
        sessionEpochDay: Long,
        typingSpeedCpm: Double,
        backspaceRate: Double,
        intervalStdDevMs: Double,
        microPauseCount: Int,
        charCount: Int
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

    @Test
    fun singleDayAggregationMatchesHandComputedFormulas() = runBlocking {
        // Session 1: cpm=60, backspaceRate=0.10, intervalStdDevMs=100, microPauseCount=2, charCount=100
        // Session 2: cpm=120, backspaceRate=0.20, intervalStdDevMs=300, microPauseCount=3, charCount=200
        database.typingSessionDao().insert(session(today, 60.0, 0.10, 100.0, 2, 100))
        database.typingSessionDao().insert(session(today, 120.0, 0.20, 300.0, 3, 200))

        assertTrue(engine.runOnce())

        val summaries = database.dailySummaryDao().getAllSummaries()
        assertEquals(1, summaries.size)
        val summary = summaries.single()

        assertEquals(today, summary.dateEpochDay)
        assertEquals(90.0, summary.avgTypingSpeedCpm, 1e-9) // AVG(60, 120)
        assertEquals(0.15, summary.avgBackspaceRate, 1e-9) // AVG(0.10, 0.20)
        assertEquals(200.0, summary.intervalStdDevMs, 1e-9) // AVG(100, 300)
        assertEquals(1.6666666666666667, summary.pauseFrequencyPer100Chars, 1e-9) // (2+3)/(100+200)*100
        assertEquals(2, summary.sessionCount)
        assertEquals(13.916666666666666, summary.riskContributionScore, 1e-9)
        assertTrue(summary.isReliable)

        // Both sessions are "today" - well within the 30-day retention window - so retention must not touch them.
        assertEquals(2, database.typingSessionDao().getAllSessions().size)
    }

    @Test
    fun multipleDaysAggregateIndependentlyWithNoCrossDayContamination() = runBlocking {
        val otherDay = today - 5
        database.typingSessionDao().insert(session(today, 60.0, 0.10, 100.0, 2, 100))
        database.typingSessionDao().insert(session(today, 120.0, 0.20, 300.0, 3, 200))
        database.typingSessionDao().insert(session(otherDay, 200.0, 0.05, 50.0, 1, 50))

        assertTrue(engine.runOnce())

        val summaries = database.dailySummaryDao().getAllSummaries().associateBy { it.dateEpochDay }
        assertEquals(2, summaries.size)

        val todaySummary = summaries.getValue(today)
        assertEquals(90.0, todaySummary.avgTypingSpeedCpm, 1e-9)
        assertEquals(0.15, todaySummary.avgBackspaceRate, 1e-9)
        assertEquals(200.0, todaySummary.intervalStdDevMs, 1e-9)
        assertEquals(2, todaySummary.sessionCount)

        val otherDaySummary = summaries.getValue(otherDay)
        assertEquals(200.0, otherDaySummary.avgTypingSpeedCpm, 1e-9)
        assertEquals(0.05, otherDaySummary.avgBackspaceRate, 1e-9)
        assertEquals(50.0, otherDaySummary.intervalStdDevMs, 1e-9)
        assertEquals(2.0, otherDaySummary.pauseFrequencyPer100Chars, 1e-9) // 1/50*100
        assertEquals(1, otherDaySummary.sessionCount)
    }

    @Test
    fun repeatedAggregationOfUnchangedDataIsIdempotent() = runBlocking {
        database.typingSessionDao().insert(session(today, 60.0, 0.10, 100.0, 2, 100))
        database.typingSessionDao().insert(session(today, 120.0, 0.20, 300.0, 3, 200))

        assertTrue(engine.runOnce())
        val afterFirstRun = database.dailySummaryDao().getAllSummaries()
        assertEquals(1, afterFirstRun.size)

        assertTrue(engine.runOnce())
        val afterSecondRun = database.dailySummaryDao().getAllSummaries()

        assertEquals(1, afterSecondRun.size) // no duplicate dateEpochDay row
        assertEquals(afterFirstRun.single(), afterSecondRun.single()) // byte-identical result
    }

    @Test
    fun zeroCharacterDayProducesZeroPauseFrequencyNotDivisionByZero() = runBlocking {
        database.typingSessionDao().insert(session(today, 0.0, 1.0, 0.0, 0, 0))

        assertTrue(engine.runOnce())

        val summary = database.dailySummaryDao().getAllSummaries().single { it.dateEpochDay == today }
        assertEquals(0.0, summary.pauseFrequencyPer100Chars, 0.0)
        assertTrue(summary.pauseFrequencyPer100Chars.isFinite()) // explicitly not NaN/Infinity
    }

    @Test
    fun retentionDeletesOlderThan30DaysAndKeepsBoundaryAndNewer() = runBlocking {
        val olderThanCutoff = today - 31 // strictly older than today-30 -> deleted
        val exactlyOnCutoff = today - 30 // NOT strictly older -> retained
        val newerThanCutoff = today - 29 // retained

        database.typingSessionDao().insert(session(olderThanCutoff, 50.0, 0.1, 100.0, 1, 100))
        database.typingSessionDao().insert(session(exactlyOnCutoff, 50.0, 0.1, 100.0, 1, 100))
        database.typingSessionDao().insert(session(newerThanCutoff, 50.0, 0.1, 100.0, 1, 100))

        assertTrue(engine.runOnce())

        // All three days were aggregated before retention ran - including the day about to be purged.
        val summaryDays = database.dailySummaryDao().getAllSummaries().map { it.dateEpochDay }.toSet()
        assertTrue(summaryDays.contains(olderThanCutoff))
        assertTrue(summaryDays.contains(exactlyOnCutoff))
        assertTrue(summaryDays.contains(newerThanCutoff))

        val remainingSessionDays = database.typingSessionDao().getAllSessions().map { it.sessionEpochDay }.toSet()
        assertTrue("older-than-cutoff session must be deleted", !remainingSessionDays.contains(olderThanCutoff))
        assertTrue("exactly-on-cutoff session must be retained", remainingSessionDays.contains(exactlyOnCutoff))
        assertTrue("newer-than-cutoff session must be retained", remainingSessionDays.contains(newerThanCutoff))
    }

    @Test
    fun aggregationCapturesAnOldSessionsContributionBeforeItIsDeleted() = runBlocking {
        val olderThanCutoff = today - 45
        database.typingSessionDao().insert(session(olderThanCutoff, 77.0, 0.33, 150.0, 4, 400))

        assertTrue(engine.runOnce())

        // 1. Its daily summary contribution is included, with the exact hand-computed values.
        val summary = database.dailySummaryDao().getAllSummaries().single { it.dateEpochDay == olderThanCutoff }
        assertEquals(77.0, summary.avgTypingSpeedCpm, 1e-9)
        assertEquals(0.33, summary.avgBackspaceRate, 1e-9)
        assertEquals(150.0, summary.intervalStdDevMs, 1e-9)
        assertEquals(1.0, summary.pauseFrequencyPer100Chars, 1e-9) // 4/400*100
        assertEquals(1, summary.sessionCount)

        // 2. Only after that, the now-purgeable raw session row is actually gone.
        val remaining = database.typingSessionDao().getAllSessions()
        assertTrue(remaining.none { it.sessionEpochDay == olderThanCutoff })
    }

    @Test
    fun legacySprint3RowsWithNullSessionEpochDayAreIgnoredByAggregationAndRetention() = runBlocking {
        val legacyRow = TypingSessionEntity(
            startTimeMs = 111L,
            endTimeMs = 222L,
            typingSpeedCpm = 999.0,
            backspaceRate = 0.5,
            intervalStdDevMs = 50.0,
            microPauseCount = 9,
            charCount = 500,
            sessionEpochDay = null
        )
        database.typingSessionDao().insert(legacyRow)

        assertTrue(engine.runOnce())

        // Not aggregated: no summary row could plausibly correspond to a NULL day.
        assertEquals(0, database.dailySummaryDao().getAllSummaries().size)
        // Not deleted by retention either - SQL `NULL < x` is never true.
        val remaining = database.typingSessionDao().getAllSessions()
        assertEquals(1, remaining.size)
        assertNull(remaining.single().sessionEpochDay)
    }
}
