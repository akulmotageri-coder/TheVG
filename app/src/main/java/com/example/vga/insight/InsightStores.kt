package com.example.vga.insight

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


private const val TAG = "VGA_INSIGHT_STORE"


/**
 * Persists transcripts produced by the existing IndicWhisper pipeline.
 *
 * Transcripts were previously computed on demand and discarded, which left no
 * history to compare against. Storing them is what makes a personal baseline
 * possible.
 */
object TranscriptStore {

    private const val FILE_NAME = "linguistic_transcripts.json"
    private const val MAX_RECORDS = 100

    fun save(context: Context, record: TranscriptRecord) {

        runCatching {

            // One record per source recording. Re-analysing the same file
            // replaces its entry instead of adding a duplicate - otherwise
            // repeated taps would stack identical transcripts and skew the
            // personal baseline they are averaged into.
            val existing = getAll(context)
                .filterNot { it.sourceFileName == record.sourceFileName }
                .toMutableList()

            existing.add(record)

            // Newest first, bounded so the file cannot grow without limit.
            val trimmed = existing
                .sortedByDescending { it.timestampMs }
                .take(MAX_RECORDS)

            val array = JSONArray()
            trimmed.forEach { item ->
                array.put(
                    JSONObject()
                        .put("timestampMs", item.timestampMs)
                        .put("sourceFileName", item.sourceFileName)
                        .put("text", item.text)
                )
            }

            File(context.filesDir, FILE_NAME).writeText(array.toString())

        }.onFailure {
            Log.e(TAG, "Failed to save transcript", it)
        }
    }

    fun getAll(context: Context): List<TranscriptRecord> {

        return runCatching {

            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()

            val array = JSONArray(file.readText())

            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { item ->
                    TranscriptRecord(
                        timestampMs = item.optLong("timestampMs"),
                        sourceFileName = item.optString("sourceFileName"),
                        text = item.optString("text")
                    )
                }
            }.sortedByDescending { it.timestampMs }

        }.getOrElse {
            Log.e(TAG, "Failed to read transcripts", it)
            emptyList()
        }
    }
}


/**
 * Persists the latest cognitive-test results so the fusion step can read them.
 *
 * The cognitive module keeps its results in memory only; this store is written
 * from VGA's own entry adapter, leaving the module's internals untouched.
 */
object CognitiveResultStore {

    private const val FILE_NAME = "cognitive_results.json"

    fun save(context: Context, snapshot: CognitiveSnapshot) {

        runCatching {

            val json = JSONObject()
                .put("timestampMs", System.currentTimeMillis())

            snapshot.stroopAccuracyPercent?.let { json.put("stroopAccuracyPercent", it) }
            snapshot.stroopAvgResponseMs?.let { json.put("stroopAvgResponseMs", it) }
            snapshot.digitSpanForward?.let { json.put("digitSpanForward", it) }
            snapshot.digitSpanBackward?.let { json.put("digitSpanBackward", it) }
            snapshot.trailMakingPartAMs?.let { json.put("trailMakingPartAMs", it) }
            snapshot.trailMakingPartBMs?.let { json.put("trailMakingPartBMs", it) }

            File(context.filesDir, FILE_NAME).writeText(json.toString())

        }.onFailure {
            Log.e(TAG, "Failed to save cognitive results", it)
        }
    }

    fun get(context: Context): CognitiveSnapshot? {

        return runCatching {

            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return null

            val json = JSONObject(file.readText())

            CognitiveSnapshot(
                stroopAccuracyPercent =
                    if (json.has("stroopAccuracyPercent")) json.getDouble("stroopAccuracyPercent") else null,
                stroopAvgResponseMs =
                    if (json.has("stroopAvgResponseMs")) json.getLong("stroopAvgResponseMs") else null,
                digitSpanForward =
                    if (json.has("digitSpanForward")) json.getInt("digitSpanForward") else null,
                digitSpanBackward =
                    if (json.has("digitSpanBackward")) json.getInt("digitSpanBackward") else null,
                trailMakingPartAMs =
                    if (json.has("trailMakingPartAMs")) json.getLong("trailMakingPartAMs") else null,
                trailMakingPartBMs =
                    if (json.has("trailMakingPartBMs")) json.getLong("trailMakingPartBMs") else null
            )

        }.getOrElse {
            Log.e(TAG, "Failed to read cognitive results", it)
            null
        }
    }
}


/**
 * Persists completed analyses. This is the backing store for the timeline
 * shown on the Main Dashboard.
 */
object InsightStore {

    private const val FILE_NAME = "linguistic_insights.json"
    private const val MAX_RECORDS = 50

    fun save(context: Context, insight: LinguisticInsight) {

        runCatching {

            val existing = getAll(context).toMutableList()
            existing.add(insight)

            val trimmed = existing
                .sortedByDescending { it.timestampMs }
                .take(MAX_RECORDS)

            val array = JSONArray()
            trimmed.forEach { array.put(it.toJson()) }

            File(context.filesDir, FILE_NAME).writeText(array.toString())

        }.onFailure {
            Log.e(TAG, "Failed to save insight", it)
        }
    }

    fun getAll(context: Context): List<LinguisticInsight> {

        return runCatching {

            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()

            val array = JSONArray(file.readText())

            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.toInsight()
            }.sortedByDescending { it.timestampMs }

        }.getOrElse {
            Log.e(TAG, "Failed to read insights", it)
            emptyList()
        }
    }

    private fun LinguisticInsight.toJson(): JSONObject {

        val patternArray = JSONArray()
        patterns.forEach { pattern ->
            patternArray.put(
                JSONObject()
                    .put("label", pattern.label)
                    .put("strength", pattern.strength)
                    .put("evidence", pattern.evidence)
            )
        }

        val metricArray = JSONArray()
        supportingMetrics.forEach { metric ->
            metricArray.put(
                JSONObject()
                    .put("label", metric.label)
                    .put("value", metric.value)
                    .put("source", metric.source)
            )
        }

        return JSONObject()
            .put("timestampMs", timestampMs)
            .put("sourceFileName", sourceFileName)
            .put("patterns", patternArray)
            .put("supportingMetrics", metricArray)
            .put("baselineComparison", baselineComparison)
            .put("confidence", confidence)
            .put("explanation", explanation)
            .put("transcriptWordCount", transcriptWordCount)
            .put("modelName", modelName)
            .put("guardApplied", guardApplied)
    }

    private fun JSONObject.toInsight(): LinguisticInsight {

        val patternArray = optJSONArray("patterns") ?: JSONArray()
        val patterns = (0 until patternArray.length()).mapNotNull { index ->
            patternArray.optJSONObject(index)?.let {
                DetectedPattern(
                    label = it.optString("label"),
                    strength = it.optString("strength"),
                    evidence = it.optString("evidence")
                )
            }
        }

        val metricArray = optJSONArray("supportingMetrics") ?: JSONArray()
        val metrics = (0 until metricArray.length()).mapNotNull { index ->
            metricArray.optJSONObject(index)?.let {
                SupportingMetric(
                    label = it.optString("label"),
                    value = it.optString("value"),
                    source = it.optString("source")
                )
            }
        }

        return LinguisticInsight(
            timestampMs = optLong("timestampMs"),
            sourceFileName = optString("sourceFileName"),
            patterns = patterns,
            supportingMetrics = metrics,
            baselineComparison = optString("baselineComparison"),
            confidence = optString("confidence"),
            explanation = optString("explanation"),
            transcriptWordCount = optInt("transcriptWordCount"),
            modelName = optString("modelName"),
            guardApplied = optBoolean("guardApplied")
        )
    }
}
