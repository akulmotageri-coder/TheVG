package com.example.vga.dementia.linguistic

import android.content.Context
import android.util.Log
import com.example.vga.audioseparation.processing.AudioDecoder
import java.io.File

object WhisperTest {

    fun test(
        context: Context,
        audioFile: File
    ) {

        try {

            Log.d(
                "WHISPER_TEST",
                "Starting Whisper test"
            )

            val audio =
                AudioDecoder.decodeToMonoFloat(
                    audioFile
                )

            Log.d(
                "WHISPER_TEST",
                "Audio decoded: " +
                        "samples=${audio.samples.size}, " +
                        "sampleRate=${audio.sampleRate}"
            )

            val transcriber =
                IndicWhisperTranscriber(
                    context
                )

            val text =
                transcriber.transcribe(
                    samples = audio.samples,
                    sampleRate = audio.sampleRate
                )

            Log.d(
                "WHISPER_TEST",
                "TRANSCRIPTION:"
            )

            Log.d(
                "WHISPER_TEST",
                text
            )

        } catch (e: Exception) {

            Log.e(
                "WHISPER_TEST",
                "Whisper test failed",
                e
            )
        }
    }
}