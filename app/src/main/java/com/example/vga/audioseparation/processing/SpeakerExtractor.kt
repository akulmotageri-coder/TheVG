package com.example.vga.audioseparation.processing

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object SpeakerExtractor {

    private const val SAMPLE_RATE = 16000

    // Same settings as Python
    private const val WINDOW_SECONDS = 0.5
    private const val STEP_SECONDS = 0.25

    private const val THRESHOLD = 0.70

    private const val FADE_TIME_SECONDS = 0.15
    private const val SMOOTH_SECONDS = 0.20

    fun extractSpeaker(
        context: Context,
        callSamples: FloatArray,
        referenceEmbedding: FloatArray,
        outputFile: File
    ) {

        require(callSamples.isNotEmpty()) {
            "Call audio is empty"
        }

        require(referenceEmbedding.size == 256) {
            "Reference embedding must contain 256 values"
        }

        val windowSamples =
            (SAMPLE_RATE * WINDOW_SECONDS).toInt()

        val stepSamples =
            (SAMPLE_RATE * STEP_SECONDS).toInt()

        val fadeSamples =
            (SAMPLE_RATE * FADE_TIME_SECONDS).toInt()

        Log.d(
            "VGA_SEPARATION",
            "Window samples=$windowSamples"
        )

        Log.d(
            "VGA_SEPARATION",
            "Step samples=$stepSamples"
        )

        // --------------------------------
        // Voice confidence mask
        // --------------------------------

        val voiceMask =
            FloatArray(callSamples.size)

        val weightMask =
            FloatArray(callSamples.size)

        // --------------------------------
        // Speaker encoder
        // --------------------------------

        SpeakerEncoder(context).use { encoder ->

            var windowIndex = 0

            var start = 0

            while (
                start + windowSamples <=
                callSamples.size
            ) {

                val end =
                    start + windowSamples

                val segment =
                    callSamples.copyOfRange(
                        start,
                        end
                    )

                // --------------------------------
                // Compute mel spectrogram
                // --------------------------------

                val mel =
                    MelSpectrogram.compute(
                        segment
                    )

                /*
                 * The speaker encoder expects
                 * exactly 160 × 40.
                 *
                 * The 0.5 second audio window
                 * produces fewer than 160 frames,
                 * so we pad the mel frames.
                 */

                val encoderMel =
                    MelChunker.prepareForEncoder(
                        mel
                    )

                val embedding =
                    encoder.encode(
                        encoderMel
                    )

                // --------------------------------
                // Cosine similarity
                // --------------------------------

                val similarity =
                    cosineSimilarity(
                        referenceEmbedding,
                        embedding
                    )

                val timestamp =
                    start.toDouble() /
                            SAMPLE_RATE

                Log.d(
                    "VGA_SEPARATION",
                    String.format(
                        "%.2fs | similarity=%.4f",
                        timestamp,
                        similarity
                    )
                )

                // --------------------------------
                // Convert similarity to confidence
                // --------------------------------

                if (similarity >= THRESHOLD) {

                    val confidence =
                        min(
                            1.0,
                            (similarity - THRESHOLD) / 0.15
                        ).toFloat()

                    for (i in start until end) {

                        voiceMask[i] +=
                            confidence
                    }
                }

                for (i in start until end) {

                    weightMask[i] += 1.0f
                }

                windowIndex++

                start += stepSamples
            }

            Log.d(
                "VGA_SEPARATION",
                "Windows processed=$windowIndex"
            )
        }

        // --------------------------------
        // Normalize overlapping windows
        // --------------------------------

        for (i in voiceMask.indices) {

            if (weightMask[i] > 0f) {

                voiceMask[i] /=
                    weightMask[i]
            }
        }

        // --------------------------------
        // Smooth mask
        // --------------------------------

        smoothMask(
            voiceMask,
            SAMPLE_RATE,
            SMOOTH_SECONDS
        )

        // --------------------------------
        // Fade protection
        // --------------------------------

        applyFadeProtection(
            voiceMask,
            fadeSamples
        )

        // --------------------------------
        // Apply mask
        // --------------------------------

        val result =
            FloatArray(callSamples.size)

        for (i in callSamples.indices) {

            result[i] =
                callSamples[i] *
                        voiceMask[i]
        }

        // --------------------------------
        // Normalize output
        // --------------------------------

        var peak = 0f

        for (value in result) {

            peak =
                max(
                    peak,
                    kotlin.math.abs(value)
                )
        }

        if (peak > 0f) {

            val scale =
                0.95f / peak

            for (i in result.indices) {

                result[i] *= scale
            }
        }

        // --------------------------------
        // Save WAV
        // --------------------------------

        AudioPreprocessor.saveFloatWav(
            result,
            SAMPLE_RATE,
            outputFile
        )

        Log.d(
            "VGA_SEPARATION",
            "Speaker extraction completed"
        )

        Log.d(
            "VGA_SEPARATION",
            "Output: ${outputFile.absolutePath}"
        )
    }

    // --------------------------------
    // Cosine similarity
    // --------------------------------

    private fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray
    ): Float {

        require(a.size == b.size)

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {

            val x =
                a[i].toDouble()

            val y =
                b[i].toDouble()

            dot += x * y

            normA += x * x

            normB += y * y
        }

        if (
            normA == 0.0 ||
            normB == 0.0
        ) {
            return 0f
        }

        return (
                dot /
                        (
                                sqrt(normA) *
                                        sqrt(normB)
                                )
                ).toFloat()
    }

    // --------------------------------
    // Moving average smoothing
    // --------------------------------

    private fun smoothMask(
        mask: FloatArray,
        sampleRate: Int,
        seconds: Double
    ) {

        val window =
            max(
                1,
                (sampleRate * seconds).toInt()
            )

        val smoothed =
            FloatArray(mask.size)

        var sum = 0.0

        for (i in mask.indices) {

            sum += mask[i]

            if (i >= window) {

                sum -=
                    mask[i - window]
            }

            val count =
                min(
                    i + 1,
                    window
                )

            smoothed[i] =
                (
                        sum / count
                        ).toFloat()
        }

        for (i in mask.indices) {

            mask[i] =
                smoothed[i]
                    .coerceIn(0f, 1f)
        }
    }

    // --------------------------------
    // Fade protection
    // --------------------------------

    private fun applyFadeProtection(
        mask: FloatArray,
        fadeSamples: Int
    ) {

        if (fadeSamples <= 0) {
            return
        }

        val maxChange =
            1.0f / fadeSamples

        for (i in 1 until mask.size) {

            val difference =
                mask[i] - mask[i - 1]

            if (difference > maxChange) {

                mask[i] =
                    mask[i - 1] +
                            maxChange

            } else if (
                difference < -maxChange
            ) {

                mask[i] =
                    mask[i - 1] -
                            maxChange
            }
        }

        for (i in mask.indices) {

            mask[i] =
                mask[i]
                    .coerceIn(0f, 1f)
        }
    }
}