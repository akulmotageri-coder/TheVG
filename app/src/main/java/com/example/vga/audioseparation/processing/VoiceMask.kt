package com.example.vga.audioseparation.processing

import kotlin.math.max
import kotlin.math.min

object VoiceMask {

    private const val SAMPLE_RATE = 16000

    private const val WINDOW_SECONDS = 0.5
    private const val STEP_SECONDS = 0.25

    private const val SMOOTH_SECONDS = 0.20
    private const val FADE_SECONDS = 0.15

    private const val THRESHOLD = 0.70f

    /**
     * Creates a sample-level voice mask from
     * speaker similarity scores.
     *
     * similarities[i] corresponds to a window beginning
     * at:
     *
     * i * stepSamples
     */
    fun createMask(
        audioLength: Int,
        similarities: FloatArray
    ): FloatArray {

        val mask = FloatArray(audioLength)
        val weights = FloatArray(audioLength)

        val windowSamples =
            (SAMPLE_RATE * WINDOW_SECONDS).toInt()

        val stepSamples =
            (SAMPLE_RATE * STEP_SECONDS).toInt()

        for (index in similarities.indices) {

            val start =
                index * stepSamples

            if (start >= audioLength) {
                break
            }

            val end =
                min(
                    start + windowSamples,
                    audioLength
                )

            val confidence =
                SpeakerMatcher.similarityToConfidence(
                    similarities[index],
                    THRESHOLD
                )

            for (i in start until end) {

                mask[i] += confidence
                weights[i] += 1f
            }
        }

        // Normalize overlapping windows.
        for (i in mask.indices) {

            if (weights[i] > 0f) {
                mask[i] /= weights[i]
            }
        }

        // Smooth.
        val smoothSamples =
            max(
                1,
                (SAMPLE_RATE * SMOOTH_SECONDS).toInt()
            )

        smooth(mask, smoothSamples)

        // Clamp.
        for (i in mask.indices) {
            mask[i] =
                mask[i].coerceIn(0f, 1f)
        }

        // Fade protection.
        applyFadeProtection(mask)

        return mask
    }

    private fun smooth(
        mask: FloatArray,
        windowSize: Int
    ) {

        if (mask.isEmpty() || windowSize <= 1) {
            return
        }

        val result =
            FloatArray(mask.size)

        var sum = 0.0

        for (i in mask.indices) {

            sum += mask[i]

            if (i >= windowSize) {
                sum -= mask[i - windowSize]
            }

            val count =
                min(
                    i + 1,
                    windowSize
                )

            result[i] =
                (sum / count).toFloat()
        }

        System.arraycopy(
            result,
            0,
            mask,
            0,
            mask.size
        )
    }

    private fun applyFadeProtection(
        mask: FloatArray
    ) {

        if (mask.size < 2) {
            return
        }

        val fadeSamples =
            max(
                1,
                (SAMPLE_RATE * FADE_SECONDS).toInt()
            )

        val maxChange =
            1f / fadeSamples

        for (i in 1 until mask.size) {

            val difference =
                mask[i] - mask[i - 1]

            if (difference > maxChange) {

                mask[i] =
                    mask[i - 1] + maxChange

            } else if (difference < -maxChange) {

                mask[i] =
                    mask[i - 1] - maxChange
            }
        }
    }

    /**
     * Applies the voice mask to the original
     * 16 kHz normalized audio.
     */
    fun apply(
        audio: FloatArray,
        mask: FloatArray
    ): FloatArray {

        require(audio.size == mask.size) {
            "Audio and mask sizes differ: " +
                    "${audio.size} vs ${mask.size}"
        }

        val output =
            FloatArray(audio.size)

        for (i in audio.indices) {
            output[i] =
                audio[i] * mask[i]
        }

        return output
    }
}