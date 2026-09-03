package com.example.vga.audioseparation.processing

import android.content.Context
import java.io.File

object ReferenceVoiceManager {

    private const val EMBEDDING_FILE =
        "my_voice_embedding.bin"

    fun saveEmbedding(
        context: Context,
        embedding: FloatArray
    ) {
        require(embedding.size == 256) {
            "Expected 256-dimensional embedding"
        }

        val file =
            File(
                context.filesDir,
                EMBEDDING_FILE
            )

        file.outputStream().use { output ->

            val buffer =
                java.nio.ByteBuffer
                    .allocate(256 * 4)
                    .order(
                        java.nio.ByteOrder.LITTLE_ENDIAN
                    )

            for (value in embedding) {
                buffer.putFloat(value)
            }

            output.write(buffer.array())
        }
    }

    fun loadEmbedding(
        context: Context
    ): FloatArray? {

        val file =
            File(
                context.filesDir,
                EMBEDDING_FILE
            )

        if (!file.exists()) {
            return null
        }

        val bytes =
            file.readBytes()

        if (bytes.size != 256 * 4) {
            return null
        }

        val buffer =
            java.nio.ByteBuffer
                .wrap(bytes)
                .order(
                    java.nio.ByteOrder.LITTLE_ENDIAN
                )

        val embedding =
            FloatArray(256)

        for (i in embedding.indices) {
            embedding[i] =
                buffer.float
        }

        return embedding
    }

    fun exists(
        context: Context
    ): Boolean {

        return File(
            context.filesDir,
            EMBEDDING_FILE
        ).exists()
    }
}