package com.example.vga.audioseparation.repository

import android.content.Context
import com.example.vga.audioseparation.models.CallRecording
import java.io.File

class CallRecordingRepository(
    private val context: Context
) {

    fun getRecordings(): List<CallRecording> {

        val recordingsDirectory = File(
            context.filesDir,
            "call_recordings"
        )

        if (!recordingsDirectory.exists()) {
            return emptyList()
        }

        return recordingsDirectory
            .listFiles()
            ?.filter { it.isFile && it.extension.lowercase() == "wav" }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                CallRecording(
                    file = it,
                    displayName = it.name
                )
            }
            ?: emptyList()
    }
}