
package com.example.vga

import android.content.Context
import android.util.Log
import com.audeering.opensmile.CallbackExternalSink
import com.audeering.opensmile.OpenSmileAdapter
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class OpenSmileTest(private val context: Context) {

    private var smile: OpenSmileAdapter? = null
    @Volatile
    private var latestFeatures: FloatArray? = null

    @Volatile
    private var featuresReceived = false

    private fun copyAssetTree(
        assetPath: String,
        destination: File
    ) {
        val files = context.assets.list(assetPath)

        if (files.isNullOrEmpty()) {
            destination.parentFile?.mkdirs()

            context.assets.open(assetPath).use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return
        }

        destination.mkdirs()

        for (file in files) {
            val childAssetPath =
                if (assetPath.isEmpty()) {
                    file
                } else {
                    "$assetPath/$file"
                }

            copyAssetTree(
                childAssetPath,
                File(destination, file)
            )
        }
    }

    private fun copyOpenSmileConfigs() {

        copyAssetTree(
            "gemaps",
            File(context.cacheDir, "gemaps")
        )

        copyAssetTree(
            "egemaps",
            File(context.cacheDir, "egemaps")
        )

        copyAssetTree(
            "shared",
            File(context.cacheDir, "shared")
        )

        val mainConfig = File(
            context.cacheDir,
            "egemaps_test.conf"
        )

        context.assets.open("egemaps_test.conf").use { input ->
            mainConfig.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun initialize(): Boolean {

        return try {

            latestFeatures = null
            featuresReceived = false

            Log.d(
                "OpenSMILE_TEST",
                "Starting OpenSMILE initialization"
            )

            copyOpenSmileConfigs()

            val configFile = File(
                context.cacheDir,
                "egemaps_test.conf"
            )

            Log.d(
                "OpenSMILE_TEST",
                "Config path = ${configFile.absolutePath}"
            )

            Log.d(
                "OpenSMILE_TEST",
                "Config exists = ${configFile.exists()}"
            )

            smile = OpenSmileAdapter()

            val callback = object : CallbackExternalSink() {

                override fun onCalledExternalSinkCallback(
                    data: FloatArray?
                ): Boolean {

                    if (data != null) {
                        latestFeatures = data.copyOf()
                        featuresReceived = true
                    }

                    if (data == null) {

                        Log.d(
                            "OPENSMILE_FEATURES",
                            "Received null feature vector"
                        )

                        return true
                    }

                    Log.d(
                        "OPENSMILE_FEATURES",
                        "Received feature vector"
                    )

                    Log.d(
                        "OPENSMILE_FEATURES",
                        "Feature count = ${data.size}"
                    )

                    Log.d(
                        "OPENSMILE_FEATURES",
                        "Features = ${data.contentToString()}"
                    )

                    return true
                }
            }

            val result = smile!!.smile_initialize(
                configFile.absolutePath,
                HashMap(),
                3,
                1,
                1
            )

            Log.d(
                "OpenSMILE_TEST",
                "Initialize result = $result"
            )

            if (result.toString() != "SMILE_SUCCESS") {

                Log.e(
                    "OpenSMILE_TEST",
                    "OpenSMILE initialization failed"
                )

                return false
            }

            val callbackResult =
                smile!!.smile_extsink_set_data_callback(
                    "externalSink",
                    callback
                )

            Log.d(
                "OpenSMILE_TEST",
                "Callback registration result = $callbackResult"
            )

            if (callbackResult.toString() != "SMILE_SUCCESS") {

                Log.e(
                    "OpenSMILE_TEST",
                    "External sink callback registration failed"
                )

                smile!!.smile_free()
                smile = null

                return false
            }

            Log.d(
                "OpenSMILE_TEST",
                "OpenSMILE initialized successfully"
            )

            true

        } catch (e: Exception) {

            Log.e(
                "OpenSMILE_TEST",
                "Initialization exception",
                e
            )

            false
        }
    }

    fun start(): Boolean {

        return try {

            if (smile == null) {

                Log.e(
                    "OpenSMILE_TEST",
                    "Cannot start: OpenSMILE is not initialized"
                )

                return false
            }

            Thread {

                try {

                    Log.d(
                        "OpenSMILE_TEST",
                        "Starting OpenSMILE processing thread"
                    )

                    val result = smile!!.smile_run()

                    Log.d(
                        "OpenSMILE_TEST",
                        "smile_run result = $result"
                    )

                } catch (e: Exception) {

                    Log.e(
                        "OpenSMILE_TEST",
                        "OpenSMILE run failed",
                        e
                    )
                }

            }.start()

            true

        } catch (e: Exception) {

            Log.e(
                "OpenSMILE_TEST",
                "Start failed",
                e
            )

            false
        }
    }

    /**
     * Process a PCM WAV file.
     *
     * Expected format:
     *  - PCM
     *  - 16-bit
     *  - 16,000 Hz
     *  - mono
     */
    fun processWavFile(file: File): Boolean {

        return try {

            if (smile == null) {

                Log.e(
                    "OpenSMILE_TEST",
                    "Cannot process WAV: OpenSMILE is not initialized"
                )

                return false
            }

            if (!file.exists()) {

                Log.e(
                    "OpenSMILE_TEST",
                    "WAV file does not exist: ${file.absolutePath}"
                )

                return false
            }

            Log.d(
                "OpenSMILE_TEST",
                "Processing WAV file: ${file.absolutePath}"
            )

            RandomAccessFile(file, "r").use { wav ->

                val header = ByteArray(12)

                wav.readFully(header)

                val riff =
                    String(header, 0, 4, Charsets.US_ASCII)

                val wave =
                    String(header, 8, 4, Charsets.US_ASCII)

                if (riff != "RIFF" || wave != "WAVE") {

                    Log.e(
                        "OpenSMILE_TEST",
                        "Invalid WAV file"
                    )

                    return false
                }

                var sampleRate = 0
                var channels = 0
                var bitsPerSample = 0
                var audioFormat = 0
                var dataOffset = -1L
                var dataSize = 0L

                while (wav.filePointer < wav.length()) {

                    val chunkHeader = ByteArray(8)

                    if (wav.read(chunkHeader) != 8) {
                        break
                    }

                    val chunkId =
                        String(
                            chunkHeader,
                            0,
                            4,
                            Charsets.US_ASCII
                        )

                    val chunkSize =
                        readLittleEndianInt(chunkHeader, 4).toLong()

                    when (chunkId) {

                        "fmt " -> {

                            val fmt = ByteArray(chunkSize.toInt())

                            wav.readFully(fmt)

                            audioFormat =
                                readLittleEndianShort(fmt, 0)

                            channels =
                                readLittleEndianShort(fmt, 2)

                            sampleRate =
                                readLittleEndianInt(fmt, 4)

                            bitsPerSample =
                                readLittleEndianShort(fmt, 14)

                        }

                        "data" -> {

                            dataOffset =
                                wav.filePointer

                            dataSize =
                                chunkSize

                            break
                        }

                        else -> {

                            wav.seek(
                                wav.filePointer + chunkSize
                            )
                        }
                    }
                }

                Log.d(
                    "OpenSMILE_TEST",
                    "WAV format=$audioFormat channels=$channels sampleRate=$sampleRate bits=$bitsPerSample"
                )

                if (audioFormat != 1) {

                    Log.e(
                        "OpenSMILE_TEST",
                        "Unsupported WAV encoding. Expected PCM (1)."
                    )

                    return false
                }

                if (channels != 1) {

                    Log.e(
                        "OpenSMILE_TEST",
                        "WAV must be mono. Found $channels channels."
                    )

                    return false
                }

                if (sampleRate != 16000) {

                    Log.e(
                        "OpenSMILE_TEST",
                        "WAV must be 16000 Hz. Found $sampleRate Hz."
                    )

                    return false
                }

                if (bitsPerSample != 16) {

                    Log.e(
                        "OpenSMILE_TEST",
                        "WAV must be 16-bit. Found $bitsPerSample-bit."
                    )

                    return false
                }

                if (dataOffset < 0 || dataSize <= 0) {

                    Log.e(
                        "OpenSMILE_TEST",
                        "WAV contains no audio data"
                    )

                    return false
                }

                wav.seek(dataOffset)

                val bufferSize = 3200
                val buffer = ByteArray(bufferSize)

                var remaining = dataSize
                var totalSent = 0L

                while (remaining > 0) {

                    val requested =
                        minOf(
                            buffer.size.toLong(),
                            remaining
                        ).toInt()

                    val bytesRead =
                        wav.read(buffer, 0, requested)

                    if (bytesRead <= 0) {
                        break
                    }

                    val pcm =
                        if (bytesRead == buffer.size) {
                            buffer
                        } else {
                            buffer.copyOf(bytesRead)
                        }

                    val result =
                        smile!!.smile_extaudiosource_write_data(
                            "externalAudioSource",
                            pcm
                        )

                    if (result.toString() != "SMILE_SUCCESS") {

                        Log.e(
                            "OpenSMILE_TEST",
                            "Audio write failed: $result"
                        )

                        return false
                    }

                    totalSent += bytesRead

                    remaining -= bytesRead
                }

                Log.d(
                    "OpenSMILE_TEST",
                    "PCM bytes sent = $totalSent"
                )
            }

            val eoiResult =
                smile!!.smile_extaudiosource_set_external_eoi(
                    "externalAudioSource"
                )

            Log.d(
                "OpenSMILE_TEST",
                "EOI result = $eoiResult"
            )

            eoiResult.toString() == "SMILE_SUCCESS"

        } catch (e: Exception) {

            Log.e(
                "OpenSMILE_TEST",
                "WAV processing failed",
                e
            )

            false
        }
    }

    private fun readLittleEndianShort(
        data: ByteArray,
        offset: Int
    ): Int {

        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readLittleEndianInt(
        data: ByteArray,
        offset: Int
    ): Int {

        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    fun writePcm16(
        audio: ByteArray
    ): Boolean {

        val currentSmile = smile

        if (currentSmile == null) {
            Log.e(
                "OpenSMILE_TEST",
                "Cannot write PCM: OpenSMILE is not initialized"
            )
            return false
        }

        return try {

            var attempts = 0
            val maxAttempts = 500

            while (attempts < maxAttempts) {

                val result =
                    currentSmile.smile_extaudiosource_write_data(
                        "externalAudioSource",
                        audio
                    )

                if (result.toString() == "SMILE_SUCCESS") {
                    return true
                }

                if (result.toString() == "SMILE_NOT_WRITTEN") {

                    attempts++

                    // Give OpenSMILE processing thread time
                    // to consume data from the external source.
                    Thread.sleep(10)

                    continue
                }

                Log.e(
                    "OpenSMILE_TEST",
                    "PCM write failed: $result"
                )

                return false
            }

            Log.e(
                "OpenSMILE_TEST",
                "PCM write timed out after $maxAttempts attempts"
            )

            false

        } catch (e: Exception) {

            Log.e(
                "OpenSMILE_TEST",
                "PCM write failed",
                e
            )

            false
        }
    }

    fun endOfInput(): String {

        return try {

            val currentSmile = smile

            if (currentSmile == null) {

                Log.e(
                    "OpenSMILE_TEST",
                    "Cannot send EOI: OpenSMILE is not initialized"
                )

                return "NOT_INITIALIZED"
            }

            val result =
                currentSmile.smile_extaudiosource_set_external_eoi(
                    "externalAudioSource"
                )

            Log.d(
                "OpenSMILE_TEST",
                "EOI result = $result"
            )

            result.toString()

        } catch (e: Exception) {

            Log.e(
                "OpenSMILE_TEST",
                "EOI failed",
                e
            )

            "EXCEPTION"
        }
    }

    fun waitForFeatures(
        timeoutMs: Long = 5000
    ): FloatArray? {

        val start =
            System.currentTimeMillis()

        while (
            !featuresReceived &&
            System.currentTimeMillis() - start < timeoutMs
        ) {

            Thread.sleep(50)
        }

        val result =
            latestFeatures

        if (result == null) {

            Log.e(
                "OPENSMILE_FEATURES",
                "Timed out waiting for feature vector"
            )

            return null
        }

        Log.d(
            "OPENSMILE_FEATURES",
            "Returning ${result.size} features"
        )

        return result.copyOf()
    }

    fun close() {

        try {

            smile?.smile_abort()

            smile?.smile_free()

            smile = null

            Log.d(
                "OpenSMILE_TEST",
                "OpenSMILE closed"
            )

        } catch (e: Exception) {

            Log.e(
                "OpenSMILE_TEST",
                "Close failed",
                e
            )
        }
    }
}
