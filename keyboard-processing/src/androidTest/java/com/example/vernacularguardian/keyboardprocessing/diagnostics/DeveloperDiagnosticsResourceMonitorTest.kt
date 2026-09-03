package com.example.vernacularguardian.keyboardprocessing.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 8 Section 34 items 6-8: memory/battery/thermal resource-adapter
 * behavior against the real Android runtime on the connected device/
 * emulator. Section 38 requirement: must never crash regardless of which of
 * these APIs the device/API level actually supports.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperDiagnosticsResourceMonitorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun captureSampleNeverCrashesAndReturnsAWellFormedSample() {
        val monitor = DeveloperDiagnosticsResourceMonitor(context)

        val sample = monitor.captureSample()

        assertNotNull(sample)
        assertTrue(sample.timestampEpochMs > 0L)
        assertTrue(sample.wallClockElapsedMs >= 0L)
    }

    @Test
    fun memoryReadingIsPresentAndNonNegativeWhenAvailable() {
        val monitor = DeveloperDiagnosticsResourceMonitor(context)

        val sample = monitor.captureSample()

        // totalPssKb is expected to be available on every real device/emulator;
        // it is either a positive reading or (gracefully) zero, never negative.
        assertTrue(sample.totalPssKb >= 0)
    }

    @Test
    fun batteryPercentIsEitherUnavailableOrWithinZeroToOneHundred() {
        val monitor = DeveloperDiagnosticsResourceMonitor(context)

        val sample = monitor.captureSample()

        sample.batteryPercent?.let { percent ->
            assertTrue(percent in 0..100)
        }
        // null (unavailable) is an equally acceptable, honestly-reported outcome - never a crash.
    }

    @Test
    fun thermalStatusGracefullyDegradesInsteadOfCrashing() {
        val monitor = DeveloperDiagnosticsResourceMonitor(context)

        // Below API 29 (or if the API is otherwise unsupported) this must be
        // null, not a thrown exception - the call completing at all (never
        // throwing) is the assertion.
        val sample = monitor.captureSample()
        assertNotNull(sample)
    }

    @Test
    fun cpuUtilizationBecomesComputableAcrossTwoConsecutiveSamples() {
        val monitor = DeveloperDiagnosticsResourceMonitor(context)

        monitor.captureSample() // establishes the first data point
        Thread.sleep(20)
        val second = monitor.captureSample()

        // Either genuinely unavailable on this API level (documented, not a
        // crash) or a non-negative percentage - never a fabricated value.
        second.cpuUtilizationPercent?.let { utilization ->
            assertTrue(utilization >= 0.0)
        }
    }

    @Test
    fun processUptimeIsNonNegativeWhenAvailable() {
        val monitor = DeveloperDiagnosticsResourceMonitor(context)

        val sample = monitor.captureSample()

        sample.processCpuTimeMs // access only to prove no crash reading it
        // processUptimeMs itself is set via Process.getStartElapsedRealtime(),
        // available on this module's minSdk 24 - must be non-negative when present.
        sample.processUptimeMs?.let { assertTrue(it >= 0L) }
    }
}
