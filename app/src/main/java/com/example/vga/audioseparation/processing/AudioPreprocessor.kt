package com.example.vga.audioseparation.processing

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.roundToInt
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import kotlin.math.pow
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int
)

object AudioPreprocessor {

    fun saveFloatWav(
        samples: FloatArray,
        sampleRate: Int,
        file: File
    ) {
        val pcmData = ByteArray(samples.size * 2)

        for (i in samples.indices) {

            val sample =
                (samples[i].coerceIn(-1f, 1f) * 32767f)
                    .toInt()
                    .toShort()

            pcmData[i * 2] =
                (sample.toInt() and 0xFF).toByte()

            pcmData[i * 2 + 1] =
                ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        val dataSize = pcmData.size
        val byteRate = sampleRate * 2
        val blockAlign = 2

        file.outputStream().use { output ->

            fun writeInt(value: Int) {
                output.write(value and 0xFF)
                output.write((value shr 8) and 0xFF)
                output.write((value shr 16) and 0xFF)
                output.write((value shr 24) and 0xFF)
            }

            fun writeShort(value: Int) {
                output.write(value and 0xFF)
                output.write((value shr 8) and 0xFF)
            }

            output.write("RIFF".toByteArray())
            writeInt(36 + dataSize)
            output.write("WAVE".toByteArray())

            output.write("fmt ".toByteArray())
            writeInt(16)
            writeShort(1)
            writeShort(1)
            writeInt(sampleRate)
            writeInt(byteRate)
            writeShort(blockAlign)
            writeShort(16)

            output.write("data".toByteArray())
            writeInt(dataSize)
            output.write(pcmData)
        }
    }

    fun trimLongSilences(samples: FloatArray): FloatArray {

        if (samples.isEmpty()) {
            return samples
        }

        val sampleRate = 16_000
        val frameSamples = 480 // 30 ms @ 16 kHz

        // Match Resemblyzer:
        // vad_window_length = 30 ms
        // mode = 3 / VERY_AGGRESSIVE
        // vad_moving_average_width = 8
        // vad_max_silence_length = 6

        val usableLength =
            samples.size - (samples.size % frameSamples)

        if (usableLength <= 0) {
            return samples
        }

        val trimmedInput =
            samples.copyOf(usableLength)

        val voiceFlags =
            BooleanArray(usableLength / frameSamples)

        VadWebRTC(
            SampleRate.SAMPLE_RATE_16K,
            FrameSize.FRAME_SIZE_480,
            Mode.VERY_AGGRESSIVE,
            50,
            300
        ).use { vad ->

            for (i in voiceFlags.indices) {

                val start = i * frameSamples
                val end = start + frameSamples

                val frame =
                    trimmedInput.copyOfRange(start, end)

                voiceFlags[i] =
                    vad.isSpeech(frame)
            }
        }

        // Resemblyzer moving average width = 8
        val width = 8

        val audioMask =
            BooleanArray(voiceFlags.size)

        for (i in voiceFlags.indices) {

            var sum = 0

            val start =
                maxOf(0, i - (width - 1) / 2)

            val end =
                minOf(
                    voiceFlags.size,
                    i + width / 2 + 1
                )

            for (j in start until end) {
                if (voiceFlags[j]) {
                    sum++
                }
            }

            val average =
                sum.toFloat() / (end - start)

            audioMask[i] =
                average.roundToInt() != 0
        }

        // Dilate voiced regions.
        // Resemblyzer uses:
        // np.ones(vad_max_silence_length + 1)
        // where vad_max_silence_length = 6
        //
        // This preserves short gaps between speech regions.

        val dilation =
            7

        val dilatedMask =
            BooleanArray(audioMask.size)

        for (i in audioMask.indices) {

            var voiced = false

            val start =
                maxOf(0, i - dilation + 1)

            val end =
                minOf(
                    audioMask.size,
                    i + dilation
                )

            for (j in start until end) {
                if (audioMask[j]) {
                    voiced = true
                    break
                }
            }

            dilatedMask[i] = voiced
        }

        // Keep only voiced samples.
        var outputSize = 0

        for (flag in dilatedMask) {
            if (flag) {
                outputSize += frameSamples
            }
        }

        val result =
            FloatArray(outputSize)

        var outputIndex = 0

        for (i in dilatedMask.indices) {

            if (!dilatedMask[i]) {
                continue
            }

            val start = i * frameSamples

            for (j in 0 until frameSamples) {
                result[outputIndex++] =
                    trimmedInput[start + j]
            }
        }

        return result
    }

    fun normalizeVolume(
        samples: FloatArray,
        targetDbfs: Float = -30f
    ): FloatArray {

        if (samples.isEmpty()) {
            return samples
        }

        var sumSquares = 0.0

        for (sample in samples) {
            sumSquares += sample.toDouble() * sample.toDouble()
        }

        val rms = kotlin.math.sqrt(
            sumSquares / samples.size
        )

        if (rms <= 0.0) {
            return samples
        }

        val currentDbfs =
            20.0 * kotlin.math.log10(rms)

        val dbChange =
            targetDbfs - currentDbfs

        // Resemblyzer uses increase_only=True.
        // Therefore, never reduce the volume.
        if (dbChange <= 0.0) {
            return samples
        }

        val gain =
            10.0.pow(dbChange / 20.0)

        return FloatArray(samples.size) { index ->
            (samples[index] * gain).toFloat()
        }
    }

    fun resampleTo16k(audio: AudioData): AudioData {

        if (audio.sampleRate == 16_000) {
            return audio
        }

        if (audio.sampleRate != 8_000) {
            throw IllegalArgumentException(
                "Unsupported sample rate: ${audio.sampleRate} Hz"
            )
        }

        val input = audio.samples

        val outputLength =
            (input.size * 16_000L / 8_000L).toInt()

        val output = FloatArray(outputLength)

        for (i in output.indices) {

            val sourcePosition =
                i.toDouble() * 8_000.0 / 16_000.0

            val index =
                sourcePosition.toInt()

            val fraction =
                sourcePosition - index

            output[i] =
                if (index + 1 < input.size) {
                    (
                            input[index] * (1.0 - fraction) +
                                    input[index + 1] * fraction
                            ).toFloat()
                } else {
                    input[index]
                }
        }

        return AudioData(
            samples = output,
            sampleRate = 16_000,
            channels = 1
        )
    }

    fun readWav(file: File): AudioData {

        FileInputStream(file).use { input ->

            val header = ByteArray(44)

            if (input.read(header) != 44) {
                throw IllegalArgumentException("Invalid WAV file")
            }

            val buffer = ByteBuffer
                .wrap(header)
                .order(ByteOrder.LITTLE_ENDIAN)

            val channels = buffer.getShort(22).toInt()
            val sampleRate = buffer.getInt(24)
            val bitsPerSample = buffer.getShort(34).toInt()

            if (bitsPerSample != 16) {
                throw IllegalArgumentException(
                    "Only 16-bit PCM WAV is currently supported"
                )
            }

            val dataSize = buffer.getInt(40)

            val audioBytes = ByteArray(dataSize)

            var offset = 0

            while (offset < dataSize) {
                val read = input.read(
                    audioBytes,
                    offset,
                    dataSize - offset
                )

                if (read <= 0) break

                offset += read
            }

            val pcm = ByteBuffer
                .wrap(audioBytes)
                .order(ByteOrder.LITTLE_ENDIAN)

            val totalSamples =
                dataSize / 2

            val rawSamples =
                ShortArray(totalSamples)

            for (i in rawSamples.indices) {
                rawSamples[i] = pcm.short
            }

            val monoSamples: FloatArray

            if (channels == 1) {

                monoSamples = FloatArray(rawSamples.size)

                for (i in rawSamples.indices) {
                    monoSamples[i] =
                        rawSamples[i] / 32768f
                }

            } else {

                val frameCount =
                    rawSamples.size / channels

                monoSamples =
                    FloatArray(frameCount)

                for (frame in 0 until frameCount) {

                    var sum = 0f

                    for (channel in 0 until channels) {
                        sum += rawSamples[
                            frame * channels + channel
                        ] / 32768f
                    }

                    monoSamples[frame] =
                        sum / channels
                }
            }

            return AudioData(
                samples = monoSamples,
                sampleRate = sampleRate,
                channels = 1
            )
        }
    }
    fun prepareForResampling(audio: AudioData): AudioData {
        if (audio.sampleRate == 16_000) {
            return AudioData(
                samples = audio.samples,
                sampleRate = 16_000,
                channels = 1
            )
        }

        throw IllegalArgumentException(
            "Sample rate ${audio.sampleRate} Hz requires high-quality resampling"
        )
    }
}