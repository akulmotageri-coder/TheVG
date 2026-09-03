package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 8 Section 34 items 1-4: developer-diagnostics storage
 * initialization, insert/read, clearing, and 7-day-retention cleanup —
 * against a real, isolated (in-memory) Room database on the connected
 * device/emulator, matching the existing [com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngineTest]
 * convention.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperDiagnosticsDaoTest {

    private lateinit var database: DeveloperDiagnosticsDatabase
    private lateinit var dao: DeveloperDiagnosticsDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, DeveloperDiagnosticsDatabase::class.java).build()
        dao = database.diagnosticsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun event(timestampEpochMs: Long, type: DiagnosticEventType) = DeveloperDiagnosticEventEntity(
        timestampEpochMs = timestampEpochMs,
        eventType = type.name
    )

    private fun sample(timestampEpochMs: Long) = DeveloperDiagnosticSampleEntity(
        timestampEpochMs = timestampEpochMs,
        processCpuTimeMs = 100L,
        wallClockElapsedMs = 200L,
        cpuUtilizationPercent = 5.0,
        totalPssKb = 1000,
        javaHeapKb = 100,
        nativeHeapKb = 50,
        privateDirtyKb = 20,
        batteryPercent = 80,
        isCharging = false,
        batteryTemperatureTenthsC = 250,
        thermalStatus = null,
        processUptimeMs = 1000L
    )

    // 1. Storage initializes and starts empty.
    @Test
    fun databaseInitializesEmpty() = runBlocking {
        assertEquals(0L, dao.totalEventCount())
        assertEquals(0L, dao.totalSampleCount())
    }

    // 2. Insert/read a diagnostic event.
    @Test
    fun insertedEventCanBeReadBack() = runBlocking {
        dao.insertEvent(event(1000L, DiagnosticEventType.OWNER_PROMPT_SHOWN))

        val events = dao.latestEvents(limit = 10)
        assertEquals(1, events.size)
        assertEquals(DiagnosticEventType.OWNER_PROMPT_SHOWN.name, events.single().eventType)
        assertEquals(1L, dao.countEventsByType(DiagnosticEventType.OWNER_PROMPT_SHOWN.name))
    }

    @Test
    fun insertedSampleCanBeReadBack() = runBlocking {
        dao.insertSample(sample(2000L))

        val samples = dao.latestSamples(limit = 10)
        assertEquals(1, samples.size)
        assertEquals(2000L, samples.single().timestampEpochMs)
        assertEquals(1000, samples.single().totalPssKb)
    }

    @Test
    fun latestTimestampByTypeReturnsTheMostRecentOccurrence() = runBlocking {
        dao.insertEvent(event(1000L, DiagnosticEventType.APP_SWITCH_BOUNDARY))
        dao.insertEvent(event(3000L, DiagnosticEventType.APP_SWITCH_BOUNDARY))
        dao.insertEvent(event(2000L, DiagnosticEventType.APP_SWITCH_BOUNDARY))

        assertEquals(3000L, dao.latestTimestampByType(DiagnosticEventType.APP_SWITCH_BOUNDARY.name))
    }

    @Test
    fun countEventsByTypeSinceOnlyCountsEventsAtOrAfterTheCutoff() = runBlocking {
        dao.insertEvent(event(500L, DiagnosticEventType.TEARDOWN_BOUNDARY))
        dao.insertEvent(event(1500L, DiagnosticEventType.TEARDOWN_BOUNDARY))
        dao.insertEvent(event(2500L, DiagnosticEventType.TEARDOWN_BOUNDARY))

        assertEquals(2L, dao.countEventsByTypeSince(DiagnosticEventType.TEARDOWN_BOUNDARY.name, sinceEpochMs = 1000L))
    }

    @Test
    fun errorBreakdownGroupsOnlyErrorPrefixedEventTypes() = runBlocking {
        dao.insertEvent(event(1L, DiagnosticEventType.ERROR_DATABASE))
        dao.insertEvent(event(2L, DiagnosticEventType.ERROR_DATABASE))
        dao.insertEvent(event(3L, DiagnosticEventType.OWNER_CONFIRMED))

        val breakdown = dao.errorBreakdown()
        assertEquals(1, breakdown.size)
        assertEquals(DiagnosticEventType.ERROR_DATABASE.name, breakdown.single().eventType)
        assertEquals(2L, breakdown.single().count)
    }

    // 3. Clear diagnostics history.
    @Test
    fun clearEventsAndClearSamplesEmptyBothTables() = runBlocking {
        dao.insertEvent(event(1L, DiagnosticEventType.SESSION_PERSISTED))
        dao.insertSample(sample(1L))

        dao.clearEvents()
        dao.clearSamples()

        assertEquals(0L, dao.totalEventCount())
        assertEquals(0L, dao.totalSampleCount())
    }

    // 4. 7-day retention cleanup.
    @Test
    fun deleteEventsOlderThanCutoffRemovesOnlyStrictlyOlderRows() = runBlocking {
        val now = 10_000_000L
        val cutoff = DeveloperDiagnosticsRetention.cutoffEpochMs(now)

        dao.insertEvent(event(cutoff - 1, DiagnosticEventType.APP_SWITCH_BOUNDARY)) // strictly older -> deleted
        dao.insertEvent(event(cutoff, DiagnosticEventType.APP_SWITCH_BOUNDARY)) // exactly at cutoff -> retained
        dao.insertEvent(event(cutoff + 1, DiagnosticEventType.APP_SWITCH_BOUNDARY)) // newer -> retained

        val deleted = dao.deleteEventsOlderThan(cutoff)

        assertEquals(1, deleted)
        assertEquals(2L, dao.totalEventCount())
        val remainingTimestamps = dao.latestEvents(10).map { it.timestampEpochMs }.toSet()
        assertTrue(remainingTimestamps.contains(cutoff))
        assertTrue(remainingTimestamps.contains(cutoff + 1))
        assertTrue(!remainingTimestamps.contains(cutoff - 1))
    }

    @Test
    fun deleteSamplesOlderThanCutoffRemovesOnlyStrictlyOlderRows() = runBlocking {
        val now = 10_000_000L
        val cutoff = DeveloperDiagnosticsRetention.cutoffEpochMs(now)

        dao.insertSample(sample(cutoff - 100))
        dao.insertSample(sample(cutoff + 100))

        val deleted = dao.deleteSamplesOlderThan(cutoff)

        assertEquals(1, deleted)
        assertEquals(1L, dao.totalSampleCount())
    }

    @Test
    fun latestTimestampByTypeIsNullWhenNoEventOfThatTypeExists() = runBlocking {
        assertNull(dao.latestTimestampByType(DiagnosticEventType.WORKER_FAILURE.name))
    }
}
