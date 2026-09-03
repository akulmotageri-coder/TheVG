package com.example.vga.audioseparation.processing

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object AudioMaskProcessor {

    private const val SAMPLE_RATE = 16000

    /*
     * Must match the mel pipeline.
     */
    private const val HOP_LENGTH = 160

    /*
     * 160 mel frames × 160 samples
     *
     * ≈ 1.6 seconds.
     */
    private const val CHUNK_FRAMES = 160

    private const val CHUNK_SAMPLES =
        CHUNK_FRAMES * HOP_LENGTH

    /**
     * Creates a sample-level speaker confidence mask.
     *
     * Each embedding chunk receives one confidence.
     *
     * Important:
     * Low-confidence chunks are strongly suppressed.
     */
    fun createMask(
        audioLength: Int,
        confidences: List<Float>
    ): FloatArray {

        require(audioLength > 0) {
            "Audio length must be greater than zero"
        }

        val mask =
            FloatArray(audioLength)

        val weight =
            FloatArray(audioLength)

        for (chunkIndex in confidences.indices) {

            val start =
                chunkIndex * CHUNK_SAMPLES

            if (start >= audioLength) {
                break
            }

            val end =
                min(
                    start + CHUNK_SAMPLES,
                    audioLength
                )

            val confidence =
                confidences[chunkIndex]
                    .coerceIn(0f, 1f)

            /*
             * Extra suppression.
             *
             * A confidence of:
             *
             * 0.3 → 0.09
             * 0.5 → 0.25
             * 0.7 → 0.49
             * 1.0 → 1.0
             *
             * This makes uncertain regions quieter.
             */
            val strengthened =
                confidence.pow(1.5f)

            for (i in start until end) {

                mask[i] +=
                    strengthened

                weight[i] += 1f
            }
        }

        /*
         * Normalize overlapping regions.
         */
        for (i in mask.indices) {

            if (weight[i] > 0f) {

                mask[i] =
                    (
                            mask[i] /
                                    weight[i]
                            )
                        .coerceIn(0f, 1f)

            } else {

                mask[i] = 0f
            }
        }

        return mask
    }

    /**
     * Smooth mask using a centered-style
     * moving average.
     *
     * Keeps transitions natural without
     * immediately opening weak regions.
     */
    fun smoothMask(
        mask: FloatArray,
        durationMs: Int = 120
    ): FloatArray {

        if (mask.isEmpty()) {
            return mask
        }

        val windowSamples =
            max(
                1,
                SAMPLE_RATE *
                        durationMs /
                        1000
            )

        val result =
            FloatArray(mask.size)

        var sum = 0.0

        for (i in mask.indices) {

            sum +=
                mask[i]

            if (i >= windowSamples) {

                sum -=
                    mask[
                        i - windowSamples
                    ]
            }

            val count =
                min(
                    i + 1,
                    windowSamples
                )

            result[i] =
                (
                        sum / count
                        )
                    .toFloat()
                    .coerceIn(0f, 1f)
        }

        return result
    }

    /**
     * Prevents abrupt gain changes.
     */
    fun applyFadeProtection(
        mask: FloatArray,
        fadeMs: Int = 100
    ): FloatArray {

        if (mask.isEmpty()) {
            return mask
        }

        val result =
            mask.copyOf()

        val fadeSamples =
            max(
                1,
                SAMPLE_RATE *
                        fadeMs /
                        1000
            )

        val maxChange =
            1f /
                    fadeSamples
                        .toFloat()

        for (i in 1 until result.size) {

            val difference =
                result[i] -
                        result[i - 1]

            when {

                difference > maxChange -> {

                    result[i] =
                        result[i - 1] +
                                maxChange
                }

                difference < -maxChange -> {

                    result[i] =
                        result[i - 1] -
                                maxChange
                }
            }

            result[i] =
                result[i]
                    .coerceIn(0f, 1f)
        }

        return result
    }

    /**
     * Applies the speaker mask to the audio.
     */
    fun applyMask(
        samples: FloatArray,
        mask: FloatArray
    ): FloatArray {

        require(
            samples.size == mask.size
        ) {
            "Audio and mask sizes must match"
        }

        val output =
            FloatArray(samples.size)

        for (i in samples.indices) {

            output[i] =
                samples[i] *
                        mask[i]
        }

        return output
    }

    /**
     * Safely normalizes output to 95% peak.
     */
    fun normalizeOutput(
        samples: FloatArray
    ): FloatArray {

        if (samples.isEmpty()) {
            return samples
        }

        var peak = 0f

        for (sample in samples) {

            peak =
                max(
                    peak,
                    abs(sample)
                )
        }

        if (peak <= 0f) {
            return samples.copyOf()
        }

        val scale =
            0.95f / peak

        val output =
            FloatArray(samples.size)

        for (i in samples.indices) {

            output[i] =
                (
                        samples[i] *
                                scale
                        )
                    .coerceIn(
                        -1f,
                        1f
                    )
        }

        return output
    }
}