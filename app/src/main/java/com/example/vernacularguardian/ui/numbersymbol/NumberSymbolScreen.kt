package com.example.vernacularguardian.ui.numbersymbol

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vernacularguardian.model.NumberSymbolResult
import kotlinx.coroutines.delay
import kotlin.random.Random


// ============================================================
// COLORS
// ============================================================

private val BackgroundColor = Color(0xFFFFF8FC)

private val PrimaryColor = Color(0xFF626274)

private val CorrectColor = Color(0xFF3C8D62)

private val ErrorColor = Color(0xFFC94C4C)


// ============================================================
// TEST SETTINGS
// ============================================================

private const val TOTAL_TRIALS = 20

private const val TEST_DURATION_MS = 60_000L


// ============================================================
// NUMBER SYMBOL KEY
// ============================================================

private val numberSymbolKey = mapOf(

    1 to "●",
    2 to "▲",
    3 to "■",
    4 to "★",
    5 to "◆",
    6 to "+",
    7 to "♥",
    8 to "○",
    9 to "✦"
)


// ============================================================
// MAIN SCREEN
// ============================================================

@Composable
fun NumberSymbolScreen(
    onFinished: (NumberSymbolResult) -> Unit
) {

    var showInstructions by remember {
        mutableStateOf(true)
    }

    var showResult by remember {
        mutableStateOf(false)
    }

    var currentTrial by remember {
        mutableIntStateOf(0)
    }

    var correct by remember {
        mutableIntStateOf(0)
    }

    var errors by remember {
        mutableIntStateOf(0)
    }

    var currentNumber by remember {
        mutableIntStateOf(1)
    }

    var options by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var testStartTime by remember {
        mutableLongStateOf(0L)
    }

    var questionStartTime by remember {
        mutableLongStateOf(0L)
    }

    var totalResponseTime by remember {
        mutableLongStateOf(0L)
    }

    var timedOut by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // TIMEOUT
    // ========================================================

    LaunchedEffect(
        testStartTime,
        showInstructions,
        showResult
    ) {

        if (
            !showInstructions &&
            !showResult &&
            testStartTime > 0L
        ) {

            while (
                !showResult
            ) {

                val elapsed =
                    System.currentTimeMillis() -
                            testStartTime

                if (
                    elapsed >= TEST_DURATION_MS
                ) {

                    timedOut = true

                    showResult = true

                    break
                }

                delay(100)
            }
        }
    }


    // ========================================================
    // INSTRUCTIONS
    // ========================================================

    if (showInstructions) {

        NumberSymbolInstructionScreen(

            onStart = {

                currentTrial = 0

                correct = 0

                errors = 0

                totalResponseTime = 0L

                timedOut = false

                currentNumber =
                    Random.nextInt(
                        1,
                        10
                    )

                options =
                    generateOptions(
                        currentNumber
                    )

                testStartTime =
                    System.currentTimeMillis()

                questionStartTime =
                    System.currentTimeMillis()

                showInstructions = false
            }
        )

        return
    }


    // ========================================================
    // RESULT SCREEN
    // ========================================================

    if (showResult) {

        val totalTime =
            System.currentTimeMillis() -
                    testStartTime


        val averageResponseTime =
            if (currentTrial > 0) {

                totalResponseTime /
                        currentTrial

            } else {

                0L
            }


        NumberSymbolResultScreen(

            correct = correct,

            errors = errors,

            totalTrials = currentTrial,

            averageResponseTimeMs =
                averageResponseTime,

            totalTimeMs =
                totalTime,

            timedOut = timedOut,

            onContinue = {

                val result =
                    NumberSymbolResult(

                        correct =
                            correct,

                        errors =
                            errors,

                        totalTrials =
                            currentTrial,

                        averageResponseTimeMs =
                            averageResponseTime,

                        totalTimeMs =
                            totalTime,

                        timedOut =
                            timedOut
                    )


                // IMPORTANT:
                //
                // MainActivity receives this only
                // after the user presses CONTINUE
                // on the result screen.

                onFinished(result)
            }
        )

        return
    }


    // ========================================================
    // MAIN TEST SCREEN
    // ========================================================

    NumberSymbolTestScreen(

        currentTrial =
            currentTrial,

        currentNumber =
            currentNumber,

        options =
            options,

        correct =
            correct,

        errors =
            errors,

        onAnswer = { selectedSymbol ->

            val responseTime =
                System.currentTimeMillis() -
                        questionStartTime


            totalResponseTime +=
                responseTime


            val correctSymbol =
                numberSymbolKey[
                    currentNumber
                ]


            if (
                selectedSymbol ==
                correctSymbol
            ) {

                correct++

            } else {

                errors++
            }


            currentTrial++


            // ================================================
            // TEST COMPLETE
            // ================================================

            if (
                currentTrial >=
                TOTAL_TRIALS
            ) {

                showResult = true

            } else {

                currentNumber =
                    Random.nextInt(
                        1,
                        10
                    )

                options =
                    generateOptions(
                        currentNumber
                    )

                questionStartTime =
                    System.currentTimeMillis()
            }
        }
    )
}


// ============================================================
// INSTRUCTION SCREEN
// ============================================================

@Composable
private fun NumberSymbolInstructionScreen(
    onStart: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                "Number Symbol Match",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Text(
            text =
                "Processing speed and visual-symbol association",

            fontSize =
                15.sp,

            color =
                Color.DarkGray,

            textAlign =
                TextAlign.Center
        )


        Spacer(
            modifier =
                Modifier.height(24.dp)
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(22.dp)
            ) {

                Text(
                    text =
                        "How it works",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )


                Text(
                    text =
                        "Use the key below to identify " +
                                "which symbol matches the number.",

                    fontSize =
                        14.sp,

                    color =
                        Color.DarkGray
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                numberSymbolKey.forEach {

                        (number, symbol) ->

                    Text(
                        text =
                            "$number  →  $symbol",

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Text(
                    text =
                        "You will answer up to 20 questions. " +
                                "The test has a maximum duration of 60 seconds.",

                    fontSize =
                        14.sp,

                    color =
                        Color.DarkGray
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(28.dp)
        )


        Button(
            onClick =
                onStart,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(55.dp),

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
                    "START TEST",

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// TEST SCREEN
// ============================================================

@Composable
private fun NumberSymbolTestScreen(

    currentTrial: Int,

    currentNumber: Int,

    options: List<String>,

    correct: Int,

    errors: Int,

    onAnswer: (
        String
    ) -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        Text(
            text =
                "Number Symbol Match",

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(
            text =
                "Question ${currentTrial + 1} / $TOTAL_TRIALS",

            fontSize =
                15.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(40.dp)
        )


        Text(
            text =
                currentNumber.toString(),

            fontSize =
                72.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )


        Spacer(
            modifier =
                Modifier.height(18.dp)
        )


        Text(
            text =
                "Choose the matching symbol",

            fontSize =
                17.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(35.dp)
        )


        options.chunked(2).forEach {

                rowOptions ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        16.dp
                    )
            ) {

                rowOptions.forEach {

                        symbol ->

                    Button(
                        onClick = {

                            onAnswer(
                                symbol
                            )
                        },

                        modifier =
                            Modifier
                                .weight(1f)
                                .height(75.dp),

                        shape =
                            RoundedCornerShape(16.dp),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color.White,

                                contentColor =
                                    Color.Black
                            )
                    ) {

                        Text(
                            text =
                                symbol,

                            fontSize =
                                32.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )
        }


        Spacer(
            modifier =
                Modifier.weight(1f)
        )


        Text(
            text =
                "Correct: $correct    Errors: $errors",

            fontSize =
                14.sp,

            color =
                Color.DarkGray
        )
    }
}


// ============================================================
// RESULT SCREEN
// ============================================================

@Composable
private fun NumberSymbolResultScreen(

    correct: Int,

    errors: Int,

    totalTrials: Int,

    averageResponseTimeMs: Long,

    totalTimeMs: Long,

    timedOut: Boolean,

    onContinue: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(30.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                "Number Symbol Match Complete",

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold,

            textAlign =
                TextAlign.Center
        )


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(24.dp)
            ) {

                ResultRow(
                    label =
                        "Correct Answers",

                    value =
                        "$correct / $totalTrials"
                )


                ResultRow(
                    label =
                        "Errors",

                    value =
                        errors.toString()
                )


                ResultRow(
                    label =
                        "Average Response Time",

                    value =
                        formatTime(
                            averageResponseTimeMs
                        )
                )


                ResultRow(
                    label =
                        "Total Time",

                    value =
                        formatTime(
                            totalTimeMs
                        )
                )


                ResultRow(
                    label =
                        "Status",

                    value =
                        if (timedOut) {
                            "Time Expired"
                        } else {
                            "Completed"
                        }
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(30.dp)
        )


        Button(
            onClick =
                onContinue,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(55.dp),

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
                    "CONTINUE TO DASHBOARD",

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// RESULT ROW
// ============================================================

@Composable
private fun ResultRow(

    label: String,

    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        8.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,

            fontSize =
                15.sp,

            color =
                Color.DarkGray
        )


        Text(
            text =
                value,

            fontSize =
                15.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}


// ============================================================
// GENERATE OPTIONS
// ============================================================

private fun generateOptions(
    number: Int
): List<String> {

    val correctSymbol =
        numberSymbolKey[number]
            ?: "?"


    val incorrectSymbols =
        numberSymbolKey
            .filter {

                    entry ->

                entry.value !=
                        correctSymbol
            }
            .values
            .shuffled()
            .take(3)


    return (
            incorrectSymbols +
                    correctSymbol
            )
        .shuffled()
}


// ============================================================
// FORMAT TIME
// ============================================================

private fun formatTime(
    milliseconds: Long
): String {

    val seconds =
        milliseconds / 1000


    val remainingMilliseconds =
        milliseconds % 1000


    return String.format(
        "%d.%03d s",

        seconds,

        remainingMilliseconds
    )
}