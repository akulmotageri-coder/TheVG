package com.example.vernacularguardian.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vernacularguardian.model.AssessmentResult
import java.util.Locale


// ============================================================
// COLORS
// ============================================================

private val BackgroundColor =
    Color(0xFFFFF8FC)

private val PrimaryColor =
    Color(0xFF5C5E6D)

private val CardColor =
    Color.White

private val SectionColor =
    Color(0xFFF7F3F7)


// ============================================================
// COMBINED RESULTS SCREEN
// ============================================================

@Composable
fun CombinedResultsScreen(
    result: AssessmentResult,
    onFinish: () -> Unit
) {

    val stroop =
        result.stroop

    val digitSpan =
        result.digitSpan

    val trailMaking =
        result.trailMaking


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // ====================================================
        // HEADER
        // ====================================================

        Text(
            text =
                "Assessment Results",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )


        Spacer(
            modifier =
                Modifier.height(6.dp)
        )


        Text(
            text =
                "Combined summary of your completed assessments.",

            fontSize =
                14.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // ====================================================
        // STROOP
        // ====================================================

        ResultSectionCard(
            title =
                "1. Stroop Test"
        ) {

            if (stroop != null) {

                val accuracy =
                    if (
                        stroop.totalTrials > 0
                    ) {

                        stroop.correct
                            .toDouble()
                            .div(
                                stroop.totalTrials
                                    .toDouble()
                            )
                            .times(100.0)

                    } else {

                        0.0
                    }


                ResultText(
                    text =
                        String.format(
                            Locale.US,
                            "Accuracy: %.1f%%",
                            accuracy
                        ),

                    bold = true
                )


                ResultText(
                    text =
                        "Correct: " +
                                "${stroop.correct} / " +
                                "${stroop.totalTrials}"
                )


                ResultText(
                    text =
                        "Errors: " +
                                stroop.errors
                )


                ResultText(
                    text =
                        "Average Response Time: " +
                                formatMilliseconds(
                                    stroop.averageResponseTimeMs
                                )
                )

            } else {

                NotCompletedText()
            }
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // ====================================================
        // DIGIT SPAN
        // ====================================================

        ResultSectionCard(
            title =
                "2. Digit Span"
        ) {

            if (digitSpan != null) {

                ResultText(
                    text =
                        "Forward Longest Span: " +
                                digitSpan.forwardLongestSpan
                )


                ResultText(
                    text =
                        "Backward Longest Span: " +
                                digitSpan.backwardLongestSpan
                )


                ResultText(
                    text =
                        "Forward Correct: " +
                                digitSpan.forwardCorrect
                )


                ResultText(
                    text =
                        "Backward Correct: " +
                                digitSpan.backwardCorrect
                )

            } else {

                NotCompletedText()
            }
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // ====================================================
        // TRAIL MAKING
        // ====================================================

        ResultSectionCard(
            title =
                "3. Trail Making"
        ) {

            if (trailMaking != null) {

                ResultText(
                    text =
                        "Part A Time: " +
                                formatMilliseconds(
                                    trailMaking.partATimeMs
                                )
                )


                ResultText(
                    text =
                        "Part A Errors: " +
                                trailMaking.partAErrors
                )


                ResultText(
                    text =
                        "Part B Time: " +
                                formatMilliseconds(
                                    trailMaking.partBTimeMs
                                )
                )


                ResultText(
                    text =
                        "Part B Errors: " +
                                trailMaking.partBErrors
                )

            } else {

                NotCompletedText()
            }
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // ====================================================
        // DISCLAIMER
        // ====================================================

        Text(
            text =
                "This assessment is for educational and " +
                        "screening purposes and is not a medical diagnosis.",

            fontSize =
                11.sp,

            color =
                Color.Gray
        )


        Spacer(
            modifier =
                Modifier.height(18.dp)
        )


        // ====================================================
        // FINISH
        // ====================================================

        Button(
            onClick =
                onFinish,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        PrimaryColor
                )
        ) {

            Text(
                text =
                    "FINISH",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )
    }
}


// ============================================================
// RESULT SECTION CARD
// ============================================================

@Composable
private fun ResultSectionCard(
    title: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SectionColor
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            Text(
                text =
                    title,

                fontSize =
                    18.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    PrimaryColor
            )


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            content()
        }
    }
}


// ============================================================
// RESULT TEXT
// ============================================================

@Composable
private fun ResultText(
    text: String,
    bold: Boolean = false
) {

    Text(
        text =
            text,

        fontSize =
            14.sp,

        fontWeight =
            if (bold) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },

        color =
            Color.DarkGray,

        modifier =
            Modifier.padding(
                vertical = 3.dp
            )
    )
}


// ============================================================
// NOT COMPLETED
// ============================================================

@Composable
private fun NotCompletedText() {

    Text(
        text =
            "Not completed",

        fontSize =
            14.sp,

        color =
            Color.Gray
    )
}


// ============================================================
// TIME FORMAT
// ============================================================

private fun formatMilliseconds(
    milliseconds: Long
): String {

    val seconds =
        milliseconds / 1000

    val remaining =
        milliseconds % 1000


    return String.format(
        Locale.US,
        "%d.%03d s",
        seconds,
        remaining
    )
}