package com.example.vga.cognitive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import com.example.vga.insight.CognitiveResultStore
import com.example.vga.insight.CognitiveSnapshot

import com.example.vernacularguardian.model.AssessmentResult
import com.example.vernacularguardian.model.DigitSpanResult
import com.example.vernacularguardian.model.StroopResult
import com.example.vernacularguardian.model.TrailMakingResult

import com.example.vernacularguardian.ui.dashboard.DashboardScreen
import com.example.vernacularguardian.ui.digitspan.DigitSpanScreen
import com.example.vernacularguardian.ui.results.CombinedResultsScreen
import com.example.vernacularguardian.ui.stroop.StroopScreen
import com.example.vernacularguardian.ui.trailmaking.TrailMakingScreen


// ============================================================
// COGNITIVE TEST SCREENS
// ============================================================

private enum class CognitiveScreen {

    DASHBOARD,

    STROOP,

    DIGIT_SPAN,

    TRAIL_MAKING,

    COMBINED_RESULTS
}


/**
 * VGA-side entry point for the Cognitive Tests module.
 *
 * Reproduces the cognitive module's own screen controller (test selection ->
 * individual assessment -> back to selection, plus combined results once all
 * three are done), adding one thing VGA needs that the standalone app did not:
 * an [onBack] out of the test-selection screen back to VGA's Main Dashboard.
 *
 * Each assessment screen owns its own instructions / active test / results
 * stages internally, so this controller only routes between them and holds the
 * accumulated [AssessmentResult]. No test logic lives here.
 */
@Composable
fun CognitiveTestsEntry(
    onBack: () -> Unit
) {


    // ========================================================
    // CURRENT SCREEN
    // ========================================================

    var currentScreen by remember {

        mutableStateOf(
            CognitiveScreen.DASHBOARD
        )
    }


    // ========================================================
    // ASSESSMENT RESULTS
    // ========================================================

    val context = LocalContext.current

    var assessmentResult by remember {

        mutableStateOf(
            AssessmentResult()
        )
    }


    /**
     * Mirrors completed results into VGA's own store so the linguistic-insight
     * fusion step can read them. The cognitive module keeps its results in
     * memory only; this writes a copy without touching its internals.
     */
    fun persistResults(result: AssessmentResult) {

        val stroop = result.stroop
        val digitSpan = result.digitSpan
        val trail = result.trailMaking

        CognitiveResultStore.save(
            context = context,
            snapshot = CognitiveSnapshot(
                stroopAccuracyPercent =
                    stroop?.let {
                        if (it.totalTrials > 0) {
                            it.correct * 100.0 / it.totalTrials
                        } else {
                            null
                        }
                    },
                stroopAvgResponseMs = stroop?.averageResponseTimeMs,
                digitSpanForward = digitSpan?.forwardLongestSpan,
                digitSpanBackward = digitSpan?.backwardLongestSpan,
                trailMakingPartAMs = trail?.partATimeMs,
                trailMakingPartBMs = trail?.partBTimeMs
            )
        )
    }


    // ========================================================
    // ANDROID BACK BUTTON
    //
    // Inside a test, back returns to the test selection screen.
    // On the selection screen itself, back leaves the Cognitive
    // Tests module and returns to VGA's Main Dashboard.
    // ========================================================

    BackHandler(
        enabled =
            currentScreen !=
                    CognitiveScreen.DASHBOARD
    ) {

        currentScreen =
            CognitiveScreen.DASHBOARD
    }


    // ========================================================
    // SCREEN NAVIGATION
    //
    // The cognitive screens were written for a non-edge-to-edge
    // host activity, so system-bar insets are applied once here
    // rather than editing each individual test screen. The
    // background matches the screens' own canvas so the inset
    // strip is not visible as a seam.
    // ========================================================

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFFFF8FC))
                .systemBarsPadding()
    ) {

    when (currentScreen) {


        // ====================================================
        // TEST SELECTION
        // ====================================================

        CognitiveScreen.DASHBOARD -> {

            DashboardScreen(

                stroopCompleted =
                    assessmentResult.stroop != null,

                digitSpanCompleted =
                    assessmentResult.digitSpan != null,

                trailMakingCompleted =
                    assessmentResult.trailMaking != null,

                onStroopClick = {

                    currentScreen =
                        CognitiveScreen.STROOP
                },

                onDigitSpanClick = {

                    currentScreen =
                        CognitiveScreen.DIGIT_SPAN
                },

                onTrailMakingClick = {

                    currentScreen =
                        CognitiveScreen.TRAIL_MAKING
                },

                onViewResultsClick = {

                    currentScreen =
                        CognitiveScreen.COMBINED_RESULTS
                },

                onBack = onBack
            )
        }


        // ====================================================
        // STROOP TEST
        // ====================================================

        CognitiveScreen.STROOP -> {

            StroopScreen(

                onFinished = {

                        result: StroopResult ->

                    assessmentResult =
                        assessmentResult.copy(
                            stroop = result
                        )

                    persistResults(assessmentResult)

                    currentScreen =
                        CognitiveScreen.DASHBOARD
                }
            )
        }


        // ====================================================
        // DIGIT SPAN TEST
        // ====================================================

        CognitiveScreen.DIGIT_SPAN -> {

            DigitSpanScreen(

                onFinished = {

                        result: DigitSpanResult ->

                    assessmentResult =
                        assessmentResult.copy(
                            digitSpan = result
                        )

                    persistResults(assessmentResult)

                    currentScreen =
                        CognitiveScreen.DASHBOARD
                }
            )
        }


        // ====================================================
        // TRAIL MAKING TEST
        // ====================================================

        CognitiveScreen.TRAIL_MAKING -> {

            TrailMakingScreen(

                onFinished = {

                        result: TrailMakingResult ->

                    assessmentResult =
                        assessmentResult.copy(
                            trailMaking = result
                        )

                    persistResults(assessmentResult)

                    currentScreen =
                        CognitiveScreen.DASHBOARD
                }
            )
        }


        // ====================================================
        // COMBINED RESULTS
        // ====================================================

        CognitiveScreen.COMBINED_RESULTS -> {

            CombinedResultsScreen(

                result = assessmentResult,

                onFinish = {

                    currentScreen =
                        CognitiveScreen.DASHBOARD
                }
            )
        }
    }
    }
}
