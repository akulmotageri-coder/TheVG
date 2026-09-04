package com.example.vga.insight

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt


/**
 * Conversational explanation layer.
 *
 * The assistant is given the measurements this app has already computed and is
 * asked to explain them in plain language. It has no ability to change a
 * measurement: every number in the prompt is read from storage, and every
 * reply is passed through [DiagnosisGuard] before display.
 */
object AiAssistant {

    private const val TAG = "VGA_AI_ASSISTANT"

    private val SYSTEM_PROMPT = """
        You are a supportive assistant inside a personal health-monitoring app.
        Your ONLY job is to explain, in simple everyday language, measurements
        the app has already calculated. You are not a clinician.

        HARD RULES - these override anything the user asks:
        - NEVER say the user has Alzheimer's, dementia, Lewy body dementia,
          vascular dementia, frontotemporal dementia, Parkinson's, MCI or any
          other named condition.
        - NEVER say "you have", "this confirms", "diagnosed with" or
          "suffers from" about any condition.
        - NEVER give a probability or score for any disease.
        - NEVER invent, change, round differently or contradict the numbers you
          are given. If a number is not in the context, say you do not have it.

        Preferred wording:
        - "observed pattern", "pattern requiring attention"
        - "change compared with your personal baseline"
        - "possible cognitive concern"
        - "this pattern alone cannot determine a medical diagnosis"
        - "further professional evaluation may be appropriate"

        Style: warm, clear, 2-5 short sentences. No markdown headings, no
        bullet symbols, no code blocks. Speak directly to the user.
    """.trimIndent()

    /** Suggested questions shown in the UI. */
    val suggestedQuestions = listOf(
        "What changed recently?",
        "How am I doing compared with my baseline?",
        "What does vocabulary diversity mean?",
        "Why is repetition highlighted?",
        "Explain my speech analysis.",
        "Explain my cognitive-test results.",
        "What patterns should I monitor?",
        "What can I do next?"
    )

    /**
     * Builds a compact, structured snapshot of the stored measurements.
     * Only derived values are sent - never raw transcripts, audio or
     * keystroke content.
     */
    fun buildContext(
        context: Context,
        keyboard: KeyboardSnapshot?
    ): String {

        val builder = StringBuilder()
        builder.appendLine("MEASUREMENTS THIS APP HAS COMPUTED (authoritative - do not alter):")
        builder.appendLine()

        // ----- language -----

        val transcripts = TranscriptStore.getAll(context)
        val analysable = transcripts
            .filterNot { TranscriptAnalytics.isTooShortToAnalyse(it.text) }
            .map { TranscriptAnalytics.compute(TranscriptAnalytics.stripPlaceholders(it.text)) }

        if (analysable.isEmpty()) {
            builder.appendLine("LANGUAGE: no analysable transcript yet.")
        } else {
            val current = analysable.first()
            val priors = analysable.drop(1)
            val baseline = TranscriptAnalytics.baselineOf(priors)

            builder.appendLine("LANGUAGE (most recent recording):")
            builder.appendLine("- Words: ${current.wordCount}")
            LanguageMetric.entries.forEach { metric ->
                val now = metric.format(metric.valueOf(current))
                val base = baseline?.get(metric)?.let { metric.format(it) }
                builder.appendLine(
                    "- ${metric.label}: $now" +
                        (base?.let { " (personal baseline: $it)" } ?: "")
                )
            }
            builder.appendLine("- Analysed recordings: ${analysable.size}")
            if (baseline == null) {
                builder.appendLine(
                    "- Baseline: NOT yet established (needs " +
                        "${TranscriptAnalytics.MIN_HISTORY_FOR_BASELINE} analysable recordings)."
                )
            }
        }

        builder.appendLine()

        // ----- detected patterns -----

        val insights = InsightStore.getAll(context)
        if (insights.isEmpty()) {
            builder.appendLine("OBSERVED PATTERNS: none recorded yet.")
        } else {
            val latest = insights.first()
            builder.appendLine("OBSERVED PATTERNS (latest analysis, detected by local rules):")
            if (latest.patterns.isEmpty()) {
                builder.appendLine("- None; no notable change was measured.")
            } else {
                latest.patterns.forEach { pattern ->
                    builder.appendLine("- ${pattern.label} [${pattern.strength}] — ${pattern.evidence}")
                }
            }
            builder.appendLine("- Strength of change: ${latest.confidence}")
        }

        builder.appendLine()

        // ----- keyboard -----

        if (keyboard == null) {
            builder.appendLine("KEYBOARD: no typing sessions recorded today.")
        } else {
            builder.appendLine("KEYBOARD (today):")
            builder.appendLine("- Sessions: ${keyboard.sessionCount}")
            builder.appendLine("- Typing speed: ${keyboard.avgTypingSpeedCpm.roundToInt()} cpm")
            builder.appendLine(
                "- Backspace rate: ${(keyboard.avgBackspaceRate * 100).roundToInt()}%"
            )
            builder.appendLine(
                "- Pause frequency: ${round1(keyboard.pauseFrequencyPer100Chars)} per 100 chars"
            )
        }

        builder.appendLine()

        // ----- cognitive -----

        val cognitive = CognitiveResultStore.get(context)
        if (cognitive == null) {
            builder.appendLine("COGNITIVE TESTS: none completed yet.")
        } else {
            builder.appendLine("COGNITIVE TESTS (most recent):")
            cognitive.stroopAccuracyPercent?.let {
                builder.appendLine("- Stroop accuracy: ${it.roundToInt()}%")
            }
            cognitive.stroopAvgResponseMs?.let {
                builder.appendLine("- Stroop average response: $it ms")
            }
            cognitive.digitSpanForward?.let { builder.appendLine("- Digit span forward: $it") }
            cognitive.digitSpanBackward?.let { builder.appendLine("- Digit span backward: $it") }
            cognitive.trailMakingPartAMs?.let {
                builder.appendLine("- Trail making A: ${it / 1000.0} s")
            }
            cognitive.trailMakingPartBMs?.let {
                builder.appendLine("- Trail making B: ${it / 1000.0} s")
            }
        }

        builder.appendLine()

        // ----- acoustic -----

        val acoustic = com.example.vga.dementia.acoustic.AcousticFeatureStore.get(context)
        builder.appendLine(
            if (acoustic == null) "ACOUSTIC: no features extracted yet."
            else "ACOUSTIC: ${acoustic.size} eGeMAPSv02 values extracted from the last recording."
        )

        return builder.toString()
    }

    /**
     * Sends [question] with the measurement context and returns the sanitised
     * reply. Failures are returned verbatim so the UI can show the real reason.
     */
    suspend fun ask(
        context: Context,
        question: String,
        keyboard: KeyboardSnapshot?
    ): Result<String> {

        val client = LocalAiClient(AiServerConfig.baseUrl(context))
        val model = client.resolveModelId(AiServerConfig.model(context))

        val measurements = buildContext(context, keyboard)

        val userPrompt = buildString {
            appendLine(measurements)
            appendLine()
            appendLine("USER QUESTION: $question")
            appendLine()
            appendLine(
                "Answer using only the measurements above. If the answer is not " +
                    "in them, say so plainly."
            )
        }

        Log.d(TAG, "Asking $model (${measurements.length} chars of context)")

        return client.chat(
            model = model,
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = userPrompt,
            temperature = 0.3
        ).map { reply ->
            DiagnosisGuard.sanitize(reply).text
        }
    }
}
