package com.example.vga.insight

import android.content.Context
import android.util.Log
import com.example.vga.audioseparation.processing.AudioData
import com.example.vga.audioseparation.processing.AudioDecoder
import com.example.vga.audioseparation.processing.AudioPreprocessor
import com.example.vga.dementia.linguistic.IndicWhisperTranscriber
import java.io.File


/**
 * Decodes a recording, brings it to the 16 kHz IndicWhisper requires, and
 * transcribes it.
 *
 * Call recordings are commonly 8 kHz, which the transcriber rejects outright
 * (`require(sampleRate == 16000)`). CallProcessingWorker already resamples
 * before its own pipeline; this does the same for the transcription path so
 * both behave identically.
 *
 * Kept in one place so the two callers (Call Recordings screen and the
 * Linguistic Insights screen) cannot drift apart.
 */
object TranscriptionSource {

    private const val TAG = "VGA_TRANSCRIBE"
    private const val TARGET_SAMPLE_RATE = 16_000

    /** Whisper's fixed encoder window. */
    private const val WINDOW_SECONDS = 30

    /**
     * Shown when the recogniser only produces repetition.
     *
     * Measured cause: the bundled whisper-tiny int8 checkpoint hallucinates
     * repeated phrases ("How are you?", "I'm sorry.") on 8 kHz telephone-band
     * call audio containing long silences, instead of transcribing it.
     *
     * Ruled out by testing, so do not re-investigate these: sample-rate
     * conversion (verified 8 kHz -> 16 kHz), explicit decode language,
     * VAD silence removal, 30-second windowing, and the tokens file (sherpa
     * reads the stop-token id from the encoder ONNX metadata, not from
     * tokens.txt). A larger checkpoint or better source audio is the fix.
     */
    private const val LOOP_MESSAGE =
        "The speech recogniser produced only repeated text for this recording, " +
            "so no usable transcript could be produced. The bundled whisper-tiny " +
            "model tends to do this on low-bandwidth (8 kHz) call audio with long " +
            "silences. A larger Whisper model, or higher-quality 16 kHz audio, is " +
            "needed for a reliable transcript."

    /**
     * Blocking: call from a background dispatcher.
     *
     * @throws IllegalStateException with a readable reason when the audio
     *   cannot be prepared for transcription.
     */
    fun transcribe(context: Context, file: File): String {

        val decoded = AudioDecoder.decodeToMonoFloat(file)

        Log.d(
            TAG,
            "${file.name}: decoded ${decoded.samples.size} samples @ " +
                "${decoded.sampleRate} Hz"
        )

        val prepared = ensure16k(decoded)

        Log.d(
            TAG,
            "${file.name}: transcribing ${prepared.samples.size} samples @ " +
                "${prepared.sampleRate} Hz"
        )

        if (prepared.samples.isEmpty()) {
            error("The recording contained no audio samples.")
        }

        // Call recordings are mostly silence (measured: ~73% near-silent on a
        // real 39s call). Whisper hallucinates repetition loops across long
        // silences - "I'm sorry." and "How are you?" repeated indefinitely -
        // so the silence is removed first using the VAD trimming the audio
        // pipeline already relies on for reference-voice enrolment.
        val speechOnly = removeSilence(prepared)

        return transcribeInWindows(context, speechOnly)
    }

    /**
     * Normalises level and drops long silences, reusing the existing
     * [AudioPreprocessor] helpers (WebRTC VAD) rather than a new
     * implementation. Falls back to the untrimmed audio if trimming removes
     * everything, so a quiet recording is still attempted.
     */
    private fun removeSilence(audio: AudioData): AudioData {

        return runCatching {

            val normalised = AudioPreprocessor.normalizeVolume(audio.samples)
            val trimmed = AudioPreprocessor.trimLongSilences(normalised)

            if (trimmed.isEmpty()) {
                Log.w(TAG, "VAD removed all audio; using untrimmed samples")
                return@runCatching audio
            }

            val removedPercent =
                (1.0 - trimmed.size.toDouble() / audio.samples.size) * 100.0

            Log.d(
                TAG,
                "VAD kept ${trimmed.size} of ${audio.samples.size} samples " +
                    "(${removedPercent.toInt()}% silence removed, " +
                    "${"%.1f".format(trimmed.size / audio.sampleRate.toDouble())}s speech)"
            )

            AudioData(
                samples = trimmed,
                sampleRate = audio.sampleRate,
                channels = 1
            )

        }.getOrElse {
            Log.w(TAG, "Silence removal failed (${it.message}); using raw audio")
            audio
        }
    }

    /**
     * Whisper's encoder works on a fixed 30-second window, so a longer
     * recording is otherwise truncated to its first 30 seconds. Audio is
     * therefore transcribed in 30-second windows and joined.
     *
     * Windows whose output is a recogniser loop are dropped rather than
     * concatenated, so one bad window cannot poison the whole transcript.
     */
    private fun transcribeInWindows(
        context: Context,
        audio: AudioData
    ): String {

        val transcriber = IndicWhisperTranscriber(context)
        val windowSamples = WINDOW_SECONDS * audio.sampleRate

        if (audio.samples.size <= windowSamples) {

            val single = transcriber.transcribe(
                samples = audio.samples,
                sampleRate = audio.sampleRate
            ).trim()

            // The same loop check the multi-window path applies, so a looped
            // result is never surfaced as if it were a real transcript.
            if (single.isNotBlank() && TranscriptAnalytics.isDegenerateOutput(single)) {
                Log.w(TAG, "single window is a recogniser loop (${single.take(60)}…)")
                error(LOOP_MESSAGE)
            }

            return single
        }

        val pieces = mutableListOf<String>()
        var start = 0
        var windowIndex = 0
        var droppedWindows = 0

        while (start < audio.samples.size) {

            val end = minOf(start + windowSamples, audio.samples.size)

            // A trailing sliver carries no usable speech.
            if (end - start >= audio.sampleRate) {

                val piece = transcriber.transcribe(
                    samples = audio.samples.copyOfRange(start, end),
                    sampleRate = audio.sampleRate
                ).trim()

                when {
                    piece.isBlank() -> Unit

                    TranscriptAnalytics.isDegenerateOutput(piece) -> {
                        droppedWindows++
                        Log.w(
                            TAG,
                            "window $windowIndex dropped: recogniser loop " +
                                "(${piece.take(60)}…)"
                        )
                    }

                    else -> pieces += piece
                }
            }

            windowIndex++
            start = end
        }

        Log.d(
            TAG,
            "transcribed $windowIndex window(s), kept ${pieces.size}, " +
                "dropped $droppedWindows"
        )

        // Reporting "no speech was recognised" here would be wrong and
        // confusing: the recogniser did produce output, it was just looping.
        // Surface the real reason instead.
        if (pieces.isEmpty() && droppedWindows > 0) {
            error(LOOP_MESSAGE)
        }

        return pieces.joinToString(" ")
    }

    /**
     * Returns [audio] at 16 kHz.
     *
     * 8 kHz is delegated to the existing [AudioPreprocessor.resampleTo16k] so
     * the transcription path uses exactly the same conversion the audio
     * pipeline already relies on. Other rates fall back to a local linear
     * resample, since that helper only accepts 8 kHz and 16 kHz.
     */
    private fun ensure16k(audio: AudioData): AudioData {

        if (audio.sampleRate == TARGET_SAMPLE_RATE) return audio

        if (audio.sampleRate == 8_000) {
            return AudioPreprocessor.resampleTo16k(audio)
        }

        if (audio.sampleRate <= 0) {
            error("Recording reported an invalid sample rate (${audio.sampleRate} Hz).")
        }

        return AudioData(
            samples = resampleLinear(
                input = audio.samples,
                inputRate = audio.sampleRate,
                outputRate = TARGET_SAMPLE_RATE
            ),
            sampleRate = TARGET_SAMPLE_RATE,
            channels = 1
        )
    }

    /** Linear interpolation resampler for rates other than 8 kHz. */
    private fun resampleLinear(
        input: FloatArray,
        inputRate: Int,
        outputRate: Int
    ): FloatArray {

        if (input.isEmpty() || inputRate == outputRate) return input

        val outputLength =
            (input.size.toLong() * outputRate / inputRate).toInt()

        if (outputLength <= 0) return FloatArray(0)

        val output = FloatArray(outputLength)
        val ratio = inputRate.toDouble() / outputRate.toDouble()

        for (i in output.indices) {
            val position = i * ratio
            val left = position.toInt()
            val right = minOf(left + 1, input.lastIndex)
            val fraction = position - left

            output[i] =
                (input[left] * (1.0 - fraction) + input[right] * fraction).toFloat()
        }

        return output
    }
}
