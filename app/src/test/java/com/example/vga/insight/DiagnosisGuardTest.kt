package com.example.vga.insight

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The non-diagnostic wording rule is a hard safety requirement, so it is
 * verified directly rather than trusted to the prompt alone.
 */
class DiagnosisGuardTest {

    @Test
    fun `named conditions are removed`() {

        val inputs = listOf(
            "The user has Alzheimer's disease.",
            "This indicates Lewy body dementia.",
            "Findings suggest frontotemporal dementia.",
            "Consistent with FTD.",
            "Likely vascular dementia.",
            "Signs of Parkinson's disease.",
            "This is mild cognitive impairment.",
            "Probable MCI.",
            "The speaker shows aphasia."
        )

        inputs.forEach { input ->
            val result = DiagnosisGuard.sanitize(input)

            assertTrue("guard should fire for: $input", result.guardApplied)

            listOf(
                "alzheimer", "lewy", "frontotemporal", "FTD",
                "vascular dementia", "parkinson",
                "mild cognitive impairment", "MCI", "aphasia"
            ).forEach { banned ->
                assertFalse(
                    "'$banned' must not survive in: ${result.text}",
                    result.text.contains(banned, ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun `definitive attribution phrasing is rewritten`() {

        val cases = listOf(
            "You have dementia.",
            "You are diagnosed with something.",
            "The patient suffers from memory loss.",
            "This confirms cognitive decline.",
            "Supports a diagnosis of decline."
        )

        cases.forEach { input ->
            val result = DiagnosisGuard.sanitize(input)

            assertTrue("guard should fire for: $input", result.guardApplied)
            assertFalse(result.text.contains("you have", ignoreCase = true))
            assertFalse(result.text.contains("suffers from", ignoreCase = true))
            assertFalse(result.text.contains("diagnosed with", ignoreCase = true))
            assertFalse(result.text.contains("diagnosis of", ignoreCase = true))
            assertFalse(result.text.contains("this confirms", ignoreCase = true))
        }
    }

    @Test
    fun `acceptable observational wording is left alone`() {

        val safe =
            "Increased repetition compared with baseline. " +
                "Pattern requiring attention. " +
                "Further professional evaluation may be appropriate."

        val result = DiagnosisGuard.sanitize(safe)

        assertFalse(result.guardApplied)
        assertTrue(result.text == safe)
    }

    @Test
    fun `explanations always carry the screening notice`() {

        val result = DiagnosisGuard.sanitizeExplanation("Some observed changes in wording.")

        assertTrue(result.text.contains("not a medical diagnosis"))
        assertTrue(result.text.contains("Further professional"))
    }

    @Test
    fun `screening notice is not duplicated`() {

        val already = "Observed change. ${DiagnosisGuard.SCREENING_NOTICE}"
        val result = DiagnosisGuard.sanitizeExplanation(already)

        val occurrences =
            Regex("not a medical diagnosis", RegexOption.IGNORE_CASE)
                .findAll(result.text).count()

        assertTrue("notice should appear once, found $occurrences", occurrences == 1)
    }
}
