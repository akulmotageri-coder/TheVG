package com.example.vga.insight

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt


/**
 * Objectively computed properties of a transcript.
 *
 * Every value here is measured locally from the real transcript text. The AI
 * assistant is never asked to produce or alter these numbers - it only ever
 * explains them. Pure Kotlin with no Android dependency so the whole feature
 * set is unit-testable on the JVM.
 */
data class TranscriptMetrics(

    // Volume / vocabulary
    val wordCount: Int,
    val distinctWordCount: Int,
    val typeTokenRatio: Double,

    // Sentence structure
    val sentenceCount: Int,
    val meanSentenceLengthWords: Double,
    val sentenceLengthStdDev: Double,
    val shortSentenceRatePercent: Double,

    // Repetition
    val immediateRepetitionCount: Int,
    val repeatedPhraseCount: Int,
    val repeatedSentenceCount: Int,
    val repetitionRatePercent: Double,

    // Word choice
    val fillerCount: Int,
    val fillerRatePer100Words: Double,
    val vagueTermCount: Int,
    val vagueTermRatePer100Words: Double,

    // Disfluency
    val selfCorrectionCount: Int,
    val selfCorrectionRatePer100Words: Double
) {

    fun asSupportingMetrics(): List<SupportingMetric> = listOf(
        SupportingMetric("Words spoken", wordCount.toString(), "Transcript"),
        SupportingMetric("Unique words", distinctWordCount.toString(), "Transcript"),
        SupportingMetric(
            "Vocabulary diversity",
            round2(typeTokenRatio).toString(),
            "Transcript"
        ),
        SupportingMetric("Sentences", sentenceCount.toString(), "Transcript"),
        SupportingMetric(
            "Mean sentence length",
            "${meanSentenceLengthWords.roundToInt()} words",
            "Transcript"
        ),
        SupportingMetric(
            "Sentence-length variability",
            round2(sentenceLengthStdDev).toString(),
            "Transcript"
        ),
        SupportingMetric(
            "Short/fragmented sentences",
            "${shortSentenceRatePercent.roundToInt()}%",
            "Transcript"
        ),
        SupportingMetric(
            "Repetition rate",
            "${round1(repetitionRatePercent)}%",
            "Transcript"
        ),
        SupportingMetric("Repeated phrases", repeatedPhraseCount.toString(), "Transcript"),
        SupportingMetric("Repeated sentences", repeatedSentenceCount.toString(), "Transcript"),
        SupportingMetric(
            "Filler words",
            "$fillerCount (${round1(fillerRatePer100Words)}/100 words)",
            "Transcript"
        ),
        SupportingMetric(
            "Vague words",
            "$vagueTermCount (${round1(vagueTermRatePer100Words)}/100 words)",
            "Transcript"
        ),
        SupportingMetric(
            "Self-corrections",
            "$selfCorrectionCount (${round1(selfCorrectionRatePer100Words)}/100 words)",
            "Transcript"
        )
    )
}


internal fun round1(value: Double): Double = (value * 10).roundToInt() / 10.0
internal fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0


/**
 * Which measured metric a chart or comparison is showing. Kept as an enum so
 * the trend chart, baseline comparison and AI context all reference the same
 * definitions rather than duplicating strings.
 */
enum class LanguageMetric(
    val label: String,
    val unit: String,
    /** True when a rise in this value is the direction worth attention. */
    val higherIsNotable: Boolean
) {
    REPETITION_RATE("Repetition rate", "%", true),
    VOCABULARY_DIVERSITY("Vocabulary diversity", "", false),
    FILLER_RATE("Filler words", "/100 words", true),
    VAGUE_RATE("Vague words", "/100 words", true),
    MEAN_SENTENCE_LENGTH("Mean sentence length", " words", false),
    SENTENCE_VARIABILITY("Sentence-length variability", "", true),
    SELF_CORRECTION_RATE("Self-corrections", "/100 words", true);

    fun valueOf(metrics: TranscriptMetrics): Double = when (this) {
        REPETITION_RATE -> metrics.repetitionRatePercent
        VOCABULARY_DIVERSITY -> metrics.typeTokenRatio
        FILLER_RATE -> metrics.fillerRatePer100Words
        VAGUE_RATE -> metrics.vagueTermRatePer100Words
        MEAN_SENTENCE_LENGTH -> metrics.meanSentenceLengthWords
        SENTENCE_VARIABILITY -> metrics.sentenceLengthStdDev
        SELF_CORRECTION_RATE -> metrics.selfCorrectionRatePer100Words
    }

    fun format(value: Double): String = when (this) {
        VOCABULARY_DIVERSITY -> round2(value).toString()
        MEAN_SENTENCE_LENGTH -> "${value.roundToInt()}$unit"
        else -> "${round1(value)}$unit"
    }
}


object TranscriptAnalytics {

    /**
     * Below this many real words a transcript cannot support a linguistic
     * judgement, so analysis is refused outright rather than producing
     * alarming patterns from noise.
     */
    const val MIN_WORDS_FOR_ANALYSIS = 20

    /** Number of prior transcripts required before a baseline is claimed. */
    const val MIN_HISTORY_FOR_BASELINE = 3

    /**
     * Non-speech placeholders emitted by the speech recogniser when it hears
     * sound but no intelligible words. These are not user speech and must
     * never be measured as vocabulary.
     */
    private val placeholderTokens = Regex(
        """[\[(<]\s*(something|blank[_ ]?audio|inaudible|unintelligible|music|noise|silence|laughter|applause|sound|speaking in [^\])>]*)\s*[\])>]""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Words that carry little specific meaning. An increase in these is one of
     * the documented word-finding / vagueness signals.
     */
    private val vagueTerms = setOf(
        "thing", "things", "stuff", "something", "someone", "somebody",
        "somewhere", "whatever", "whatsit", "thingy", "kind", "sort",
        "anything", "everything", "nothing"
    )

    /** Single-token hesitation/filler markers. */
    private val fillerWords = setOf(
        "um", "uh", "erm", "er", "ah", "hmm", "mmm", "eh",
        "like", "basically", "actually", "literally", "anyway"
    )

    /** Multi-word filler phrases, matched on the normalised text. */
    private val fillerPhrases = listOf(
        "you know", "i mean", "sort of", "kind of", "or something"
    )

    /**
     * Explicit repair markers. "i mean" also counts as a filler phrase; the
     * two feature families are reported separately and deliberately overlap.
     */
    private val selfCorrectionMarkers = listOf(
        "i mean", "no wait", "wait no", "sorry no", "or rather",
        "what i meant", "no no", "i meant", "let me rephrase", "scratch that"
    )

    /**
     * A speech recogniser under stress can fall into a loop and emit one token
     * over and over ("ndashar ndashar ndashar ..."). That is a decoder
     * artifact, not speech, and measuring it would manufacture extreme
     * repetition and near-zero vocabulary diversity.
     */
    private const val DEGENERATE_TOKEN_SHARE = 0.5

    /**
     * A looping recogniser often repeats a whole phrase rather than a single
     * token ("How are you? How are you? ..."), which slips under the
     * single-token check. Natural conversation does not repeat the same
     * three-word sequence for most of a transcript, so a trigram repetition
     * rate this high indicates a decoder loop, not clinical repetition -
     * genuine repetition of interest sits an order of magnitude lower.
     */
    private const val DEGENERATE_REPETITION_PERCENT = 60.0

    /** Strips recogniser placeholders, leaving only real spoken words. */
    fun stripPlaceholders(text: String): String =
        placeholderTokens.replace(text, " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    /**
     * True when the text looks like a recogniser loop rather than speech:
     * a single token dominates, or there is almost no distinct vocabulary.
     */
    fun isDegenerateOutput(text: String): Boolean {

        val words = stripPlaceholders(text).lowercase()
            .split(Regex("[^\\p{L}\\p{N}']+"))
            .filter { it.isNotBlank() }

        if (words.size < 6) return false

        val counts = words.groupingBy { it }.eachCount()
        val topShare = (counts.values.maxOrNull() ?: 0).toDouble() / words.size

        if (topShare >= DEGENERATE_TOKEN_SHARE) return true

        // Long output built from a two-word vocabulary is equally degenerate.
        if (counts.size <= 2 && words.size >= 10) return true

        // Phrase-level loop ("How are you? How are you? ...").
        if (words.size >= 20) {
            val metrics = compute(stripPlaceholders(text))
            if (metrics.repetitionRatePercent >= DEGENERATE_REPETITION_PERCENT) return true
        }

        return false
    }

    /**
     * True when the transcript carries too little real speech to analyse.
     * Placeholders are removed first, so "[SOMETHING]" counts as empty, and
     * recogniser loops are rejected regardless of length.
     */
    fun isTooShortToAnalyse(text: String): Boolean {
        if (isDegenerateOutput(text)) return true
        return compute(stripPlaceholders(text)).wordCount < MIN_WORDS_FOR_ANALYSIS
    }

    fun compute(text: String): TranscriptMetrics {

        val normalised = text.lowercase()

        val words = normalised
            .split(Regex("[^\\p{L}\\p{N}']+"))
            .filter { it.isNotBlank() }

        if (words.isEmpty()) return emptyMetrics()

        val distinct = words.toSet()

        // ----- sentences -----

        val sentences = text
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val sentenceWordCounts = sentences.map { sentence ->
            sentence.split(Regex("[^\\p{L}\\p{N}']+")).count { it.isNotBlank() }
        }.filter { it > 0 }

        val sentenceCount = maxOf(sentenceWordCounts.size, 1)

        val meanSentenceLength =
            if (sentenceWordCounts.isEmpty()) words.size.toDouble()
            else sentenceWordCounts.average()

        val sentenceStdDev =
            if (sentenceWordCounts.size < 2) 0.0
            else sqrt(
                sentenceWordCounts.sumOf { count ->
                    val diff = count - meanSentenceLength
                    diff * diff
                } / sentenceWordCounts.size
            )

        val shortSentences = sentenceWordCounts.count { it < 4 }
        val shortSentenceRate =
            if (sentenceWordCounts.isEmpty()) 0.0
            else shortSentences * 100.0 / sentenceWordCounts.size

        // ----- repetition -----

        val immediateRepetitions = (1 until words.size).count { index ->
            words[index] == words[index - 1]
        }

        val trigrams =
            if (words.size >= 3) {
                (0..words.size - 3).map { "${words[it]} ${words[it + 1]} ${words[it + 2]}" }
            } else {
                emptyList()
            }

        val trigramCounts = trigrams.groupingBy { it }.eachCount()
        val repeatedPhrases = trigramCounts.count { it.value > 1 }

        // Occurrences beyond the first, as a share of all trigrams. This is
        // independent of vocabulary diversity and stays in a readable range.
        val excessTrigrams = trigramCounts.values.sumOf { maxOf(it - 1, 0) }
        val repetitionRate =
            if (trigrams.isEmpty()) 0.0
            else excessTrigrams * 100.0 / trigrams.size

        val normalisedSentences = sentences.map { sentence ->
            sentence.lowercase().replace(Regex("[^\\p{L}\\p{N} ]+"), "").trim()
        }.filter { it.isNotBlank() }

        val repeatedSentences = normalisedSentences
            .groupingBy { it }
            .eachCount()
            .count { it.value > 1 }

        // ----- word choice -----

        val vagueCount = words.count { it in vagueTerms }

        val singleFillers = words.count { it in fillerWords }
        val phraseFillers = fillerPhrases.sumOf { phrase ->
            countOccurrences(normalised, phrase)
        }
        val fillerCount = singleFillers + phraseFillers

        val selfCorrections = selfCorrectionMarkers.sumOf { marker ->
            countOccurrences(normalised, marker)
        }

        val per100 = 100.0 / words.size

        return TranscriptMetrics(
            wordCount = words.size,
            distinctWordCount = distinct.size,
            typeTokenRatio = distinct.size.toDouble() / words.size,
            sentenceCount = sentenceCount,
            meanSentenceLengthWords = meanSentenceLength,
            sentenceLengthStdDev = sentenceStdDev,
            shortSentenceRatePercent = shortSentenceRate,
            immediateRepetitionCount = immediateRepetitions,
            repeatedPhraseCount = repeatedPhrases,
            repeatedSentenceCount = repeatedSentences,
            repetitionRatePercent = repetitionRate,
            fillerCount = fillerCount,
            fillerRatePer100Words = fillerCount * per100,
            vagueTermCount = vagueCount,
            vagueTermRatePer100Words = vagueCount * per100,
            selfCorrectionCount = selfCorrections,
            selfCorrectionRatePer100Words = selfCorrections * per100
        )
    }

    private fun countOccurrences(haystack: String, needle: String): Int {
        if (needle.isEmpty()) return 0
        var count = 0
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            count++
            index = haystack.indexOf(needle, index + needle.length)
        }
        return count
    }

    private fun emptyMetrics() = TranscriptMetrics(
        wordCount = 0,
        distinctWordCount = 0,
        typeTokenRatio = 0.0,
        sentenceCount = 0,
        meanSentenceLengthWords = 0.0,
        sentenceLengthStdDev = 0.0,
        shortSentenceRatePercent = 0.0,
        immediateRepetitionCount = 0,
        repeatedPhraseCount = 0,
        repeatedSentenceCount = 0,
        repetitionRatePercent = 0.0,
        fillerCount = 0,
        fillerRatePer100Words = 0.0,
        vagueTermCount = 0,
        vagueTermRatePer100Words = 0.0,
        selfCorrectionCount = 0,
        selfCorrectionRatePer100Words = 0.0
    )


    // ========================================================
    // BASELINE
    // ========================================================

    /**
     * The user's personal baseline: the mean of each metric across previous
     * transcripts. Returns null when there is not enough history, so the UI
     * can say "Baseline not yet established" rather than invent a number.
     */
    fun baselineOf(priors: List<TranscriptMetrics>): Map<LanguageMetric, Double>? {

        if (priors.size < MIN_HISTORY_FOR_BASELINE) return null

        return LanguageMetric.entries.associateWith { metric ->
            priors.map { metric.valueOf(it) }.average()
        }
    }

    /** Percentage change of [current] against [baseline], or null if undefined. */
    fun percentChange(current: Double, baseline: Double): Double? {
        if (abs(baseline) < 1e-9) return null
        return (current - baseline) / baseline * 100.0
    }
}
