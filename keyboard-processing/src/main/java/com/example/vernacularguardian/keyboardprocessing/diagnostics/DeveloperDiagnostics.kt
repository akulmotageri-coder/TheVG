package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.content.Context

/**
 * Thin, single entry point into the Sprint 8 developer-diagnostics layer
 * for `:app`'s [DeveloperDiagnosticsScreen] — mirrors the
 * `KeyboardProcessingModule.getInstance(context)` convention used by the
 * product [com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingApi].
 * Entirely separate from that public API: nothing here is reachable
 * through it, and nothing in it can change product behavior.
 */
object DeveloperDiagnostics {
    fun repository(context: Context): DeveloperDiagnosticsRepository =
        DeveloperDiagnosticsRepository.getInstance(context)
}
