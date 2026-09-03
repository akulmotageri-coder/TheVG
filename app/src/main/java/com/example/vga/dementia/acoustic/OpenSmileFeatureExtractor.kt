
package com.example.vga.dementia.acoustic

import android.content.Context
import android.util.Log
import com.example.vga.OpenSmileTest
import com.example.vga.audioseparation.processing.AudioData
import java.io.File
import kotlin.math.roundToInt

class OpenSmileFeatureExtractor(
    private val context: Context
) {

    companion object {
        private const val TAG = "OPENSMILE_EXTRACTOR"
        private const val TARGET_SAMPLE_RATE = 16000
    }

    /**
     * Extracts the 88 eGeMAPSv02 functionals from decoded mono audio.
     *
     * Input:
     *   AudioData containing mono Float PCM
     *
     * Output:
     *   FloatArray containing 88 eGeMAPSv02 features
     */
    fun extract(audio: AudioData): FloatArray? {

        Log.d(TAG, "Starting eGeMAPSv02 extraction")

        Log.d(
            TAG,
            "Input samples = ${audio.samples.size}, " +
                    "sampleRate = ${audio.sampleRate}, " +
                    "channels = ${audio.channels}"
        )

        if (audio.samples.isEmpty()) {
            Log.e(TAG, "Audio contains no samples")
            return null
        }

        /*
         * openSMILE configuration expects:
         *
         * 16000 Hz
         * mono
         * 16-bit PCM
         *
         * AudioDecoder already gives us mono Float PCM.
         *
         * If the original recording is not 16 kHz,
         * resample it before sending it to openSMILE.
         */
        val samples16k = if (
            audio.sampleRate == TARGET_SAMPLE_RATE
        ) {
            audio.samples
        } else {
            Log.d(
                TAG,
                "Resampling ${audio.sampleRate} Hz -> " +
                        "${TARGET_SAMPLE_RATE} Hz"
            )

            resampleLinear(
                audio.samples,
                audio.sampleRate,
                TARGET_SAMPLE_RATE
            )
        }

        Log.d(
            TAG,
            "Samples after resampling = ${samples16k.size}"
        )

        if (samples16k.isEmpty()) {
            Log.e(TAG, "Resampling produced no samples")
            return null
        }

        val openSmile = OpenSmileTest(context)

        return try {

            /*
             * Initialize openSMILE.
             */
            val initialized = openSmile.initialize()

            if (!initialized) {
                Log.e(
                    TAG,
                    "OpenSMILE initialization failed"
                )
                return null
            }

            /*
             * Start the OpenSMILE processing thread
             * before sending audio.
             */
            val started = openSmile.start()

            if (!started) {
                Log.e(
                    TAG,
                    "OpenSMILE failed to start"
                )
                return null
            }

            /*
             * Wait briefly for the processing thread
             * to enter the run loop.
             */
            Thread.sleep(100)

            /*
             * Convert Float PCM [-1, +1] into
             * signed 16-bit little-endian PCM.
             */
            val pcmBytes =
                FloatArrayToPcm16(samples16k)

            Log.d(
                TAG,
                "PCM byte count = ${pcmBytes.size}"
            )

            /*
             * Send audio in reasonably sized chunks.
             *
             * 3200 bytes =
             * 1600 samples =
             * 100 ms at 16 kHz.
             */
            val chunkSize = 1600
            var offset = 0

            while (offset < pcmBytes.size) {

                val end = minOf(
                    offset + chunkSize,
                    pcmBytes.size
                )

                val chunk = pcmBytes.copyOfRange(
                    offset,
                    end
                )

                val result =
                    openSmile.writePcm16(chunk)

                if (!result) {

                    Log.e(
                        TAG,
                        "Failed writing audio at offset $offset"
                    )

                    return null
                }

                offset = end
            }

            Log.d(
                TAG,
                "Finished sending audio"
            )

            /*
             * Tell externalAudioSource that the audio
             * has ended.
             *
             * This is what causes the functionals pipeline
             * to flush and the externalSink callback
             * to receive the final 88-dimensional vector.
             */
            val eoiResult =
                openSmile.endOfInput()

            Log.d(
                TAG,
                "EOI result = $eoiResult"
            )

            /*
             * Wait for OpenSMILE to finish processing.
             *
             * The callback stores the resulting vector
             * inside OpenSmileTest.
             */
            val features =
                openSmile.waitForFeatures(5000)

            if (features == null) {

                Log.e(
                    TAG,
                    "No feature vector received"
                )

                return null
            }

            Log.d(
                TAG,
                "Feature extraction complete"
            )

            Log.d(
                TAG,
                "Feature count = ${features.size}"
            )

            Log.d(
                TAG,
                "Features = ${features.contentToString()}"
            )

            if (features.size != 88) {

                Log.e(
                    TAG,
                    "Unexpected feature count: ${features.size}"
                )

                return null
            }

            features

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Feature extraction failed",
                e
            )

            null

        } finally {

            openSmile.close()
        }
    }

    /**
     * Converts Float PCM [-1, +1] into signed
     * 16-bit little-endian PCM.
     */
    private fun FloatArrayToPcm16(
        samples: FloatArray
    ): ByteArray {

        val bytes = ByteArray(
            samples.size * 2
        )

        for (i in samples.indices) {

            val sample =
                samples[i]
                    .coerceIn(-1f, 1f)

            val value =
                (sample * 32767f)
                    .roundToInt()
                    .coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    )

            bytes[i * 2] =
                (value and 0xFF).toByte()

            bytes[i * 2 + 1] =
                ((value shr 8) and 0xFF).toByte()
        }

        return bytes
    }

    /**
     * Simple linear interpolation resampler.
     *
     * Suitable for the speech-analysis pipeline here.
     */
    private fun resampleLinear(
        input: FloatArray,
        inputRate: Int,
        outputRate: Int
    ): FloatArray {

        if (input.isEmpty()) {
            return FloatArray(0)
        }

        if (inputRate == outputRate) {
            return input
        }

        val outputLength =
            (input.size.toLong() *
                    outputRate /
                    inputRate)
                .toInt()

        if (outputLength <= 0) {
            return FloatArray(0)
        }

        val output =
            FloatArray(outputLength)

        val ratio =
            inputRate.toDouble() /
                    outputRate.toDouble()

        for (i in output.indices) {

            val sourcePosition =
                i * ratio

            val left =
                sourcePosition.toInt()

            val right =
                minOf(
                    left + 1,
                    input.lastIndex
                )

            val fraction =
                sourcePosition - left

            output[i] =
                (
                        input[left] *
                                (1.0 - fraction) +
                                input[right] *
                                fraction
                        ).toFloat()
        }

        return output
    }
}
