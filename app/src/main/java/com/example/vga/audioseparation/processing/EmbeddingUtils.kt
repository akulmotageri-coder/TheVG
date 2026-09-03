package com.example.vga.audioseparation.processing

import kotlin.math.sqrt

object EmbeddingUtils {

    /**
     * Combines multiple normalized speaker embeddings
     * into one normalized call-level embedding.
     *
     * Input:
     *   N × 256
     *
     * Output:
     *   256
     */
    fun meanAndNormalize(
        embeddings: List<FloatArray>
    ): FloatArray {

        require(embeddings.isNotEmpty()) {
            "Embeddings list cannot be empty"
        }

        val dimension =
            embeddings[0].size

        require(dimension > 0) {
            "Embedding dimension cannot be zero"
        }

        for (embedding in embeddings) {
            require(embedding.size == dimension) {
                "All embeddings must have the same dimension"
            }
        }

        // --------------------------------
        // Mean pooling
        // --------------------------------

        val mean =
            FloatArray(dimension)

        for (embedding in embeddings) {

            for (i in 0 until dimension) {

                mean[i] +=
                    embedding[i]
            }
        }

        val count =
            embeddings.size.toFloat()

        for (i in 0 until dimension) {

            mean[i] /= count
        }

        // --------------------------------
        // L2 normalization
        // --------------------------------

        var normSquared = 0.0

        for (value in mean) {

            normSquared +=
                value.toDouble() *
                        value.toDouble()
        }

        val norm =
            sqrt(normSquared)

        require(norm > 0.0) {
            "Cannot normalize zero embedding"
        }

        for (i in mean.indices) {

            mean[i] =
                (mean[i] / norm).toFloat()
        }

        return mean
    }

    fun calculateNorm(
        embedding: FloatArray
    ): Double {

        var sum = 0.0

        for (value in embedding) {

            sum +=
                value.toDouble() *
                        value.toDouble()
        }

        return sqrt(sum)
    }
}