package com.example.vga.audioseparation.processing

import android.content.Context
import android.util.Log
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln

object MelSpectrogram {

    private const val SAMPLE_RATE = 16000
    private const val N_FFT = 400
    private const val HOP_LENGTH = 160
    private const val N_MELS = 40

    fun logSummary(
        mel: Array<FloatArray>,
        context: Context? = null
    ) {

        if (mel.isEmpty()) {
            println("MEL_TEST: empty")
            return
        }

        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var sum = 0.0

        for (frame in mel) {
            for (value in frame) {

                if (value < min) min = value
                if (value > max) max = value

                sum += value
            }
        }

        val count =
            mel.size * N_MELS

        println("MEL_TEST: shape=(${mel.size}, $N_MELS)")
        println("MEL_TEST: mean=${sum / count}")
        println("MEL_TEST: min=$min")
        println("MEL_TEST: max=$max")

        print("MEL_TEST: first10=")

        for (i in 0 until minOf(10, N_MELS)) {
            print("${mel[0][i]} ")
        }

        println()

        // --------------------------------
        // Save complete Android mel
        // --------------------------------

        if (context != null) {

            try {

                val file =
                    File(
                        context.filesDir,
                        "android_mel.npy"
                    )

                saveAsNpy(
                    mel,
                    file
                )

                Log.d(
                    "VGA_PROCESSING",
                    "Android mel saved: ${file.absolutePath}"
                )

            } catch (e: Exception) {

                Log.e(
                    "VGA_PROCESSING",
                    "Failed to save Android mel",
                    e
                )
            }
        }
    }

    /**
     * Save FloatArray matrix as a NumPy-compatible .npy file.
     *
     * Shape:
     * [frames, 40]
     *
     * dtype:
     * float32
     */
    private fun saveAsNpy(
        mel: Array<FloatArray>,
        file: File
    ) {

        val rows = mel.size
        val cols = N_MELS

        FileOutputStream(file).use { fos ->

            // NumPy .npy magic
            fos.write(
                byteArrayOf(
                    0x93.toByte(),
                    'N'.code.toByte(),
                    'U'.code.toByte(),
                    'M'.code.toByte(),
                    'P'.code.toByte(),
                    'Y'.code.toByte()
                )
            )

            // Version 1.0
            fos.write(1)
            fos.write(0)

            val header =
                "{'descr': '<f4', 'fortran_order': False, 'shape': ($rows, $cols), }"

            val headerBytes =
                header.toByteArray(Charsets.US_ASCII)

            val padding =
                16 -
                        ((10 + headerBytes.size + 1) % 16)

            val finalHeader =
                headerBytes +
                        ByteArray(padding) { ' '.code.toByte() } +
                        byteArrayOf('\n'.code.toByte())

            val headerLength =
                finalHeader.size

            // Header length: little-endian uint16
            fos.write(
                headerLength and 0xFF
            )

            fos.write(
                (headerLength shr 8) and 0xFF
            )

            fos.write(finalHeader)

            // Float32 little-endian
            DataOutputStream(
                fos
            ).use { output ->

                for (row in mel) {

                    for (value in row) {

                        val bits =
                            value.toRawBits()

                        output.writeByte(
                            bits and 0xFF
                        )

                        output.writeByte(
                            (bits shr 8) and 0xFF
                        )

                        output.writeByte(
                            (bits shr 16) and 0xFF
                        )

                        output.writeByte(
                            (bits shr 24) and 0xFF
                        )
                    }
                }
            }
        }
    }

    fun compute(
        samples: FloatArray
    ): Array<FloatArray> {

        if (samples.isEmpty()) {
            return emptyArray()
        }

        val pad = N_FFT / 2

        val padded =
            FloatArray(
                samples.size + 2 * pad
            )

        System.arraycopy(
            samples,
            0,
            padded,
            pad,
            samples.size
        )

        val nFrames =
            1 + (padded.size - N_FFT) / HOP_LENGTH

        val window =
            hannWindow(N_FFT)

        val melFilterBank =
            createMelFilterBank()

        val output =
            Array(nFrames) {
                FloatArray(N_MELS)
            }

        val frame =
            FloatArray(N_FFT)

        val powerSpectrum =
            FloatArray(N_FFT / 2 + 1)

        for (frameIndex in 0 until nFrames) {

            val start =
                frameIndex * HOP_LENGTH

            for (i in 0 until N_FFT) {

                frame[i] =
                    padded[start + i] *
                            window[i]
            }

            computePowerSpectrum(
                frame,
                powerSpectrum
            )

            for (mel in 0 until N_MELS) {

                var energy = 0.0

                for (bin in powerSpectrum.indices) {

                    energy +=
                        powerSpectrum[bin] *
                                melFilterBank[mel][bin]
                }

                output[frameIndex][mel] =
                    energy.toFloat()
            }
        }

        return output
    }

    private fun hannWindow(
        size: Int
    ): FloatArray {

        val window =
            FloatArray(size)

        for (i in 0 until size) {

            window[i] =
                (
                        0.5 -
                                0.5 *
                                cos(
                                    2.0 * PI * i / size
                                )
                        ).toFloat()
        }

        return window
    }

    private fun computePowerSpectrum(
        frame: FloatArray,
        output: FloatArray
    ) {

        val n = frame.size
        val half = n / 2

        for (k in 0..half) {

            var real = 0.0
            var imag = 0.0

            for (i in 0 until n) {

                val angle =
                    2.0 * PI * k * i / n

                real +=
                    frame[i] * cos(angle)

                imag -=
                    frame[i] *
                            kotlin.math.sin(angle)
            }

            output[k] =
                (
                        real * real +
                                imag * imag
                        ).toFloat()
        }
    }

    private fun createMelFilterBank():
            Array<FloatArray> {

        val nFftBins =
            N_FFT / 2 + 1

        val filters =
            Array(N_MELS) {
                FloatArray(nFftBins)
            }

        val fMin = 0.0
        val fMax = SAMPLE_RATE / 2.0

        val minMel =
            hzToMel(fMin)

        val maxMel =
            hzToMel(fMax)

        val melPoints =
            DoubleArray(N_MELS + 2)

        for (i in melPoints.indices) {

            melPoints[i] =
                melToHz(
                    minMel +
                            (maxMel - minMel) *
                            i /
                            (N_MELS + 1)
                )
        }

        val fftFrequencies =
            DoubleArray(nFftBins)

        for (i in fftFrequencies.indices) {

            fftFrequencies[i] =
                i.toDouble() *
                        SAMPLE_RATE /
                        N_FFT
        }

        for (m in 0 until N_MELS) {

            val left =
                melPoints[m]

            val center =
                melPoints[m + 1]

            val right =
                melPoints[m + 2]

            val enorm =
                2.0 /
                        (right - left)

            for (k in fftFrequencies.indices) {

                val frequency =
                    fftFrequencies[k]

                val weight =
                    when {

                        frequency < left ->
                            0.0

                        frequency <= center ->
                            (frequency - left) /
                                    (center - left)

                        frequency <= right ->
                            (right - frequency) /
                                    (right - center)

                        else ->
                            0.0
                    }

                filters[m][k] =
                    (weight * enorm).toFloat()
            }
        }

        return filters
    }

    private fun hzToMel(
        hz: Double
    ): Double {

        val fSp = 200.0 / 3.0

        return if (hz < 1000.0) {

            hz / fSp

        } else {

            val minLogHz = 1000.0

            val minLogMel =
                minLogHz / fSp

            val logStep =
                ln(6.4) / 27.0

            minLogMel +
                    ln(hz / minLogHz) /
                    logStep
        }
    }

    private fun melToHz(
        mel: Double
    ): Double {

        val fSp = 200.0 / 3.0

        val minLogHz = 1000.0

        val minLogMel =
            minLogHz / fSp

        val logStep =
            ln(6.4) / 27.0

        return if (mel < minLogMel) {

            mel * fSp

        } else {

            minLogHz *
                    kotlin.math.exp(
                        logStep *
                                (mel - minLogMel)
                    )
        }
    }
}