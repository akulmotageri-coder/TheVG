package com.example.vernacularguardian.keyboardprocessing.storage

import android.content.Context
import android.os.SystemClock
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId

@Database(
    entities = [TypingSessionEntity::class, DailySummaryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class KeyboardProcessingDatabase : RoomDatabase() {

    abstract fun typingSessionDao(): TypingSessionDao
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {
        private const val DATABASE_NAME = "keyboard_processing.db"

        /**
         * Sprint 4: adds the nullable `sessionEpochDay` column to
         * `typing_sessions`. No value is backfilled for existing Sprint 3
         * rows (they stay NULL) — their elapsedRealtime timestamps cannot be
         * converted to a calendar day, so no date is invented for them.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE typing_sessions ADD COLUMN sessionEpochDay INTEGER")
            }
        }

        /**
         * Sprint 4 hardening: one-time backfill for legacy rows still NULL
         * after [MIGRATION_1_2]. A row's `startTimeMs` is only convertible to
         * a calendar day if it is an elapsedRealtime value from *this same
         * boot* — that holds only when `0 <= startTimeMs <= (current
         * elapsedRealtime, read once here)`. For rows that satisfy this, the
         * current boot's wall-clock anchor
         * (`System.currentTimeMillis() - SystemClock.elapsedRealtime()`) is
         * added to `startTimeMs` to recover the session's real wall-clock
         * instant, then converted to a device-local calendar day. Rows that
         * fail the check are left NULL — no date is fabricated for them, and
         * they are never deleted. Runs exactly once, as part of the 2->3
         * schema migration, never on ordinary app/database access.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val currentElapsedRealtime = SystemClock.elapsedRealtime()
                val bootWallClockEpochMs = System.currentTimeMillis() - currentElapsedRealtime

                val idsToEpochDay = mutableListOf<Pair<Long, Long>>()
                db.query("SELECT id, startTimeMs FROM typing_sessions WHERE sessionEpochDay IS NULL").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val startTimeMsIndex = cursor.getColumnIndexOrThrow("startTimeMs")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val startTimeMs = cursor.getLong(startTimeMsIndex)

                        // Only valid if startTimeMs is a plausible elapsedRealtime
                        // value from the current boot (i.e. not from a boot before
                        // this one, which a reset elapsedRealtime clock cannot rule
                        // out in general, but a value greater than "now" proves).
                        if (startTimeMs in 0..currentElapsedRealtime) {
                            val sessionWallClockEpochMs = bootWallClockEpochMs + startTimeMs
                            val epochDay = Instant.ofEpochMilli(sessionWallClockEpochMs)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toEpochDay()
                            idsToEpochDay.add(id to epochDay)
                        }
                        // else: leave NULL - do not fabricate a date.
                    }
                }

                for ((id, epochDay) in idsToEpochDay) {
                    db.execSQL(
                        "UPDATE typing_sessions SET sessionEpochDay = ? WHERE id = ?",
                        arrayOf(epochDay, id)
                    )
                }
            }
        }

        @Volatile
        private var instance: KeyboardProcessingDatabase? = null

        fun getInstance(context: Context): KeyboardProcessingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KeyboardProcessingDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
    }
}