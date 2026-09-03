package com.example.vga.audioseparation.processing

import kotlin.math.sqrt

object SpeakerMatcher {

    /**
     * Calculates cosine similarity between two speaker embeddings.
     *
     * Your SpeakerEncoder already returns normalized embeddings,
     * so this is effectively the same as the Python:
     *
     * np.dot(reference_embedding, embedding)
     */
    fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray
    ): Float {

        require(a.size == b.size) {
            "Embedding sizes do not match: ${a.size} vs ${b.size}"
        }

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {

            val av = a[i].toDouble()
            val bv = b[i].toDouble()

            dot += av * bv
            normA += av * av
            normB += bv * bv
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0f
        }

        return (
                dot /
                        (sqrt(normA) * sqrt(normB))
                ).toFloat()
    }

    /**
     * Converts similarity into confidence.
     *
     * Same basic logic as the Python implementation:
     *
     * threshold = 0.70
     * confidence reaches 1.0 around 0.85
     */
    fun similarityToConfidence(
        similarity: Float,
        threshold: Float = 0.70f
    ): Float {

        if (similarity < threshold) {
            return 0f
        }

        return (
                (similarity - threshold) / 0.15f
                ).coerceIn(0f, 1f)
    }
}