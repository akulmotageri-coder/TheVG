package com.example.vga.acoustic

data class AcousticResult(
    val score: Int,
    val level: String,
    val pitchVariation: String,
    val voiceStability: String,
    val harmonicity: String
)

object AcousticRiskScorer {

    fun analyze(features: List<Float>): AcousticResult {

        require(features.size >= 88) {
            "Expected at least 88 eGeMAPSv02 features"
        }

        // eGeMAPSv02 indexes
        val f0Mean = features[0]
        val f0Std = features[1]
        val jitter = features[30]
        val shimmer = features[32]
        val hnr = features[34]

        var score = 0

        // Pitch variability
        if (f0Std > 0.25f) score += 20
        else if (f0Std > 0.15f) score += 10

        // Jitter
        if (jitter > 0.03f) score += 20
        else if (jitter > 0.015f) score += 10

        // Shimmer
        if (shimmer > 1.5f) score += 20
        else if (shimmer > 0.8f) score += 10

        // HNR
        if (hnr < 5f) score += 20
        else if (hnr < 10f) score += 10

        // F0 sanity / pitch indicator
        if (f0Mean < 20f || f0Mean > 35f) {
            score += 20
        }

        score = score.coerceIn(0, 100)

        val level = when {
            score < 30 -> "Low"
            score < 60 -> "Moderate"
            else -> "High"
        }

        val pitchVariation = when {
            f0Std < 0.15f -> "Low"
            f0Std < 0.25f -> "Moderate"
            else -> "High"
        }

        val voiceStability = when {
            jitter < 0.015f && shimmer < 0.8f -> "Good"
            jitter < 0.03f && shimmer < 1.5f -> "Moderate"
            else -> "Reduced"
        }

        val harmonicity = when {
            hnr >= 10f -> "Good"
            hnr >= 5f -> "Moderate"
            else -> "Reduced"
        }

        return AcousticResult(
            score = score,
            level = level,
            pitchVariation = pitchVariation,
            voiceStability = voiceStability,
            harmonicity = harmonicity
        )
    }
}
