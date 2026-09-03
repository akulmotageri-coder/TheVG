package com.example.vernacularguardian.keyboardprocessing.diagnostics

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vernacularguardian.keyboardprocessing.service.KeystrokeAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 8 Section 34 items 9-10: current accessibility authorization read,
 * and diagnostics enable/disable behavior, against the real
 * [DeveloperDiagnosticsRepository] singleton and the real device
 * `AccessibilityManager`.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperDiagnosticsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: DeveloperDiagnosticsRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = DeveloperDiagnostics.repository(context)
        repository.setEnabled(false)
        DeveloperDiagnosticCounters.reset()
    }

    @After
    fun tearDown() = runBlocking {
        repository.clearHistory()
        repository.setEnabled(false)
    }

    @Test
    fun setEnabledTogglesTheEnabledStateFlow() {
        assertFalse(repository.enabled.value)

        repository.setEnabled(true)
        assertTrue(repository.enabled.value)

        repository.setEnabled(false)
        assertFalse(repository.enabled.value)
    }

    @Test
    fun countAlwaysIncrementsTheInMemoryCounterRegardlessOfEnabledState() {
        repository.setEnabled(false)
        val before = DeveloperDiagnosticCounters.get(DiagnosticEventType.TEXT_CHANGED_TOTAL)

        repository.count(DiagnosticEventType.TEXT_CHANGED_TOTAL)

        assertEquals(before + 1, DeveloperDiagnosticCounters.get(DiagnosticEventType.TEXT_CHANGED_TOTAL))
    }

    @Test
    fun recordPersistsADurableEventOnlyWhileDiagnosticsAreEnabled() = runBlocking {
        repository.setEnabled(false)
        repository.record(DiagnosticEventType.SESSION_PERSISTED)
        delay(300)
        val whileDisabled = repository.buildSnapshot()
        assertTrue(
            "no durable row should be written while disabled",
            whileDisabled.history.latestEvents.none { it.eventType == DiagnosticEventType.SESSION_PERSISTED.name }
        )

        repository.setEnabled(true)
        repository.record(DiagnosticEventType.SESSION_PERSISTED)
        delay(300)
        val whileEnabled = repository.buildSnapshot()
        assertTrue(
            "a durable row must be written once enabled",
            whileEnabled.history.latestEvents.any { it.eventType == DiagnosticEventType.SESSION_PERSISTED.name }
        )
    }

    @Test
    fun accessibilityAuthorizedInOverviewMatchesTheRealOsAccessibilityState() = runBlocking {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val targetId = ComponentName(context, KeystrokeAccessibilityService::class.java).flattenToShortString()
        val expected = accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id == targetId }

        val snapshot = repository.buildSnapshot()

        assertEquals(expected, snapshot.overview.accessibilityAuthorized)
    }

    @Test
    fun clearHistoryResetsInMemoryCountersAndDurableEventHistory() = runBlocking {
        repository.setEnabled(true)
        repository.record(DiagnosticEventType.TEARDOWN_BOUNDARY)
        delay(300)

        repository.clearHistory()

        assertEquals(0L, DeveloperDiagnosticCounters.get(DiagnosticEventType.TEARDOWN_BOUNDARY))
        val snapshot = repository.buildSnapshot()
        assertTrue(snapshot.history.latestEvents.none { it.eventType == DiagnosticEventType.TEARDOWN_BOUNDARY.name })
    }
}
