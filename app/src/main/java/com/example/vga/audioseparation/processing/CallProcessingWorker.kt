
package com.example.vga.audioseparation.processing

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vga.dementia.acoustic.AcousticAnalysisManager
import com.example.vga.dementia.acoustic.AcousticFeatureStore
import java.io.File
import kotlin.math.sqrt

class CallProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        val inputPath =
            inputData.getString("input_path")
                ?: return Result.failure()

        return try {

            // ====================================================
            // WORKER STARTED
            // ====================================================

            Log.d(
                "VGA_PROCESSING",
                "Worker started"
            )

            Log.d(
                "VGA_PROCESSING",
                "Input: $inputPath"
            )

            val file =
                File(inputPath)

            if (!file.exists()) {

                Log.e(
                    "VGA_PROCESSING",
                    "File does not exist"
                )

                return Result.failure()
            }

            // ====================================================
            // LOAD SAVED REFERENCE EMBEDDING
            // ====================================================

            val referenceEmbedding =
                ReferenceVoiceManager.loadEmbedding(
                    applicationContext
                )

            if (referenceEmbedding == null) {

                Log.e(
                    "VGA_MATCH",
                    "No reference voice embedding found"
                )

                return Result.failure()
            }

            Log.d(
                "VGA_MATCH",
                "Reference embedding loaded"
            )

            Log.d(
                "VGA_MATCH",
                "Reference embedding size=" +
                        referenceEmbedding.size
            )

            // ====================================================
            // READ ORIGINAL WAV
            // ====================================================

            val audio =
                AudioPreprocessor.readWav(
                    file
                )

            Log.d(
                "VGA_PROCESSING",
                "Original WAV: " +
                        "sampleRate=${audio.sampleRate}, " +
                        "channels=${audio.channels}, " +
                        "samples=${audio.samples.size}"
            )

            // ====================================================
            // RESAMPLE TO 16 kHz
            // ====================================================

            val audio16k =
                AudioPreprocessor.resampleTo16k(
                    audio
                )

            Log.d(
                "VGA_PROCESSING",
                "Resampled WAV: " +
                        "sampleRate=${audio16k.sampleRate}, " +
                        "channels=${audio16k.channels}, " +
                        "samples=${audio16k.samples.size}"
            )

            // ====================================================
            // KEEP ORIGINAL AUDIO TIMELINE
            // ====================================================

            val originalSamples =
                audio16k.samples.copyOf()

            // ====================================================
            // NORMALIZE FOR SPEAKER MATCHING
            // ====================================================

            val normalized =
                AudioPreprocessor.normalizeVolume(
                    originalSamples
                )

            Log.d(
                "VGA_PROCESSING",
                "Normalized samples=${normalized.size}"
            )

            Log.d(
                "VGA_PROCESSING",
                "Original duration=" +
                        "${normalized.size / 16000.0}s"
            )

            // ====================================================
            // PROCESSING SAMPLES
            // ====================================================

            val processingSamples =
                normalized

            // ====================================================
            // MEL SPECTROGRAM
            // ====================================================

            Log.d(
                "VGA_PROCESSING",
                "Starting mel spectrogram..."
            )

            val mel =
                MelSpectrogram.compute(
                    processingSamples
                )

            Log.d(
                "VGA_PROCESSING",
                "Mel spectrogram completed: " +
                        "frames=${mel.size}"
            )

            MelSpectrogram.logSummary(
                mel,
                applicationContext
            )

            // ====================================================
            // CREATE 160 × 40 CHUNKS
            // ====================================================

            val chunks =
                MelChunker.chunk(
                    mel
                )

            MelChunker.logSummary(
                chunks
            )

            if (chunks.isEmpty()) {

                Log.e(
                    "VGA_PROCESSING",
                    "No valid mel chunks"
                )

                return Result.failure()
            }

            MelChunker.logFirstChunk(
                chunks[0]
            )

            // ====================================================
            // GENERATE SPEAKER EMBEDDINGS
            // ====================================================

            val embeddings =
                ArrayList<FloatArray>()

            val confidences =
                ArrayList<Float>()

            SpeakerEncoder(
                applicationContext
            ).use { encoder ->

                for (index in chunks.indices) {

                    val chunk =
                        chunks[index]

                    // --------------------------------------------
                    // ENCODE CHUNK
                    // --------------------------------------------

                    val currentEmbedding =
                        encoder.encode(
                            chunk
                        )

                    embeddings.add(
                        currentEmbedding
                    )

                    // --------------------------------------------
                    // CALCULATE EMBEDDING NORM
                    // --------------------------------------------

                    var normSquared =
                        0.0

                    for (value in currentEmbedding) {

                        normSquared +=
                            value.toDouble() *
                                    value.toDouble()
                    }

                    val norm =
                        sqrt(normSquared)

                    Log.d(
                        "VGA_EMBEDDING",
                        "Chunk $index: " +
                                "embedding size=" +
                                currentEmbedding.size
                    )

                    Log.d(
                        "VGA_EMBEDDING",
                        "Chunk $index: norm=$norm"
                    )

                    // --------------------------------------------
                    // COMPARE WITH REFERENCE VOICE
                    // --------------------------------------------

                    val similarity =
                        VoiceMatcher.cosineSimilarity(
                            referenceEmbedding,
                            currentEmbedding
                        )

                    val confidence =
                        VoiceMatcher.similarityToConfidence(
                            similarity
                        )

                    confidences.add(
                        confidence
                    )

                    Log.d(
                        "VGA_MATCH",
                        "Chunk $index: " +
                                "similarity=$similarity, " +
                                "confidence=$confidence"
                    )
                }
            }

            // ====================================================
            // CONFIDENCE SUMMARY
            // ====================================================

            Log.d(
                "VGA_MATCH",
                "Confidence values=${confidences.size}"
            )

            // ====================================================
            // CREATE SAMPLE-LEVEL MASK
            // ====================================================

            val mask =
                AudioMaskProcessor.createMask(
                    processingSamples.size,
                    confidences
                )

            Log.d(
                "VGA_MASK",
                "Mask created: samples=${mask.size}"
            )

            // ====================================================
            // SMOOTH MASK
            // ====================================================

            val smoothedMask =
                AudioMaskProcessor.smoothMask(
                    mask,
                    200
                )

            Log.d(
                "VGA_MASK",
                "Mask smoothing completed"
            )

            // ====================================================
            // FADE PROTECTION
            // ====================================================

            val finalMask =
                AudioMaskProcessor.applyFadeProtection(
                    smoothedMask,
                    150
                )

            Log.d(
                "VGA_MASK",
                "Fade protection completed"
            )

            // ====================================================
            // APPLY VOICE MASK
            // ====================================================

            val separated =
                AudioMaskProcessor.applyMask(
                    processingSamples,
                    finalMask
                )

            // ====================================================
            // NORMALIZE SEPARATED AUDIO
            // ====================================================

            val normalizedOutput =
                AudioMaskProcessor.normalizeOutput(
                    separated
                )

            // ====================================================
            // SAVE INTO extracted_voice
            // ====================================================

            val extractedVoiceDirectory =
                File(
                    applicationContext.filesDir,
                    "extracted_voice"
                )

            if (!extractedVoiceDirectory.exists()) {

                val created =
                    extractedVoiceDirectory.mkdirs()

                Log.d(
                    "VGA_OUTPUT",
                    "Created extracted_voice directory=$created"
                )
            }

            // ====================================================
            // UNIQUE OUTPUT FILENAME
            // ====================================================

            val timestamp =
                System.currentTimeMillis()

            val outputFile =
                File(
                    extractedVoiceDirectory,
                    "my_voice_only_$timestamp.wav"
                )

            // ====================================================
            // SAVE WAV
            // ====================================================

            AudioPreprocessor.saveFloatWav(
                normalizedOutput,
                16000,
                outputFile
            )

            Log.d(
                "VGA_OUTPUT",
                "Voice-only audio saved: " +
                        outputFile.absolutePath
            )

            // ====================================================
            // ACOUSTIC FEATURE EXTRACTION
            // ====================================================

            Log.d(
                "VGA_ACOUSTIC",
                "Starting eGeMAPSv02 acoustic analysis"
            )

            val acousticAnalysisManager =
                AcousticAnalysisManager(
                    applicationContext
                )

            val acousticFeatures =
                acousticAnalysisManager.analyze(
                    outputFile
                )

            if (acousticFeatures == null) {

                Log.e(
                    "VGA_ACOUSTIC",
                    "Acoustic feature extraction failed"
                )

            } else {

                // =================================================
                // SAVE ACOUSTIC FEATURES PERMANENTLY
                // =================================================

                AcousticFeatureStore.save(
                    context = applicationContext,
                    features = acousticFeatures.values
                )

                Log.d(
                    "VGA_ACOUSTIC",
                    "Acoustic feature extraction successful"
                )

                Log.d(
                    "VGA_ACOUSTIC",
                    "Feature count=${acousticFeatures.size}"
                )

                Log.d(
                    "VGA_ACOUSTIC",
                    "Features=" +
                            acousticFeatures.values.contentToString()
                )

                Log.d(
                    "VGA_ACOUSTIC",
                    "Acoustic features saved permanently"
                )
            }

            // ====================================================
            // OUTPUT INFORMATION
            // ====================================================

            Log.d(
                "VGA_OUTPUT",
                "Output samples=" +
                        normalizedOutput.size
            )

            Log.d(
                "VGA_OUTPUT",
                "Output duration=" +
                        "${normalizedOutput.size / 16000.0}s"
            )

            // ====================================================
            // CALL-LEVEL EMBEDDING
            // ====================================================

            if (embeddings.isNotEmpty()) {

                val callEmbedding =
                    EmbeddingUtils.meanAndNormalize(
                        embeddings
                    )

                val callNorm =
                    EmbeddingUtils.calculateNorm(
                        callEmbedding
                    )

                Log.d(
                    "VGA_EMBEDDING",
                    "Call embedding size=" +
                            callEmbedding.size
                )

                Log.d(
                    "VGA_EMBEDDING",
                    "Call embedding norm=" +
                            callNorm
                )

                val callSimilarity =
                    VoiceMatcher.cosineSimilarity(
                        referenceEmbedding,
                        callEmbedding
                    )

                Log.d(
                    "VGA_MATCH",
                    "Call-level similarity=" +
                            callSimilarity
                )
            }

            // ====================================================
            // PROCESSING COMPLETED
            // ====================================================

            Log.d(
                "VGA_PROCESSING",
                "Voice separation completed successfully"
            )

            Result.success()

        } catch (e: Exception) {

            Log.e(
                "VGA_PROCESSING",
                "Processing failed",
                e
            )

            Result.failure()
        }
    }
}
