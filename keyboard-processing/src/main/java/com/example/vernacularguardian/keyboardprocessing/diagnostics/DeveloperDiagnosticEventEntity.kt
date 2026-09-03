package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One compact, durable diagnostic event row (Sprint 8). Stored in
 * `developer_diagnostics.db` — never in `keyboard_processing.db` — so
 * engineering telemetry stays fully separate from product data.
 *
 * [eventType] is always a [DiagnosticEventType] name, never free text.
 * [valueLong]/[valueDouble]/[durationMs] are optional numeric payloads (e.g.
 * a duration or a row count); no field here ever holds typed text, a
 * package name, or any other sensitive content.
 *
 * Only written for the sparser boundary/lifecycle/error/aggregation event
 * types (see [DeveloperDiagnosticsRepository.record]) — never once per raw
 * accessibility event — to keep database writes low-frequency.
 */
@Entity(tableName = "diagnostic_events")
data class DeveloperDiagnosticEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampEpochMs: Long,
    val eventType: String,
    val valueLong: Long = 0L,
    val valueDouble: Double = 0.0,
    val durationMs: Long = 0L
)
