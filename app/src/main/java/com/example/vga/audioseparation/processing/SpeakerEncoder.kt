package com.example.vga.audioseparation.processing

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class SpeakerEncoder(
    context: Context
) : AutoCloseable {

    private val environment =
        OrtEnvironment.getEnvironment()

    private val session: OrtSession

    init {

        val modelBytes =
            context.assets.open(
                "models/speaker_encoder.onnx"
            ).use {
                it.readBytes()
            }

        session =
            environment.createSession(
                modelBytes,
                OrtSession.SessionOptions()
            )
    }

    /**
     * Input:
     *     [160][40]
     *
     * ONNX input:
     *     [1][160][40]
     *
     * Output:
     *     [1][256]
     */
    fun encode(
        mel: Array<FloatArray>
    ): FloatArray {

        require(mel.size == 160) {
            "Expected 160 frames, got ${mel.size}"
        }

        require(
            mel.all { it.size == 40 }
        ) {
            "Expected 40 mel bins per frame"
        }

        val input =
            FloatArray(160 * 40)

        var index = 0

        for (frame in mel) {

            for (value in frame) {

                input[index++] = value
            }
        }

        val inputTensor =
            OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(input),
                longArrayOf(
                    1,
                    160,
                    40
                )
            )

        inputTensor.use { tensor ->

            val inputs =
                mapOf(
                    "mel" to tensor
                )

            session.run(inputs).use { result ->

                val output =
                    result[0].value

                val embedding =
                    output as Array<FloatArray>

                return embedding[0]
            }
        }
    }

    override fun close() {
        session.close()
    }
}