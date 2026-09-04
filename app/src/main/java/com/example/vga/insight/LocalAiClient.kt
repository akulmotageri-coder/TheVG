package com.example.vga.insight

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


/**
 * Persisted connection settings for the local AI server. The address is
 * editable because the server runs on the device's own Wi-Fi IP, which DHCP
 * can change between sessions.
 */
object AiServerConfig {

    private const val PREFS = "vga_local_ai"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_MODEL = "model"

    /**
     * The server runs on the device itself and listens on all interfaces.
     * 192.0.0.2 is the device's own 464XLAT/CLAT address (RFC 7335) on the
     * mobile-data interface, so this resolves locally and never leaves the
     * device.
     */
    const val DEFAULT_BASE_URL = "http://192.0.0.2:8080"

    /**
     * The server matches model ids case-sensitively and rejects a
     * differently-cased name with "Model not found or not initialized", so the
     * casing here matters. [LocalAiClient.resolveModelId] additionally
     * reconciles this against the server's own /v1/models at call time.
     */
    const val DEFAULT_MODEL = "Qwen2.5-1.5B-Instruct"

    fun baseUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    fun model(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL

    fun setBaseUrl(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, value.trim().trimEnd('/'))
            .apply()
    }

    fun setModel(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL, value.trim())
            .apply()
    }
}


/**
 * Minimal OpenAI-compatible client for the on-device server
 * (`/v1/models`, `/v1/chat/completions`).
 *
 * Uses JDK [HttpURLConnection] and `org.json` so no new dependency is added.
 * Failures are returned as [Result.failure] with the real reason - the caller
 * surfaces that verbatim and never substitutes placeholder analysis.
 */
class LocalAiClient(
    private val baseUrl: String,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 180_000
) {

    companion object {
        private const val TAG = "VGA_LOCAL_AI"
    }

    /** GET /v1/models - used as the connectivity check. */
    suspend fun listModels(): Result<List<String>> = withContext(Dispatchers.IO) {

        runCatching {

            val connection =
                (URL("$baseUrl/v1/models").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = connectTimeoutMs
                    readTimeout = 30_000
                }

            try {
                val code = connection.responseCode
                val body = connection.readBody()

                if (code !in 200..299) {
                    error("Server returned HTTP $code: ${body.take(300)}")
                }

                val data = JSONObject(body).optJSONArray("data") ?: JSONArray()

                (0 until data.length()).mapNotNull { index ->
                    data.optJSONObject(index)?.optString("id")?.takeIf { it.isNotBlank() }
                }

            } finally {
                connection.disconnect()
            }
        }.onFailure {
            Log.e(TAG, "listModels failed: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    /**
     * Resolves the exact model id the server expects.
     *
     * The server matches model ids case-sensitively and replies "Model not
     * found or not initialized" otherwise, so a configured name that differs
     * only in case is corrected here against the server's own /v1/models.
     * Falls back to [preferred] when the list cannot be read.
     */
    suspend fun resolveModelId(preferred: String): String {

        val models = listModels().getOrNull().orEmpty()

        if (models.isEmpty()) return preferred

        models.firstOrNull { it == preferred }?.let { return it }

        models.firstOrNull { it.equals(preferred, ignoreCase = true) }?.let { return it }

        return models.first()
    }

    /**
     * POST /v1/chat/completions (non-streaming) and return the assistant
     * message content.
     */
    suspend fun chat(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.2
    ): Result<String> = withContext(Dispatchers.IO) {

        runCatching {

            val payload = JSONObject().apply {
                put("model", model)
                put("temperature", temperature)
                put("stream", false)
                put(
                    "messages",
                    JSONArray().apply {
                        put(
                            JSONObject()
                                .put("role", "system")
                                .put("content", systemPrompt)
                        )
                        put(
                            JSONObject()
                                .put("role", "user")
                                .put("content", userPrompt)
                        )
                    }
                )
            }

            val connection =
                (URL("$baseUrl/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

            try {
                connection.outputStream.use { stream ->
                    stream.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val body = connection.readBody()

                // The server can report a problem (e.g. an unknown model id)
                // in an "error" field; surface that verbatim rather than
                // failing later with a confusing parse error.
                extractServerError(body)?.let { serverError ->
                    error("Server error: $serverError")
                }

                if (code !in 200..299) {
                    error("Server returned HTTP $code: ${body.take(300)}")
                }

                parseChatContent(body)

            } finally {
                connection.disconnect()
            }
        }.onFailure {
            Log.e(TAG, "chat failed: ${it.javaClass.simpleName}: ${it.message}")
        }
    }

    private fun HttpURLConnection.readBody(): String {
        val stream = try {
            if (responseCode in 200..299) inputStream else errorStream
        } catch (e: Exception) {
            errorStream
        } ?: return ""

        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}


/**
 * Returns the server's error message when the body carries one, else null.
 *
 * Handles both `{"error": "text"}` and `{"error": {"message": "text"}}`.
 */
fun extractServerError(responseBody: String): String? {

    if (responseBody.isBlank()) return null

    return runCatching {
        val root = JSONObject(responseBody)

        when {
            !root.has("error") -> null

            root.optJSONObject("error") != null ->
                root.getJSONObject("error").optString("message")
                    .takeIf { it.isNotBlank() }

            else -> root.optString("error").takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}


/**
 * Extracts `choices[0].message.content` from an OpenAI-compatible response.
 *
 * Kept top-level and pure so it can be unit-tested on the JVM without a
 * server or an Android runtime.
 */
fun parseChatContent(responseBody: String): String {

    val root = JSONObject(responseBody)

    val choices = root.optJSONArray("choices")
        ?: error("Response contained no 'choices' array")

    if (choices.length() == 0) {
        error("Response contained an empty 'choices' array")
    }

    val first = choices.optJSONObject(0)
        ?: error("Response 'choices[0]' was not an object")

    // Standard chat-completions shape.
    first.optJSONObject("message")?.optString("content")
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    // Some servers echo the legacy completion shape.
    first.optString("text").takeIf { it.isNotBlank() }?.let { return it }

    error("Response contained no assistant message content")
}
