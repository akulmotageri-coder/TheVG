package com.example.vernacularguardian.keyboardprocessing.storage

import android.os.SystemClock
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * Exercises [KeyboardProcessingDatabase.MIGRATION_2_3]'s actual logic
 * directly against a real, on-device SQLite database (a fresh in-memory
 * Room database's own [androidx.sqlite.db.SupportSQLiteDatabase]), rather
 * than via Room's schema-export-JSON-based `MigrationTestHelper` — this
 * project keeps `exportSchema = false` (an unchanged Sprint 3 decision), so
 * that tool isn't available; calling the migration function directly against
 * a real database exercises the identical code path Room would invoke
 * during a real 2->3 version transition.
 */
@RunWith(AndroidJUnit4::class)
class KeyboardProcessingDatabaseMigrationTest {

    private fun newDatabase(): KeyboardProcessingDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, KeyboardProcessingDatabase::class.java).build()
    }

    @Test
    fun migration2To3BackfillsValidLegacyRowAndLeavesImpossibleTimestampNull() = runBlocking {
        val database = newDatabase()
        val supportDb = database.openHelper.writableDatabase

        val currentElapsedRealtime = SystemClock.elapsedRealtime()
        val validStartTimeMs = currentElapsedRealtime / 2 // plausibly from this boot
        val impossibleStartTimeMs = currentElapsedRealtime + 999_999_999L // cannot be from this boot

        fun insertLegacyRow(startTimeMs: Long, typingSpeedCpm: Double) {
            supportDb.execSQL(
                """
                INSERT INTO typing_sessions
                    (startTimeMs, endTimeMs, typingSpeedCpm, backspaceRate, intervalStdDevMs, microPauseCount, charCount, sessionEpochDay)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
                """.trimIndent(),
                arrayOf(startTimeMs, startTimeMs + 1000, typingSpeedCpm, 0.1, 100.0, 1, 100)
            )
        }

        insertLegacyRow(validStartTimeMs, 55.5)
        insertLegacyRow(impossibleStartTimeMs, 66.6)

        KeyboardProcessingDatabase.MIGRATION_2_3.migrate(supportDb)

        val rows = database.typingSessionDao().getAllSessions()
        assertEquals(2, rows.size)

        val validRow = rows.single { it.typingSpeedCpm == 55.5 }
        val impossibleRow = rows.single { it.typingSpeedCpm == 66.6 }

        // Valid row: backfilled to today's real calendar day (this test's whole
        // elapsedRealtime/wall-clock read happens within the same boot/moment).
        assertEquals(LocalDate.now(ZoneId.systemDefault()).toEpochDay(), validRow.sessionEpochDay)

        // Impossible row (startTimeMs > current elapsedRealtime => not from this
        // boot): left NULL, no date fabricated.
        assertNull(impossibleRow.sessionEpochDay)

        // Original metrics preserved exactly, for both rows.
        assertEquals(0.1, validRow.backspaceRate, 1e-9)
        assertEquals(100.0, validRow.intervalStdDevMs, 1e-9)
        assertEquals(1, validRow.microPauseCount)
        assertEquals(100, validRow.charCount)
        assertEquals(0.1, impossibleRow.backspaceRate, 1e-9)

        database.close()
    }

    @Test
    fun migration2To3NeverOverwritesARowThatAlreadyHasASessionEpochDay() = runBlocking {
        val database = newDatabase()
        val supportDb = database.openHelper.writableDatabase
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

        supportDb.execSQL(
            """
            INSERT INTO typing_sessions
                (startTimeMs, endTimeMs, typingSpeedCpm, backspaceRate, intervalStdDevMs, microPauseCount, charCount, sessionEpochDay)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(0L, 1000L, 42.0, 0.0, 0.0, 0, 10, today)
        )

        KeyboardProcessingDatabase.MIGRATION_2_3.migrate(supportDb)

        val rows = database.typingSessionDao().getAllSessions()
        assertEquals(1, rows.size)
        assertEquals(today, rows.single().sessionEpochDay)

        database.close()
    }

    @Test
    fun migration2To3IsSafeToRunWhenThereAreNoNullRowsAtAll() = runBlocking {
        val database = newDatabase()
        val supportDb = database.openHelper.writableDatabase

        // No rows at all - the migration's SELECT/UPDATE pass must be a clean no-op.
        KeyboardProcessingDatabase.MIGRATION_2_3.migrate(supportDb)

        assertEquals(0, database.typingSessionDao().getAllSessions().size)
        database.close()
    }
}