package com.example.vga.audioseparation.audio

import android.content.Context
import android.media.MediaRecorder

class VoiceRecorder(
    private val context: Context
) {

    private var recorder: MediaRecorder? = null

    fun startRecording(outputPath: String) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputPath)

            prepare()
            start()
        }
    }

    fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }

        recorder = null
    }
}