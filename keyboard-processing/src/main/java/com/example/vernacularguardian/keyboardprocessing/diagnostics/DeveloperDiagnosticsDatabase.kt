package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Separate Room database for Sprint 8 developer diagnostics, deliberately
 * distinct from [com.example.vernacularguardian.keyboardprocessing.storage.KeyboardProcessingDatabase]
 * (`keyboard_processing.db`, the product database). This keeps engineering
 * telemetry (event counters, resource samples) fully isolated from product
 * data: clearing this database can never touch `typing_sessions` or
 * `daily_summaries`, and a schema change here never requires a product
 * migration.
 */
@Database(
    entities = [DeveloperDiagnosticEventEntity::class, DeveloperDiagnosticSampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DeveloperDiagnosticsDatabase : RoomDatabase() {

    abstract fun diagnosticsDao(): DeveloperDiagnosticsDao

    companion object {
        const val DATABASE_NAME = "developer_diagnostics.db"

        @Volatile
        private var instance: DeveloperDiagnosticsDatabase? = null

        fun getInstance(context: Context): DeveloperDiagnosticsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeveloperDiagnosticsDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
    }
}
