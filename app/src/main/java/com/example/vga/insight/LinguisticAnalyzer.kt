package com.example.vga.insight

import android.content.Context
import android.util.Log
import com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingModule
import com.example.vga.dementia.acoustic.AcousticFeatureStore
import com.example.vga.dementia.acoustic.EgemapsFeatures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.math.roundToInt


/**
 * Orchestrates the multimodal analysis:
 *
 *   transcript (IndicWhisper)
 * + acoustic features (openSMILE eGeMAPSv02)
 * + keyboard behaviour (keyboard-processing daily summary)
 * + cognitive-test results
 * + personal historical baseline (previous transcripts)
 *      -> local AI server -> pattern-based insight
 *
 * Every input is read from the existing stores. Anything unavailable is simply
 * omitted from the prompt and reported as unavailable - never fabricated.
 */
object LinguisticAnalyzer {

    private const val TAG = "VGA_INSIGHT"

    private val SYSTEM_PROMPT = """
        You are a careful screening assistant that reviews a transcript of a
        person's speech for LINGUISTIC PATTERNS ONLY.

        You must NEVER state or imply a medical diagnosis. Specifically you must
        never say the person has Alzheimer's, Lewy body dementia, frontotemporal
        dementia, vascular dementia, Parkinson's, MCI, or any other named
        condition. Never write "you have", "diagnosed with", or "suffers from".

        Use only observational, non-diagnostic language such as:
        "pattern requiring attention", "observed linguistic change",
        "increased repetition compared with baseline",
        "possible cognitive concern detected",
        "further professional evaluation may be appropriate".

        Look for these patterns, and report ONLY those actually evidenced by the
        transcript text provided:
        - word-finding difficulty
        - repetition of words, questions or information
        - reduced vocabulary diversity
        - increased use of vague or general words
        - fragmented or unusually simplified sentences
        - loss of conversational coherence
        - topic drifting
        - semantic or word substitutions

        If the transcript is too short or too sparse to judge, say so plainly
        and report no patterns. Do not speculate. Do not invent evidence.

        Reply with STRICT JSON only, no markdown fence, in exactly this shape:
        {
          "patterns": [
            {"label": "...", "strength": "low|moderate|high", "evidence": "short quote or observation"}
          ],
          "confidence": "low|moderate|high",
          "explanation": "2-4 plain sentences a non-clinician can understand"
        }
    """.trimIndent()


    /**
     * Runs the full analysis. Returns [Result.failure] with the real error when
     * the server cannot be reached or the transcript is unusable - the caller
     * shows that message and no insight is stored.
     */
    suspend fun analyze(
        context: Context,
        transcript: TranscriptRecord
    ): Result<LinguisticInsight> {

        if (transcript.text.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Transcript is empty - nothing to analyse.")
            )
        }

        // ----------------------------------------------------
        // MINIMUM-CONTENT GATE
        //
        // The recogniser emits placeholders such as "[SOMETHING]" when it
        // hears sound but no intelligible speech. Measuring those as
        // vocabulary produced high-confidence "patterns" from what was
        // effectively an empty transcript, so analysis is refused here
        // instead. The system prompt asks the model to do the same, but a
        // prompt is not a guarantee - this is the hard backstop.
        // ----------------------------------------------------

        val spokenText = TranscriptAnalytics.stripPlaceholders(transcript.text)

        val metrics = TranscriptAnalytics.compute(spokenText)

        if (metrics.wordCount < TranscriptAnalytics.MIN_WORDS_FOR_ANALYSIS) {
            return Result.failure(
                IllegalArgumentException(
                    "Transcript has only ${metrics.wordCount} intelligible word" +
                        (if (metrics.wordCount == 1) "" else "s") +
                        " (minimum ${TranscriptAnalytics.MIN_WORDS_FOR_ANALYSIS}). " +
                        "The recording did not contain enough clear speech to analyse."
                )
            )
        }

        // ----------------------------------------------------
        // GATHER REAL INPUTS
        // ----------------------------------------------------

        val priorTranscripts = TranscriptStore.getAll(context)
            .filter { it.timestampMs != transcript.timestampMs }

        val priorMetrics = priorTranscripts.map {
            TranscriptAnalytics.compute(TranscriptAnalytics.stripPlaceholders(it.text))
        }

        val baselineComparison =
            TranscriptAnalytics.compareWithBaseline(metrics, priorMetrics)

        val acoustic = AcousticFeatureStore.get(context)

        val keyboard = readKeyboardSnapshot(context)

        val cognitive = CognitiveResultStore.get(context)

        // ----------------------------------------------------
        // CALL LOCAL AI SERVER
        // ----------------------------------------------------

        val client = LocalAiClient(AiServerConfig.baseUrl(context))

        // The server matches model ids case-sensitively, so use the exact id
        // it reports rather than whatever casing happens to be configured.
        val model = client.resolveModelId(AiServerConfig.model(context))

        val userPrompt = buildPrompt(
            transcript = transcript.copy(text = spokenText),
            metrics = metrics,
            baselineComparison = baselineComparison,
            acoustic = acoustic,
            keyboard = keyboard,
            cognitive = cognitive
        )

        Log.d(TAG, "Sending transcript (${metrics.wordCount} words) to $model")

        val response = client.chat(
            model = model,
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = userPrompt
        )

        val rawContent = response.getOrElse { error ->
            return Result.failure(error)
        }

        Log.d(TAG, "Model responded with ${rawContent.length} chars")

        // ----------------------------------------------------
        // PARSE + GUARD
        // ----------------------------------------------------

        val parsed = parseModelJson(rawContent)

        var guardApplied = false

        val safePatterns = parsed.patterns.map { pattern ->
            val label = DiagnosisGuard.sanitize(pattern.label)
            val evidence = DiagnosisGuard.sanitize(pattern.evidence)
            if (label.guardApplied || evidence.guardApplied) guardApplied = true
            pattern.copy(label = label.text, evidence = evidence.text)
        }

        val safeExplanation = DiagnosisGuard.sanitizeExplanation(parsed.explanation)
        if (safeExplanation.guardApplied) guardApplied = true

        // ----------------------------------------------------
        // SUPPORTING METRICS (measured, not model-generated)
        // ----------------------------------------------------

        val supporting = buildList {
            addAll(metrics.asSupportingMetrics())

            keyboard?.let {
                add(
                    SupportingMetric(
                        label = "Typing speed",
                        value = "${it.avgTypingSpeedCpm.roundToInt()} cpm",
                        source = "Keyboard"
                    )
                )
                add(
                    SupportingMetric(
                        label = "Keyboard risk contribution",
                        value = "${it.riskContributionScore.roundToInt()} / 100",
                        source = "Keyboard"
                    )
                )
            }

            acoustic?.let {
                add(
                    SupportingMetric(
                        label = "Acoustic features",
                        value = "${it.size} eGeMAPSv02 values",
                        source = "openSMILE"
                    )
                )
            }

            cognitive?.let { snapshot ->
                snapshot.stroopAccuracyPercent?.let {
                    add(
                        SupportingMetric(
                            label = "Stroop accuracy",
                            value = "${it.roundToInt()}%",
                            source = "Cognitive tests"
                        )
                    )
                }
                snapshot.digitSpanForward?.let {
                    add(
                        SupportingMetric(
                            label = "Digit span (forward)",
                            value = it.toString(),
                            source = "Cognitive tests"
                        )
                    )
                }
            }
        }

        val insight = LinguisticInsight(
            timestampMs = System.currentTimeMillis(),
            sourceFileName = transcript.sourceFileName,
            patterns = safePatterns,
            supportingMetrics = supporting,
            baselineComparison =
                baselineComparison ?: "No previous transcript yet - this becomes your baseline.",
            confidence = parsed.confidence,
            explanation = safeExplanation.text,
            transcriptWordCount = metrics.wordCount,
            modelName = model,
            guardApplied = guardApplied
        )

        InsightStore.save(context, insight)

        return Result.success(insight)
    }


    // ========================================================
    // KEYBOARD SNAPSHOT
    // ========================================================

    private suspend fun readKeyboardSnapshot(context: Context): KeyboardSnapshot? {

        return runCatching {

            val api = KeyboardProcessingModule.getInstance(context)

            val summary = withTimeoutOrNull(3_000) {
                api.observeDailyBehavioralSummary().first()
            } ?: return null

            if (summary.sessionCount == 0) return null

            KeyboardSnapshot(
                avgTypingSpeedCpm = summary.avgTypingSpeedCpm,
                avgBackspaceRate = summary.avgBackspaceRate,
                pauseFrequencyPer100Chars = summary.pauseFrequencyPer100Chars,
                sessionCount = summary.sessionCount,
                riskContributionScore = summary.riskContributionScore
            )

        }.getOrElse {
            Log.e(TAG, "Keyboard snapshot unavailable: ${it.message}")
            null
        }
    }


    // ========================================================
    // PROMPT
    // ========================================================

    private fun buildPrompt(
        transcript: TranscriptRecord,
        metrics: TranscriptMetrics,
        baselineComparison: String?,
        acoustic: FloatArray?,
        keyboard: KeyboardSnapshot?,
        cognitive: CognitiveSnapshot?
    ): String {

        val builder = StringBuilder()

        builder.appendLine("TRANSCRIPT (verbatim, from on-device speech recognition):")
        builder.appendLine("\"\"\"")
        builder.appendLine(transcript.text.take(6000))
        builder.appendLine("\"\"\"")
        builder.appendLine()

        builder.appendLine("MEASURED TRANSCRIPT METRICS (computed locally, treat as ground truth):")
        builder.appendLine("- Words: ${metrics.wordCount}")
        builder.appendLine("- Distinct words: ${metrics.distinctWordCount}")
        builder.appendLine(
            "- Vocabulary diversity (type-token ratio): " +
                "${(metrics.typeTokenRatio * 100).roundToInt()}%"
        )
        builder.appendLine(
            "- Mean sentence length: ${metrics.meanSentenceLengthWords.roundToInt()} words"
        )
        builder.appendLine("- Immediate word repetitions: ${metrics.immediateRepetitionCount}")
        builder.appendLine("- Repeated 3-word phrases: ${metrics.repeatedPhraseCount}")
        builder.appendLine(
            "- Vague-term rate: " +
                "${(metrics.vagueTermRatePer100Words * 10).roundToInt() / 10.0} per 100 words"
        )
        builder.appendLine()

        if (baselineComparison != null) {
            builder.appendLine("PERSONAL BASELINE COMPARISON:")
            builder.appendLine(baselineComparison)
            builder.appendLine()
        } else {
            builder.appendLine("PERSONAL BASELINE: none yet (this is the first stored transcript).")
            builder.appendLine()
        }

        if (keyboard != null) {
            builder.appendLine("KEYBOARD BEHAVIOUR (today, from typing sessions):")
            builder.appendLine("- Sessions: ${keyboard.sessionCount}")
            builder.appendLine("- Typing speed: ${keyboard.avgTypingSpeedCpm.roundToInt()} cpm")
            builder.appendLine(
                "- Backspace rate: ${(keyboard.avgBackspaceRate * 100).roundToInt()}%"
            )
            builder.appendLine(
                "- Pause frequency: " +
                    "${(keyboard.pauseFrequencyPer100Chars * 10).roundToInt() / 10.0} per 100 chars"
            )
            builder.appendLine()
        }

        if (cognitive != null) {
            builder.appendLine("COGNITIVE TEST RESULTS:")
            cognitive.stroopAccuracyPercent?.let {
                builder.appendLine("- Stroop accuracy: ${it.roundToInt()}%")
            }
            cognitive.stroopAvgResponseMs?.let {
                builder.appendLine("- Stroop average response: ${it} ms")
            }
            cognitive.digitSpanForward?.let {
                builder.appendLine("- Digit span forward: $it")
            }
            cognitive.digitSpanBackward?.let {
                builder.appendLine("- Digit span backward: $it")
            }
            cognitive.trailMakingPartAMs?.let {
                builder.appendLine("- Trail making part A: ${it / 1000.0} s")
            }
            cognitive.trailMakingPartBMs?.let {
                builder.appendLine("- Trail making part B: ${it / 1000.0} s")
            }
            builder.appendLine()
        }

        if (acoustic != null && acoustic.size == EgemapsFeatures.names.size) {
            builder.appendLine("SELECTED ACOUSTIC FEATURES (openSMILE eGeMAPSv02):")
            // A readable subset - sending all 88 raw floats adds noise for a
            // small local model without adding interpretive value.
            val interesting = listOf(
                "F0semitoneFrom27.5Hz_sma3nz_amean",
                "loudness_sma3_amean",
                "jitterLocal_sma3nz_amean",
                "shimmerLocaldB_sma3nz_amean",
                "HNRdBACF_sma3nz_amean",
                "VoicedSegmentsPerSec",
                "MeanVoicedSegmentLengthSec",
                "MeanUnvoicedSegmentLength"
            )
            interesting.forEach { name ->
                val index = EgemapsFeatures.names.indexOf(name)
                if (index >= 0) {
                    builder.appendLine("- $name: ${acoustic[index]}")
                }
            }
            builder.appendLine()
        }

        builder.appendLine(
            "Analyse ONLY the transcript for linguistic patterns, using the other " +
                "signals as context. Reply with the strict JSON described in your instructions."
        )

        return builder.toString()
    }


    // ========================================================
    // MODEL RESPONSE PARSING
    // ========================================================

    data class ParsedResponse(
        val patterns: List<DetectedPattern>,
        val confidence: String,
        val explanation: String
    )

    /**
     * Parses the model's JSON reply. Small local models often wrap JSON in
     * prose or a code fence, so the first balanced object is extracted. If no
     * JSON can be found the raw text becomes the explanation and NO patterns
     * are reported - the app never invents findings.
     */
    fun parseModelJson(raw: String): ParsedResponse {

        val jsonText = extractJsonObject(raw)

        if (jsonText == null) {
            return ParsedResponse(
                patterns = emptyList(),
                confidence = "unknown",
                explanation = raw.trim()
            )
        }

        return runCatching {

            val root = JSONObject(jsonText)

            val patternArray = root.optJSONArray("patterns")

            val patterns = if (patternArray == null) {
                emptyList()
            } else {
                (0 until patternArray.length()).mapNotNull { index ->
                    patternArray.optJSONObject(index)?.let { item ->
                        val label = item.optString("label").trim()
                        if (label.isBlank()) null
                        else DetectedPattern(
                            label = label,
                            strength = item.optString("strength", "unknown")
                                .ifBlank { "unknown" },
                            evidence = item.optString("evidence").trim()
                        )
                    }
                }
            }

            ParsedResponse(
                patterns = patterns,
                confidence = root.optString("confidence", "unknown").ifBlank { "unknown" },
                explanation = root.optString("explanation").trim().ifBlank { raw.trim() }
            )

        }.getOrElse {
            ParsedResponse(
                patterns = emptyList(),
                confidence = "unknown",
                explanation = raw.trim()
            )
        }
    }

    /** Returns the first balanced `{...}` block, or null. */
    private fun extractJsonObject(raw: String): String? {

        val start = raw.indexOf('{')
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false

        for (index in start until raw.length) {
            val char = raw[index]

            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, index + 1)
                }
            }
        }

        return null
    }
}
