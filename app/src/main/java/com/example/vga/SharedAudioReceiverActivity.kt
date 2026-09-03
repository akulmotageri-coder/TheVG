
package com.example.vga

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vga.audioseparation.processing.CallProcessingWorker
import com.example.vga.audioseparation.storage.AudioStorage

class SharedAudioReceiverActivity : Activity() {

    companion object {
        private const val TAG = "VGA_SHARE"
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        handleSharedAudio(intent)

        // Close immediately.
        // The normal VGA UI is never opened.
        finish()
    }

    private fun handleSharedAudio(
        intent: Intent?
    ) {

        if (intent?.action != Intent.ACTION_SEND) {

            Log.e(
                TAG,
                "Unsupported action: ${intent?.action}"
            )

            return
        }

        val audioUri =
            intent.getParcelableExtra<Uri>(
                Intent.EXTRA_STREAM
            )

        if (audioUri == null) {

            Log.e(
                TAG,
                "No shared audio URI received"
            )

            return
        }

        try {

            Log.d(
                TAG,
                "Shared audio URI: $audioUri"
            )

            Log.d(
                TAG,
                "MIME type: ${contentResolver.getType(audioUri)}"
            )

            // --------------------------------
            // Copy shared file into VGA storage
            // --------------------------------

            val storage =
                AudioStorage(this)

            val savedFile =
                storage.saveSharedAudio(
                    audioUri
                )

            if (savedFile == null) {

                Log.e(
                    TAG,
                    "Failed to save shared audio"
                )

                return
            }

            Log.d(
                TAG,
                "Audio saved: ${savedFile.absolutePath}"
            )

            // --------------------------------
            // Send file path to Worker
            // --------------------------------

            val inputData =
                Data.Builder()
                    .putString(
                        "input_path",
                        savedFile.absolutePath
                    )
                    .build()

            val processingRequest =
                OneTimeWorkRequestBuilder<CallProcessingWorker>()
                    .setInputData(inputData)
                    .build()

            WorkManager
                .getInstance(applicationContext)
                .enqueue(processingRequest)

            Log.d(
                TAG,
                "Background processing queued"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to handle shared audio",
                e
            )
        }
    }
}
