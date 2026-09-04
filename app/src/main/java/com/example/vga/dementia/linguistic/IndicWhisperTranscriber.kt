package com.example.vga.dementia.linguistic

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import java.io.File

/**
 * Speech-to-text used by VGA's linguistic pipeline.
 *
 * Backed by a sherpa-onnx offline **Zipformer transducer** rather than
 * Whisper. Whisper-tiny hallucinated repetition loops on this app's real
 * input - 8 kHz telephone-band call audio containing long silences - emitting
 * "How are you?" or "I'm sorry." indefinitely instead of transcribing. That
 * was reproduced after ruling out sample-rate conversion, decode language,
 * VAD silence removal, 30-second windowing and the tokens file.
 *
 * A transducer emits tokens aligned to audio frames, so it cannot run away
 * autoregressively the way Whisper does; it stays silent on silence instead of
 * inventing text.
 *
 * The class name and public API are unchanged so every existing caller keeps
 * working.
 */
class IndicWhisperTranscriber(
    private val context: Context
) {

    companion object {

        private const val TAG = "VGA_ASR"

        private const val ASSET_DIR = "indic_whisper"

        private const val ENCODER = "zipformer-encoder-epoch-99-avg-1.int8.onnx"
        private const val DECODER = "zipformer-decoder-epoch-99-avg-1.onnx"
        private const val JOINER = "zipformer-joiner-epoch-99-avg-1.int8.onnx"
        private const val TOKENS = "zipformer-tokens.txt"

        /** Required input rate; callers resample before this point. */
        const val REQUIRED_SAMPLE_RATE = 16_000
    }

    /**
     * Copies a bundled asset into cache once and returns its path.
     * sherpa-onnx needs real filesystem paths, not asset streams.
     */
    private fun copyAssetToCache(assetName: String): String {

        val outputFile = File(context.cacheDir, assetName)

        // Re-copy when the cached copy does not match the packaged asset, so a
        // model swap in a new build is picked up instead of silently reusing a
        // stale file.
        //
        // openFd() cannot be used here: .onnx assets are stored compressed in
        // the APK and it fails with "this file can not be opened as a file
        // descriptor; it is probably compressed". available() reports the
        // uncompressed length, and if even that is unavailable we fall back to
        // copying only when the cache file is missing.
        val expectedSize = runCatching {
            context.assets.open("$ASSET_DIR/$assetName").use { it.available().toLong() }
        }.getOrDefault(-1L)

        val needsCopy =
            !outputFile.exists() ||
                outputFile.length() == 0L ||
                (expectedSize > 0L && outputFile.length() != expectedSize)

        if (needsCopy) {

            context.assets.open("$ASSET_DIR/$assetName").use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "cached $assetName (${outputFile.length()} bytes)")
        }

        return outputFile.absolutePath
    }

    private val recognizer by lazy {

        OfflineRecognizer(
            config = OfflineRecognizerConfig(

                featConfig = FeatureConfig(
                    sampleRate = REQUIRED_SAMPLE_RATE,
                    featureDim = 80
                ),

                modelConfig = OfflineModelConfig(

                    transducer = OfflineTransducerModelConfig(
                        encoder = copyAssetToCache(ENCODER),
                        decoder = copyAssetToCache(DECODER),
                        joiner = copyAssetToCache(JOINER)
                    ),

                    tokens = copyAssetToCache(TOKENS),

                    numThreads = 2,

                    debug = false,

                    provider = "cpu",

                    modelType = "transducer"
                )
            )
        )
    }

    fun transcribe(
        samples: FloatArray,
        sampleRate: Int = REQUIRED_SAMPLE_RATE
    ): String {

        require(sampleRate == REQUIRED_SAMPLE_RATE) {
            "Speech recognition requires ${REQUIRED_SAMPLE_RATE} Hz audio, got $sampleRate Hz"
        }

        if (samples.isEmpty()) return ""

        val stream = recognizer.createStream()

        stream.acceptWaveform(samples, sampleRate)

        recognizer.decode(stream)

        return recognizer.getResult(stream).text.trim()
    }
}
