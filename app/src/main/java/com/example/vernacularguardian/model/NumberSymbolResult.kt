package com.example.vernacularguardian.model


// ============================================================
// NUMBER SYMBOL MATCH RESULT
// ============================================================

data class NumberSymbolResult(

    // Number of correct answers
    val correct: Int,

    // Number of incorrect answers
    val errors: Int,

    // Total questions attempted
    val totalTrials: Int,

    // Average time taken for each answer
    val averageResponseTimeMs: Long,

    // Total duration of the test
    val totalTimeMs: Long,

    // Whether the test ended because of timeout
    val timedOut: Boolean
)