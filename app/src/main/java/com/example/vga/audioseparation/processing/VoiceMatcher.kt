package com.example.vga.audioseparation.processing

import kotlin.math.pow
import kotlin.math.sqrt

object VoiceMatcher {

    /**
     * Calculates cosine similarity between two embeddings.
     *
     * Returns approximately -1.0 to +1.0.
     */
    fun cosineSimilarity(
        reference: FloatArray,
        candidate: FloatArray
    ): Float {

        require(reference.size == candidate.size) {
            "Embedding size mismatch: " +
                    "${reference.size} vs ${candidate.size}"
        }

        var dotProduct = 0.0
        var referenceNorm = 0.0
        var candidateNorm = 0.0

        for (i in reference.indices) {

            val a = reference[i].toDouble()
            val b = candidate[i].toDouble()

            dotProduct += a * b
            referenceNorm += a * a
            candidateNorm += b * b
        }

        val denominator =
            sqrt(referenceNorm) *
                    sqrt(candidateNorm)

        if (denominator <= 0.0) {
            return 0f
        }

        return (
                dotProduct / denominator
                ).toFloat()
            .coerceIn(-1f, 1f)
    }

    /**
     * Converts speaker similarity into a confidence value.
     *
     * The mapping is intentionally conservative.
     *
     * Below 0.56:
     *     reject the chunk.
     *
     * 0.56 - 0.64:
     *     gradually increase confidence.
     *
     * 0.64+:
     *     strong match.
     *
     * The nonlinear curve makes weak matches much
     * less likely to leak into the extracted audio.
     */
    fun similarityToConfidence(
        similarity: Float,
        threshold: Float = 0.56f,
        range: Float = 0.08f
    ): Float {

        if (similarity <= threshold) {
            return 0f
        }

        val normalized =
            (
                    (similarity - threshold) / range
                    )
                .coerceIn(0f, 1f)

        /*
         * Nonlinear mapping.
         *
         * Weak matches stay suppressed.
         * Strong matches rise rapidly.
         */
        return normalized.pow(2.0f)
            .coerceIn(0f, 1f)
    }

    /**
     * Optional stricter decision function.
     *
     * Useful when we want to know whether a chunk
     * is confidently the reference speaker.
     */
    fun isReferenceSpeaker(
        similarity: Float,
        threshold: Float = 0.60f
    ): Boolean {

        return similarity >= threshold
    }
}
