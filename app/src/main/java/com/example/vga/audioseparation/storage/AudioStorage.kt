package com.example.vga.audioseparation.storage

import android.content.Context
import android.net.Uri
import java.io.File

class AudioStorage(
    private val context: Context
) {

    fun saveSharedAudio(uri: Uri): File? {
        val recordingsDirectory = File(
            context.filesDir,
            "call_recordings"
        )

        if (!recordingsDirectory.exists()) {
            recordingsDirectory.mkdirs()
        }

        val file = File(
            recordingsDirectory,
            "call_${System.currentTimeMillis()}.wav"
        )

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()

            if (file.exists()) {
                file.delete()
            }

            null
        }
    }
}