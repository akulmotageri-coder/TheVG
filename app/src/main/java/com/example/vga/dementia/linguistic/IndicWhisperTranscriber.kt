package com.example.vga.dementia.linguistic

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import java.io.File

class IndicWhisperTranscriber(
    private val context: Context
) {

    private fun copyAssetToCache(
        assetName: String
    ): String {

        val outputFile =
            File(
                context.cacheDir,
                assetName
            )

        if (!outputFile.exists()) {

            context.assets
                .open("indic_whisper/$assetName")
                .use { input ->

                    outputFile
                        .outputStream()
                        .use { output ->

                            input.copyTo(output)
                        }
                }
        }

        return outputFile.absolutePath
    }

    private val encoderPath =
        copyAssetToCache(
            "tiny-encoder.int8.onnx"
        )

    private val decoderPath =
        copyAssetToCache(
            "tiny-decoder.int8.onnx"
        )

    private val tokensPath =
        copyAssetToCache(
            "tiny-tokens.txt"
        )

    private val recognizer =
        OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(
                    sampleRate = 16000,
                    featureDim = 80
                ),

                modelConfig =
                    com.k2fsa.sherpa.onnx.OfflineModelConfig(
                        whisper =
                            OfflineWhisperModelConfig(
                                encoder = encoderPath,
                                decoder = decoderPath,
                                language = "",
                                task = "transcribe"
                            ),

                        tokens = tokensPath,

                        numThreads = 2,

                        debug = false,

                        provider = "cpu"
                    )
            )
        )
    fun transcribe(
        samples: FloatArray,
        sampleRate: Int = 16000
    ): String {

        require(sampleRate == 16000) {
            "Whisper requires 16 kHz audio"
        }

        val stream =
            recognizer.createStream()

        stream.acceptWaveform(
            samples,
            sampleRate
        )

        recognizer.decode(
            stream
        )

        return recognizer
            .getResult(stream)
            .text
            .trim()
    }
}