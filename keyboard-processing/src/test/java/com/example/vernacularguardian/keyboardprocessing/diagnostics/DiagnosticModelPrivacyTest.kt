package com.example.vernacularguardian.keyboardprocessing.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sprint 8 Section 35: a repository-wide, reflection-based check (not just
 * visual inspection) that no developer-diagnostics model carries a raw-text
 * field. Every `String`-typed field across the diagnostics entities and
 * dashboard DTOs must be one of a small, explicit allowlist of fixed
 * code/label fields - never an open-ended text field - and none may be
 * named after a known-sensitive concept (package name, typed text, password,
 * clipboard, device identity).
 */
class DiagnosticModelPrivacyTest {

    private val modelsToCheck: List<Class<*>> = listOf(
        DeveloperDiagnosticEventEntity::class.java,
        DeveloperDiagnosticSampleEntity::class.java,
        DeveloperDiagnosticsSnapshot::class.java,
        OverviewSnapshot::class.java,
        SessionAnalyticsSnapshot::class.java,
        SessionDetailRow::class.java,
        DailySummaryRow::class.java,
        DailyAnalyticsSnapshot::class.java,
        OwnerGateSnapshot::class.java,
        AccessibilityEventSnapshot::class.java,
        BoundaryTypeStat::class.java,
        SessionBoundarySnapshot::class.java,
        WorkManagerSnapshot::class.java,
        DatabaseSnapshot::class.java,
        ResourceSnapshot::class.java,
        BatterySnapshot::class.java,
        ThermalSnapshot::class.java,
        ServiceLifecycleSnapshot::class.java,
        ErrorCountRow::class.java,
        ErrorSnapshot::class.java,
        DiagnosticEventRow::class.java,
        DiagnosticHistorySnapshot::class.java
    )

    private val allowedStringFields = setOf(
        "eventType",
        "currentState",
        "uniqueWorkName",
        "databaseName",
        "cpuMeasurementNote",
        "label",
        "thermalStatus",
        "note",
        "errorType"
    )

    private val forbiddenFieldNameFragments = listOf(
        "packagename", "package", "typedtext", "rawtext", "clipboard",
        "passwordtext", "nodetext", "appname", "deviceid", "typedcontent"
    )

    private fun declaredInstanceFields(clazz: Class<*>) =
        clazz.declaredFields.filter { !it.isSynthetic && it.name != "Companion" && it.name != "INSTANCE" }

    @Test
    fun `every String field across diagnostic models is an explicitly allowed fixed-code field`() {
        for (clazz in modelsToCheck) {
            val stringFields = declaredInstanceFields(clazz).filter { it.type == String::class.java }
            for (field in stringFields) {
                assertTrue(
                    "${clazz.simpleName}.${field.name} is a String field not in the allowlist - " +
                        "diagnostic models must never carry an open-ended text field",
                    allowedStringFields.contains(field.name)
                )
            }
        }
    }

    @Test
    fun `no String-typed diagnostic model field name matches a known-sensitive concept`() {
        // Scoped to String fields only: a Long counter is free to describe
        // WHAT it counts (e.g. "nullPackageWindowEvents" counts events with a
        // null package - it holds no package identity itself), but a String
        // field named after one of these concepts would actually be able to
        // carry the sensitive value.
        for (clazz in modelsToCheck) {
            val stringFields = declaredInstanceFields(clazz).filter { it.type == String::class.java }
            for (field in stringFields) {
                val lowerName = field.name.lowercase()
                for (fragment in forbiddenFieldNameFragments) {
                    assertFalse(
                        "${clazz.simpleName}.${field.name} (a String field) matches forbidden fragment '$fragment'",
                        lowerName.contains(fragment)
                    )
                }
            }
        }
    }

    @Test
    fun `DeveloperDiagnosticEventEntity has no free-text payload field`() {
        val fieldNames = declaredInstanceFields(DeveloperDiagnosticEventEntity::class.java).map { it.name }.toSet()
        assertTrue(fieldNames.containsAll(setOf("id", "timestampEpochMs", "eventType", "valueLong", "valueDouble", "durationMs")))
        assertTrue(fieldNames.size == 6)
    }

    @Test
    fun `DeveloperDiagnosticSampleEntity has no String field at all`() {
        val stringFields = declaredInstanceFields(DeveloperDiagnosticSampleEntity::class.java)
            .filter { it.type == String::class.java }
        assertTrue(stringFields.isEmpty())
    }
}
