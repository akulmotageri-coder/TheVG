package com.example.vernacularguardian.keyboardprocessing.aggregation

/**
 * `riskContributionScore` per the PRD's exact weighted formula:
 *
 * ```
 * riskContributionScore =
 *     0.40 * normalize(avgBackspaceRate)
 *   + 0.35 * normalize(pauseFrequencyPer100Chars)
 *   + 0.25 * normalize(intervalStdDevMs)
 * ```
 *
 * The PRD leaves `normalize(...)`'s numeric bounds undefined. The three
 * saturation thresholds below are an explicitly authorized, minimal,
 * deterministic implementation decision (not PRD text) — see `Sprint_4.md`
 * Section 4.2. Kept as named constants so they can be retuned later without
 * touching the aggregation architecture.
 */
object RiskScoring {

    // Implementation-defined normalization bounds (PRD normalize() left undefined).
    private const val BACKSPACE_RATE_MAX = 1.0
    private const val PAUSE_FREQUENCY_SATURATION_PER_100_CHARS = 20.0
    private const val INTERVAL_STD_DEV_SATURATION_MS = 1000.0

    // PRD-exact weights.
    private const val BACKSPACE_WEIGHT = 0.40
    private const val PAUSE_WEIGHT = 0.35
    private const val INTERVAL_WEIGHT = 0.25

    private const val SCORE_MIN = 0.0
    private const val SCORE_MAX = 100.0

    fun normalizeBackspaceRate(rate: Double): Double =
        clamp(rate, 0.0, BACKSPACE_RATE_MAX) * 100.0

    fun normalizePauseFrequency(pauseFrequencyPer100Chars: Double): Double =
        clamp(pauseFrequencyPer100Chars / PAUSE_FREQUENCY_SATURATION_PER_100_CHARS, 0.0, 1.0) * 100.0

    fun normalizeIntervalStdDev(intervalStdDevMs: Double): Double =
        clamp(intervalStdDevMs / INTERVAL_STD_DEV_SATURATION_MS, 0.0, 1.0) * 100.0

    fun calculate(
        avgBackspaceRate: Double,
        pauseFrequencyPer100Chars: Double,
        intervalStdDevMs: Double
    ): Double {
        val score = BACKSPACE_WEIGHT * normalizeBackspaceRate(avgBackspaceRate) +
            PAUSE_WEIGHT * normalizePauseFrequency(pauseFrequencyPer100Chars) +
            INTERVAL_WEIGHT * normalizeIntervalStdDev(intervalStdDevMs)
        return clamp(score, SCORE_MIN, SCORE_MAX)
    }

    private fun clamp(value: Double, min: Double, max: Double): Double = value.coerceIn(min, max)
}
