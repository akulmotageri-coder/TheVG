

// ============================================================
// COMPLETE ASSESSMENT RESULT
// ============================================================

package com.example.vernacularguardian.model


// ============================================================
// COMPLETE ASSESSMENT RESULT
// ============================================================

data class AssessmentResult(

    // ========================================================
    // STROOP
    // ========================================================

    val stroop: StroopResult? = null,


    // ========================================================
    // DIGIT SPAN
    // ========================================================

    val digitSpan: DigitSpanResult? = null,


    // ========================================================
    // TRAIL MAKING
    // ========================================================

    val trailMaking: TrailMakingResult? = null,


    // ========================================================
    // NUMBER SYMBOL MATCH
    // ========================================================

    val numberSymbol: NumberSymbolResult? = null
)


// ============================================================
// STROOP RESULT
// ============================================================

data class StroopResult(

    val correct: Int,

    val errors: Int,

    val timeouts: Int,

    val totalTrials: Int,

    val averageResponseTimeMs: Long
)


// ============================================================
// DIGIT SPAN RESULT
// ============================================================

data class DigitSpanResult(

    val forwardLongestSpan: Int,

    val backwardLongestSpan: Int,

    val forwardCorrect: Int,

    val backwardCorrect: Int
)


// ============================================================
// TRAIL MAKING RESULT
// ============================================================

data class TrailMakingResult(

    val partATimeMs: Long,

    val partBTimeMs: Long,

    val partAErrors: Int,

    val partBErrors: Int
)