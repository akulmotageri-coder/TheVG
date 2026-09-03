package com.example.vernacularguardian.keyboardprocessing.storage

import android.content.Context
import android.util.Log
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DeveloperDiagnosticCounters
import com.example.vernacularguardian.keyboardprocessing.diagnostics.DiagnosticEventType
import com.example.vernacularguardian.keyboardprocessing.tracker.TypingSessionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Persists completed typing sessions to Room. Owns its own coroutine scope
 * (tied to the Room database singleton's process lifetime, not to any one
 * caller's lifecycle) so a persist triggered from service teardown is not
 * cancelled by that same teardown.
 */
class KeyboardProcessingStorage(context: Context) {

    private val dao = KeyboardProcessingDatabase.getInstance(context).typingSessionDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun persistCompletedSession(result: TypingSessionResult) {
        scope.launch {
            try {
                dao.insert(result.toEntity())
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.SESSION_PERSISTED)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist completed session: ${e.javaClass.simpleName}: ${e.message}")
                DeveloperDiagnosticCounters.increment(DiagnosticEventType.SESSION_PERSIST_FAILED)
            }
        }
    }

    private companion object {
        const val TAG = "KeyboardProcessingStorage"
    }
}

private fun TypingSessionResult.toEntity() = TypingSessionEntity(
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    typingSpeedCpm = typingSpeedCpm,
    backspaceRate = backspaceRate,
    intervalStdDevMs = intervalStdDevMs,
    microPauseCount = microPauseCount,
    charCount = charCount,
    // Sprint 4: captured once, now, at persist time — startTimeMs/endTimeMs
    // remain elapsedRealtime and are never reinterpreted as calendar time.
    sessionEpochDay = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
)
