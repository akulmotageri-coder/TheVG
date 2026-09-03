package com.example.vga.audioseparation.audio

import android.media.MediaPlayer

class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    fun play(filePath: String) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}