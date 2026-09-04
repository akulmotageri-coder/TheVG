package com.example.vga.insight

import kotlin.math.roundToInt


/**
 * Objectively computed properties of a transcript.
 *
 * These are measured locally from the real transcript text - the model is
 * never asked to invent them. They serve three purposes: they are shown as
 * supporting metrics, they are given to the model as grounding, and they are
 * what the personal baseline comparison is computed from.
 *
 * Pure Kotlin with no Android dependency so it is unit-testable on the JVM.
 */
data class TranscriptMetrics(
    val wordCount: Int,
    val distinctWordCount: Int,
    val typeTokenRatio: Double,
    val sentenceCount: Int,
    val meanSentenceLengthWords: Double,
    val immediateRepetitionCount: Int,
    val repeatedPhraseCount: Int,
    val vagueTermCount: Int,
    val vagueTermRatePer100Words: Double
) {

    fun asSupportingMetrics(): List<SupportingMetric> = listOf(
        SupportingMetric(
            label = "Words spoken",
            value = wordCount.toString(),
            source = "Transcript"
        ),
        SupportingMetric(
            label = "Vocabulary diversity",
            value = "${(typeTokenRatio * 100).roundToInt()}%",
            source = "Transcript"
        ),
        SupportingMetric(
            label = "Mean sentence length",
            value = "${meanSentenceLengthWords.roundToInt()} words",
            source = "Transcript"
        ),
        SupportingMetric(
            label = "Immediate word repetitions",
            value = immediateRepetitionCount.toString(),
            source = "Transcript"
        ),
        SupportingMetric(
            label = "Repeated phrases",
            value = repeatedPhraseCount.toString(),
            source = "Transcript"
        ),
        SupportingMetric(
            label = "Vague-term rate",
            value = "${(vagueTermRatePer100Words * 10).roundToInt() / 10.0} /100 words",
            source = "Transcript"
        )
    )
}


object TranscriptAnalytics {

    /**
     * Below this many real words a transcript cannot support a linguistic
     * judgement, so analysis is refused outright rather than producing
     * alarming patterns from noise.
     */
    const val MIN_WORDS_FOR_ANALYSIS = 20

    /**
     * Non-speech placeholders emitted by the speech recogniser when it hears
     * sound but no intelligible words. These are not user speech and must
     * never be measured as vocabulary.
     */
    private val placeholderTokens = Regex(
        """[\[(<]\s*(something|blank[_ ]?audio|inaudible|unintelligible|music|noise|silence|laughter|applause|sound|speaking in [^\])>]*)\s*[\])>]""",
        RegexOption.IGNORE_CASE
    )

    /** Strips recogniser placeholders, leaving only real spoken words. */
    fun stripPlaceholders(text: String): String =
        placeholderTokens.replace(text, " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    /**
     * True when the transcript carries too little real speech to analyse.
     * Placeholders are removed first, so "[SOMETHING]" counts as empty.
     */
    fun isTooShortToAnalyse(text: String): Boolean =
        compute(stripPlaceholders(text)).wordCount < MIN_WORDS_FOR_ANALYSIS


    /**
     * Words that carry little specific meaning. An increase in these is one of
     * the documented word-finding / vagueness signals.
     */
    private val vagueTerms = setOf(
        "thing", "things", "stuff", "something", "someone", "somebody",
        "somewhere", "whatever", "whatsit", "thingy", "kind", "sort",
        "anything", "everything", "nothing", "it", "that", "those", "these"
    )

    fun compute(text: String): TranscriptMetrics {

        val words = text.lowercase()
            .split(Regex("[^\\p{L}\\p{N}']+"))
            .filter { it.isNotBlank() }

        if (words.isEmpty()) {
            return TranscriptMetrics(
                wordCount = 0,
                distinctWordCount = 0,
                typeTokenRatio = 0.0,
                sentenceCount = 0,
                meanSentenceLengthWords = 0.0,
                immediateRepetitionCount = 0,
                repeatedPhraseCount = 0,
                vagueTermCount = 0,
                vagueTermRatePer100Words = 0.0
            )
        }

        val distinct = words.toSet()

        val sentences = text
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val sentenceCount = maxOf(sentences.size, 1)

        val immediateRepetitions = (1 until words.size).count { index ->
            words[index] == words[index - 1]
        }

        // Repeated 3-word phrases (a proxy for repeating questions/information).
        val repeatedPhrases =
            if (words.size >= 3) {
                val trigrams = (0..words.size - 3).map {
                    "${words[it]} ${words[it + 1]} ${words[it + 2]}"
                }
                trigrams.groupingBy { it }.eachCount().count { it.value > 1 }
            } else {
                0
            }

        val vagueCount = words.count { it in vagueTerms }

        return TranscriptMetrics(
            wordCount = words.size,
            distinctWordCount = distinct.size,
            typeTokenRatio = distinct.size.toDouble() / words.size.toDouble(),
            sentenceCount = sentenceCount,
            meanSentenceLengthWords = words.size.toDouble() / sentenceCount.toDouble(),
            immediateRepetitionCount = immediateRepetitions,
            repeatedPhraseCount = repeatedPhrases,
            vagueTermCount = vagueCount,
            vagueTermRatePer100Words = vagueCount * 100.0 / words.size.toDouble()
        )
    }

    /**
     * Compares the current transcript against the mean of previous ones.
     * Returns null when there is no history to compare with, rather than
     * inventing a baseline.
     */
    fun compareWithBaseline(
        current: TranscriptMetrics,
        priors: List<TranscriptMetrics>
    ): String? {

        if (priors.isEmpty()) return null

        val baselineDiversity = priors.map { it.typeTokenRatio }.average()
        val baselineVague = priors.map { it.vagueTermRatePer100Words }.average()
        val baselineRepeats = priors.map { it.repeatedPhraseCount.toDouble() }.average()
        val baselineSentence = priors.map { it.meanSentenceLengthWords }.average()

        val parts = mutableListOf<String>()

        fun describe(label: String, now: Double, before: Double, higherIsConcerning: Boolean) {
            if (before <= 0.0 && now <= 0.0) return
            val delta = now - before
            val percent =
                if (before > 0.0) (delta / before * 100).roundToInt() else 0
            if (kotlin.math.abs(percent) < 10) {
                parts += "$label stable vs baseline"
            } else {
                val direction = if (delta > 0) "higher" else "lower"
                val concerning =
                    (delta > 0 && higherIsConcerning) || (delta < 0 && !higherIsConcerning)
                val suffix = if (concerning) " (worth attention)" else ""
                parts += "$label ${kotlin.math.abs(percent)}% $direction than baseline$suffix"
            }
        }

        describe("Vocabulary diversity", current.typeTokenRatio, baselineDiversity, false)
        describe("Vague-term use", current.vagueTermRatePer100Words, baselineVague, true)
        describe("Repeated phrases", current.repeatedPhraseCount.toDouble(), baselineRepeats, true)
        describe("Sentence length", current.meanSentenceLengthWords, baselineSentence, false)

        return "Compared with ${priors.size} previous transcript" +
            (if (priors.size == 1) "" else "s") + ": " + parts.joinToString("; ") + "."
    }
}
