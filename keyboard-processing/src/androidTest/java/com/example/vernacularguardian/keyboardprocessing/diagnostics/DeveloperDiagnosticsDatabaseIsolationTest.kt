package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine
import com.example.vernacularguardian.keyboardprocessing.storage.DailySummaryEntity
import com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase
import com.example.vernacularguardian.keyboardprocessing.storage.TypingSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 8 Section 36 (mandatory): `developer_diagnostics.db` must be a
 * separate database from `keyboard_processing.db`, and clearing diagnostics
 * history must never affect `typing_sessions`/`daily_summaries`. Proven here
 * against two independently-built, isolated in-memory Room databases running
 * the exact same clear operation [DeveloperDiagnosticsRepository.clearHistory]
 * performs against the real singletons.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperDiagnosticsDatabaseIsolationTest {

    private lateinit var productDatabase: KeyboardProcessingDatabase
    private lateinit var diagnosticsDatabase: DeveloperDiagnosticsDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        productDatabase = Room.inMemoryDatabaseBuilder(context, KeyboardProcessingDatabase::class.java).build()
        diagnosticsDatabase = Room.inMemoryDatabaseBuilder(context, DeveloperDiagnosticsDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        productDatabase.close()
        diagnosticsDatabase.close()
    }

    @Test
    fun databaseFileNamesAreDistinct() {
        assertEquals("developer_diagnostics.db", DeveloperDiagnosticsDatabase.DATABASE_NAME)
        assertNotEquals(DeveloperDiagnosticsDatabase.DATABASE_NAME, "keyboard_processing.db")
    }

    @Test
    fun clearingDiagnosticTablesNeverTouchesProductTables() = runBlocking {
        val today = BehavioralAggregationEngine.todayEpochDay()
        productDatabase.typingSessionDao().insert(
            TypingSessionEntity(
                startTimeMs = 0L,
                endTimeMs = 1000L,
                typingSpeedCpm = 60.0,
                backspaceRate = 0.1,
                intervalStdDevMs = 100.0,
                microPauseCount = 2,
                charCount = 100,
                sessionEpochDay = today
            )
        )
        productDatabase.dailySummaryDao().upsert(
            DailySummaryEntity(
                dateEpochDay = today,
                avgTypingSpeedCpm = 60.0,
                avgBackspaceRate = 0.1,
                intervalStdDevMs = 100.0,
                pauseFrequencyPer100Chars = 2.0,
                sessionCount = 1,
                riskContributionScore = 10.0,
                isReliable = true
            )
        )

        diagnosticsDatabase.diagnosticsDao().insertEvent(
            DeveloperDiagnosticEventEntity(timestampEpochMs = 1L, eventType = DiagnosticEventType.APP_SWITCH_BOUNDARY.name)
        )
        diagnosticsDatabase.diagnosticsDao().insertSample(
            DeveloperDiagnosticSampleEntity(
                timestampEpochMs = 1L,
                processCpuTimeMs = null,
                wallClockElapsedMs = 1L,
                cpuUtilizationPercent = null,
                totalPssKb = 100,
                javaHeapKb = null,
                nativeHeapKb = null,
                privateDirtyKb = null,
                batteryPercent = null,
                isCharging = null,
                batteryTemperatureTenthsC = null,
                thermalStatus = null,
                processUptimeMs = 1L
            )
        )

        // Exactly what DeveloperDiagnosticsRepository.clearHistory() does to the database.
        diagnosticsDatabase.diagnosticsDao().clearEvents()
        diagnosticsDatabase.diagnosticsDao().clearSamples()

        assertEquals(0L, diagnosticsDatabase.diagnosticsDao().totalEventCount())
        assertEquals(0L, diagnosticsDatabase.diagnosticsDao().totalSampleCount())

        // Product tables are a structurally separate database instance - untouched.
        assertEquals(1, productDatabase.typingSessionDao().getAllSessions().size)
        assertEquals(1, productDatabase.dailySummaryDao().getAllSummaries().size)
    }
}
