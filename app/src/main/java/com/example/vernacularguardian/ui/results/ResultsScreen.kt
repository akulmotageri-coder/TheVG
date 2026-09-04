package com.example.vernacularguardian.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.Locale


private val BackgroundColor =
    Color(0xFFFFF8FC)

private val PrimaryColor =
    Color(0xFF5C5E6D)

private val CardColor =
    Color.White

private val SuccessBackground =
    Color(0xFFE8F7F3)


@Composable
fun ResultsScreen(
    correctAnswers: Int,
    totalQuestions: Int,
    averageResponseTime: Double,
    timeouts: Int = 0,
    onContinue: () -> Unit,
    onTakeAgain: () -> Unit
) {

    val errors =
        (totalQuestions -
                correctAnswers)
            .coerceAtLeast(0)


    val accuracy =
        if (
            totalQuestions > 0
        ) {

            correctAnswers
                .toDouble()
                .div(
                    totalQuestions.toDouble()
                )
                .times(100.0)

        } else {

            0.0
        }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Top
    ) {

        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        Text(
            text =
                "Stroop Test Result",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        Text(
            text =
                "Your Stroop Test performance",

            fontSize =
                14.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(22.dp)
        )


        // ====================================================
        // ACCURACY
        // ====================================================

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        CardColor
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        "${accuracy.toInt()}%",

                    fontSize =
                        46.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        PrimaryColor
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Accuracy",

                    fontSize =
                        14.sp,

                    color =
                        Color.Gray
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(14.dp)
        )


        // ====================================================
        // CORRECT / ERRORS
        // ====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            SmallResultCard(
                modifier =
                    Modifier.weight(1f),

                title =
                    "Correct",

                value =
                    "$correctAnswers"
            )


            SmallResultCard(
                modifier =
                    Modifier.weight(1f),

                title =
                    "Errors",

                value =
                    "$errors"
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // ====================================================
        // TIMEOUT
        // ====================================================

        SmallResultCard(
            modifier =
                Modifier.fillMaxWidth(),

            title =
                "Timeouts",

            value =
                "$timeouts"
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        // ====================================================
        // RESPONSE TIME
        // ====================================================

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        SuccessBackground
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
            ) {

                Text(
                    text =
                        "Average Response Time",

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Medium
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Text(
                    text =
                        String.format(
                            Locale.US,
                            "%.3f s",
                            averageResponseTime
                        ),

                    fontSize =
                        25.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        PrimaryColor
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(
            text =
                "Total Trials: $totalQuestions",

            fontSize =
                14.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Text(
            text =
                "Timeout limit: 3 seconds per trial",

            fontSize =
                12.sp,

            color =
                Color.Gray
        )


        Spacer(
            modifier =
                Modifier.height(18.dp)
        )


        Text(
            text =
                "This tool is for educational and screening " +
                        "purposes and is not a medical diagnosis.",

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
        // CONTINUE
        // ====================================================

        Button(
            onClick =
                onContinue,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),

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
                    "CONTINUE",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Button(
            onClick =
                onTakeAgain,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp),

            shape =
                RoundedCornerShape(16.dp)
        ) {

            Text(
                text =
                    "TAKE STROOP AGAIN",

                fontSize =
                    14.sp
            )
        }
    }
}


// ============================================================
// SMALL RESULT CARD
// ============================================================

@Composable
private fun SmallResultCard(
    modifier: Modifier,
    title: String,
    value: String
) {

    Card(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardColor
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
                    13.sp,

                color =
                    Color.Gray
            )


            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )


            Text(
                text =
                    value,

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    PrimaryColor
            )
        }
    }
}