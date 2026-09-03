package com.example.vga.audioseparation.processing

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer

object AudioDecoder {

    fun decodeToMonoFloat(
        file: File
    ): AudioData {

        val extractor = MediaExtractor()

        extractor.setDataSource(
            file.absolutePath
        )

        var audioTrackIndex = -1
        var format: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {

            val trackFormat =
                extractor.getTrackFormat(i)

            val mime =
                trackFormat.getString(
                    MediaFormat.KEY_MIME
                )

            if (mime?.startsWith("audio/") == true) {

                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        require(audioTrackIndex >= 0) {
            "No audio track found"
        }

        extractor.selectTrack(audioTrackIndex)

        val inputFormat =
            requireNotNull(format)

        val mime =
            requireNotNull(
                inputFormat.getString(
                    MediaFormat.KEY_MIME
                )
            )

        val sampleRate =
            inputFormat.getInteger(
                MediaFormat.KEY_SAMPLE_RATE
            )

        val channels =
            inputFormat.getInteger(
                MediaFormat.KEY_CHANNEL_COUNT
            )

        val codec =
            MediaCodec.createDecoderByType(mime)

        codec.configure(
            inputFormat,
            null,
            null,
            0
        )

        codec.start()

        val output =
            ArrayList<Float>()

        val bufferInfo =
            MediaCodec.BufferInfo()

        var inputDone = false
        var outputDone = false

        while (!outputDone) {

            if (!inputDone) {

                val inputIndex =
                    codec.dequeueInputBuffer(
                        10_000
                    )

                if (inputIndex >= 0) {

                    val inputBuffer =
                        codec.getInputBuffer(
                            inputIndex
                        )!!

                    val sampleSize =
                        extractor.readSampleData(
                            inputBuffer,
                            0
                        )

                    if (sampleSize < 0) {

                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )

                        inputDone = true

                    } else {

                        codec.queueInputBuffer(
                            inputIndex,
                            0,
                            sampleSize,
                            extractor.sampleTime,
                            0
                        )

                        extractor.advance()
                    }
                }
            }

            val outputIndex =
                codec.dequeueOutputBuffer(
                    bufferInfo,
                    10_000
                )

            when {

                outputIndex >= 0 -> {

                    val outputBuffer =
                        codec.getOutputBuffer(
                            outputIndex
                        )

                    if (
                        outputBuffer != null &&
                        bufferInfo.size > 0
                    ) {

                        outputBuffer.position(
                            bufferInfo.offset
                        )

                        outputBuffer.limit(
                            bufferInfo.offset +
                                    bufferInfo.size
                        )

                        val pcm =
                            outputBuffer
                                .slice()
                                .order(
                                    java.nio.ByteOrder.LITTLE_ENDIAN
                                )

                        while (
                            pcm.remaining() >= 2
                        ) {

                            var sum = 0f

                            for (
                            channel in 0 until channels
                            ) {

                                if (
                                    pcm.remaining() < 2
                                ) {
                                    break
                                }

                                val sample =
                                    pcm.short

                                sum +=
                                    sample / 32768f
                            }

                            output.add(
                                sum / channels
                            )
                        }
                    }

                    codec.releaseOutputBuffer(
                        outputIndex,
                        false
                    )

                    if (
                        bufferInfo.flags and
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        != 0
                    ) {
                        outputDone = true
                    }
                }

                outputIndex ==
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {

                    // Decoder may report the actual output
                    // format here. We continue using PCM output.
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val samples =
            FloatArray(output.size)

        for (i in output.indices) {
            samples[i] = output[i]
        }

        return AudioData(
            samples = samples,
            sampleRate = sampleRate,
            channels = 1
        )
    }
}