package com.example.vga.audioseparation.processing

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.sqrt

object ReferenceVoiceProcessor {

    private const val TAG = "VGA_REFERENCE"

    fun processAndSave(
        context: Context,
        voiceFile: File
    ): FloatArray {

        Log.d(TAG, "Starting reference voice processing")
        Log.d(TAG, "Input: ${voiceFile.absolutePath}")

        require(voiceFile.exists()) {
            "Reference voice file does not exist"
        }

        // --------------------------------
        // Decode M4A
        // --------------------------------

        val decoded =
            AudioDecoder.decodeToMonoFloat(
                voiceFile
            )

        Log.d(
            TAG,
            "Decoded: " +
                    "sampleRate=${decoded.sampleRate}, " +
                    "samples=${decoded.samples.size}"
        )

        // --------------------------------
        // Convert to 16 kHz
        // --------------------------------

        val audio16k =
            AudioPreprocessor.resampleTo16k(
                decoded
            )

        Log.d(
            TAG,
            "Resampled: " +
                    "sampleRate=${audio16k.sampleRate}, " +
                    "samples=${audio16k.samples.size}"
        )

        // --------------------------------
        // Normalize
        // --------------------------------

        val normalized =
            AudioPreprocessor.normalizeVolume(
                audio16k.samples
            )

        // --------------------------------
        // VAD
        // --------------------------------

        val trimmed =
            AudioPreprocessor.trimLongSilences(
                normalized
            )

        Log.d(
            TAG,
            "After VAD: ${trimmed.size} samples"
        )

        require(trimmed.isNotEmpty()) {
            "No usable speech found"
        }

        // --------------------------------
        // Mel spectrogram
        // --------------------------------

        val mel =
            MelSpectrogram.compute(
                trimmed
            )

        Log.d(
            TAG,
            "Mel frames=${mel.size}"
        )

        // --------------------------------
        // Split into 160-frame chunks
        // --------------------------------

        val chunks =
            MelChunker.chunk(
                mel
            )

        Log.d(
            TAG,
            "Chunks=${chunks.size}"
        )

        require(chunks.isNotEmpty()) {
            "Reference recording is too short"
        }

        // --------------------------------
        // Encode every chunk
        // --------------------------------

        val embeddings =
            ArrayList<FloatArray>()

        SpeakerEncoder(context).use { encoder ->

            for (
            (index, chunk) in chunks.withIndex()
            ) {

                val embedding =
                    encoder.encode(chunk)

                embeddings.add(
                    embedding
                )

                Log.d(
                    TAG,
                    "Chunk $index encoded"
                )
            }
        }

        // --------------------------------
        // Average embeddings
        // --------------------------------

        val referenceEmbedding =
            averageEmbeddings(
                embeddings
            )

        // --------------------------------
        // Save reference embedding
        // --------------------------------

        ReferenceVoiceManager.saveEmbedding(
            context,
            referenceEmbedding
        )

        Log.d(
            TAG,
            "Reference embedding saved"
        )

        Log.d(
            TAG,
            "Embedding size=${referenceEmbedding.size}"
        )

        return referenceEmbedding
    }

    private fun averageEmbeddings(
        embeddings: List<FloatArray>
    ): FloatArray {

        require(embeddings.isNotEmpty())

        val size =
            embeddings[0].size

        val average =
            FloatArray(size)

        for (embedding in embeddings) {

            require(
                embedding.size == size
            )

            for (i in embedding.indices) {

                average[i] +=
                    embedding[i]
            }
        }

        for (i in average.indices) {

            average[i] /=
                embeddings.size.toFloat()
        }

        // L2 normalize
        var norm = 0.0

        for (value in average) {
            norm += value * value
        }

        norm = sqrt(norm)

        if (norm > 0.0) {

            for (i in average.indices) {

                average[i] =
                    (
                            average[i] / norm
                            ).toFloat()
            }
        }

        return average
    }
}