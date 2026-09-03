package com.example.vernacularguardian.keyboardprocessing.diagnostics

/**
 * Pure, in-memory, process-wide mirror of the most recent
 * [com.example.vernacularguardian.keyboardprocessing.aggregation.BehavioralAggregationEngine.runOnce]
 * run, observed via purely-additive hooks in that class. No Android
 * dependency, so it is deterministically unit testable; state resets on
 * process restart (durable history for aggregation events instead lives in
 * `developer_diagnostics.db` via [DeveloperDiagnosticsRepository]).
 */
object DeveloperDiagnosticsAggregationState {

    @Volatile
    var lastStartEpochMs: Long? = null
        private set

    @Volatile
    var lastSuccessEpochMs: Long? = null
        private set

    @Volatile
    var lastFailureEpochMs: Long? = null
        private set

    @Volatile
    var lastDurationMs: Long? = null
        private set

    @Volatile
    var lastRetentionDeletedRows: Int? = null
        private set

    fun recordStart(epochMs: Long) {
        lastStartEpochMs = epochMs
    }

    fun recordSuccess(epochMs: Long, durationMs: Long) {
        lastSuccessEpochMs = epochMs
        lastDurationMs = durationMs
    }

    fun recordFailure(epochMs: Long, durationMs: Long) {
        lastFailureEpochMs = epochMs
        lastDurationMs = durationMs
    }

    fun recordRetentionDeletion(deletedRows: Int) {
        lastRetentionDeletedRows = deletedRows
    }

    /** Test/clear-diagnostics-history support only; never called from product code paths. */
    fun reset() {
        lastStartEpochMs = null
        lastSuccessEpochMs = null
        lastFailureEpochMs = null
        lastDurationMs = null
        lastRetentionDeletedRows = null
    }
}
