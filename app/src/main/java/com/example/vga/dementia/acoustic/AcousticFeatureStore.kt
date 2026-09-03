package com.example.vga.dementia.acoustic

import android.content.Context
import org.json.JSONArray
import java.io.File

object AcousticFeatureStore {

    private const val FILE_NAME = "acoustic_features.json"

    // --------------------------------------------------------
    // SAVE FEATURES
    // --------------------------------------------------------

    fun save(
        context: Context,
        features: FloatArray
    ) {

        try {

            val jsonArray = JSONArray()

            features.forEach { value ->
                jsonArray.put(value.toDouble())
            }

            val file = File(
                context.filesDir,
                FILE_NAME
            )

            file.writeText(
                jsonArray.toString()
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // --------------------------------------------------------
    // LOAD FEATURES
    // --------------------------------------------------------

    fun get(
        context: Context
    ): FloatArray? {

        return try {

            val file = File(
                context.filesDir,
                FILE_NAME
            )

            if (!file.exists()) {
                return null
            }

            val jsonArray =
                JSONArray(
                    file.readText()
                )

            FloatArray(
                jsonArray.length()
            ) { index ->
                jsonArray
                    .getDouble(index)
                    .toFloat()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }

    // --------------------------------------------------------
    // CHECK IF FEATURES EXIST
    // --------------------------------------------------------

    fun exists(
        context: Context
    ): Boolean {

        return File(
            context.filesDir,
            FILE_NAME
        ).exists()
    }

    // --------------------------------------------------------
    // DELETE FEATURES
    // --------------------------------------------------------

    fun clear(
        context: Context
    ) {

        try {

            val file = File(
                context.filesDir,
                FILE_NAME
            )

            if (file.exists()) {
                file.delete()
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}