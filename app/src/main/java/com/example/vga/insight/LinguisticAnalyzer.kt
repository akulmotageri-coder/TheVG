package com.example.vga.insight

import android.content.Context
import android.util.Log
import com.example.vernacularguardian.keyboardprocessing.KeyboardProcessingModule
import com.example.vga.dementia.acoustic.AcousticFeatureStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt


/**
 * Runs the local, deterministic analysis of a transcript.
 *
 *   RAW TRANSCRIPT
 *   -> local NLP feature extraction   (TranscriptAnalytics)
 *   -> personal baseline comparison   (TranscriptAnalytics.baselineOf)
 *   -> pattern detection              (PatternDetector)
 *   -> stored insight -> charts / timeline
 *
 * No AI is involved at any point here. The AI assistant reads the stored
 * result afterwards to explain it, and cannot change any measured value.
 */
object LinguisticAnalyzer {

    private const val TAG = "VGA_INSIGHT"

    /**
     * Analyses [transcript] and stores the result.
     *
     * Returns [Result.failure] with a real reason when the transcript cannot
     * support analysis; nothing is stored in that case.
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
        // vocabulary produced findings from an effectively empty transcript,
        // so analysis is refused here instead.
        // ----------------------------------------------------

        val spokenText = TranscriptAnalytics.stripPlaceholders(transcript.text)
        val metrics = TranscriptAnalytics.compute(spokenText)

        if (TranscriptAnalytics.isDegenerateOutput(transcript.text)) {
            return Result.failure(
                IllegalArgumentException(
                    "The transcript is a speech-recognition loop (one word repeated), " +
                        "not usable speech. This usually means the audio was too quiet " +
                        "or too noisy to transcribe."
                )
            )
        }

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
        // PERSONAL BASELINE (real history only)
        // ----------------------------------------------------

        val priorMetrics = TranscriptStore.getAll(context)
            .filter { it.sourceFileName != transcript.sourceFileName }
            .filterNot { TranscriptAnalytics.isTooShortToAnalyse(it.text) }
            .map { TranscriptAnalytics.compute(TranscriptAnalytics.stripPlaceholders(it.text)) }

        val baseline = TranscriptAnalytics.baselineOf(priorMetrics)

        // ----------------------------------------------------
        // DETERMINISTIC PATTERN DETECTION
        // ----------------------------------------------------

        val patterns = PatternDetector.detect(metrics, baseline)

        val baselineComparison = describeBaseline(metrics, baseline, priorMetrics.size)

        // ----------------------------------------------------
        // SUPPORTING METRICS FROM OTHER MODALITIES
        // ----------------------------------------------------

        val supporting = buildList {
            addAll(metrics.asSupportingMetrics())

            readKeyboardSnapshot(context)?.let {
                add(
                    SupportingMetric(
                        "Typing speed",
                        "${it.avgTypingSpeedCpm.roundToInt()} cpm",
                        "Keyboard"
                    )
                )
                add(
                    SupportingMetric(
                        "Backspace rate",
                        "${(it.avgBackspaceRate * 100).roundToInt()}%",
                        "Keyboard"
                    )
                )
            }

            AcousticFeatureStore.get(context)?.let {
                add(
                    SupportingMetric(
                        "Acoustic features",
                        "${it.size} eGeMAPSv02 values",
                        "openSMILE"
                    )
                )
            }

            CognitiveResultStore.get(context)?.let { snapshot ->
                snapshot.stroopAccuracyPercent?.let {
                    add(
                        SupportingMetric(
                            "Stroop accuracy",
                            "${it.roundToInt()}%",
                            "Cognitive tests"
                        )
                    )
                }
                snapshot.digitSpanForward?.let {
                    add(SupportingMetric("Digit span (forward)", it.toString(), "Cognitive tests"))
                }
            }
        }

        val explanation = DiagnosisGuard.sanitizeExplanation(
            PatternDetector.summarise(patterns, baseline != null)
        )

        val insight = LinguisticInsight(
            timestampMs = System.currentTimeMillis(),
            sourceFileName = transcript.sourceFileName,
            patterns = patterns,
            supportingMetrics = supporting,
            baselineComparison = baselineComparison,
            confidence = PatternDetector.confidenceOf(patterns, baseline != null),
            explanation = explanation.text,
            transcriptWordCount = metrics.wordCount,
            modelName = "Local NLP (deterministic)",
            guardApplied = explanation.guardApplied
        )

        InsightStore.save(context, insight)

        Log.d(
            TAG,
            "Deterministic analysis: ${metrics.wordCount} words, " +
                "${patterns.size} pattern(s), baseline=${baseline != null}"
        )

        return Result.success(insight)
    }


    private fun describeBaseline(
        current: TranscriptMetrics,
        baseline: Map<LanguageMetric, Double>?,
        historyCount: Int
    ): String {

        if (baseline == null) {
            return "Baseline not yet established " +
                "($historyCount of ${TranscriptAnalytics.MIN_HISTORY_FOR_BASELINE} " +
                "analysable recordings so far)."
        }

        val parts = LanguageMetric.entries.mapNotNull { metric ->
            val now = metric.valueOf(current)
            val before = baseline[metric] ?: return@mapNotNull null
            "${metric.label}: ${metric.format(now)} vs ${metric.format(before)} baseline"
        }

        return "Compared with your $historyCount previous recordings — " +
            parts.joinToString("; ") + "."
    }


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
}
