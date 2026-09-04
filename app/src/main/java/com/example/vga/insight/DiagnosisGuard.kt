package com.example.vga.insight

/**
 * Enforces the non-diagnostic wording rule on anything the local model emits.
 *
 * The prompt already forbids diagnostic language, but a model is not a
 * guarantee, so every string that reaches the UI is passed through here as a
 * hard backstop. Two things are removed:
 *
 *  1. Named conditions (Alzheimer's, Lewy body, FTD, ...) - replaced with
 *     neutral wording, so the app can never attribute a disease to the user.
 *  2. Definitive attribution phrasing ("you have", "diagnosed with",
 *     "suffers from", ...) - rewritten to observational phrasing.
 *
 * The result is always a pattern/screening statement, never a diagnosis.
 */
object DiagnosisGuard {

    private const val NEUTRAL_CONDITION = "a pattern requiring attention"

    /** Named conditions that must never be attributed to the user. */
    private val conditionPatterns = listOf(
        Regex("""\balzheimer'?s?(\s+disease)?\b""", RegexOption.IGNORE_CASE),
        Regex("""\blewy\s+bod(y|ies)(\s+dementia)?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bfrontotemporal(\s+dementia)?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bFTD\b"""),
        Regex("""\bvascular\s+dementia\b""", RegexOption.IGNORE_CASE),
        Regex("""\bparkinson'?s?(\s+disease)?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bhuntington'?s?(\s+disease)?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmild\s+cognitive\s+impairment\b""", RegexOption.IGNORE_CASE),
        Regex("""\bMCI\b"""),
        Regex("""\baphasia\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Definitive attribution phrasings, rewritten rather than removed so the
     * surrounding sentence still reads naturally.
     */
    private val phrasingRewrites = listOf(
        Regex("""\byou\s+(definitely\s+|certainly\s+|clearly\s+)?have\b""", RegexOption.IGNORE_CASE)
            to "the transcript shows signs associated with",
        Regex("""\byou\s+are\s+(suffering\s+from|diagnosed\s+with)\b""", RegexOption.IGNORE_CASE)
            to "the transcript shows signs associated with",
        Regex("""\b(is|are)\s+diagnosed\s+with\b""", RegexOption.IGNORE_CASE)
            to "shows patterns associated with",
        Regex("""\bsuffers?\s+from\b""", RegexOption.IGNORE_CASE)
            to "shows patterns associated with",
        Regex("""\bthis\s+(confirms|proves)\b""", RegexOption.IGNORE_CASE)
            to "this may indicate",
        Regex("""\bdiagnosis\s+of\b""", RegexOption.IGNORE_CASE)
            to "pattern consistent with"
    )

    /** Standard footer appended to every explanation shown to the user. */
    const val SCREENING_NOTICE: String =
        "This is an early-warning screening signal based on observed language " +
            "patterns, not a medical diagnosis. Further professional " +
            "evaluation may be appropriate."

    data class Result(
        val text: String,
        val guardApplied: Boolean
    )

    /**
     * Sanitises one string. Returns the cleaned text plus whether anything had
     * to be rewritten, so the UI can disclose that a guard fired.
     */
    fun sanitize(input: String): Result {

        if (input.isBlank()) {
            return Result(input, false)
        }

        var output = input
        var applied = false

        for ((pattern, replacement) in phrasingRewrites) {
            if (pattern.containsMatchIn(output)) {
                output = pattern.replace(output, replacement)
                applied = true
            }
        }

        for (pattern in conditionPatterns) {
            if (pattern.containsMatchIn(output)) {
                output = pattern.replace(output, NEUTRAL_CONDITION)
                applied = true
            }
        }

        // Collapse any double spaces introduced by replacement.
        output = output.replace(Regex(" {2,}"), " ").trim()

        return Result(output, applied)
    }

    /** Sanitises and guarantees the screening notice is present. */
    fun sanitizeExplanation(input: String): Result {

        val sanitized = sanitize(input)

        val withNotice =
            if (sanitized.text.contains("not a medical diagnosis", ignoreCase = true)) {
                sanitized.text
            } else {
                sanitized.text.trimEnd().let {
                    if (it.isEmpty()) SCREENING_NOTICE else "$it\n\n$SCREENING_NOTICE"
                }
            }

        return Result(withNotice, sanitized.guardApplied)
    }
}
