package com.example.vernacularguardian.ui.digitspan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vernacularguardian.model.DigitSpanResult
import kotlinx.coroutines.delay
import kotlin.random.Random


// ============================================================
// COLORS
// ============================================================

private val BackgroundColor =
    Color(0xFFFFF8FC)

private val ButtonColor =
    Color(0xFF626274)

private val CardColor =
    Color.White

private val TextColor =
    Color(0xFF30303A)


// ============================================================
// DIGIT SPAN PART
// ============================================================

private enum class DigitSpanPart {

    FORWARD,

    BACKWARD
}


// ============================================================
// DIGIT TRIAL
// ============================================================

private data class DigitTrial(
    val digits: List<Int>
)


// ============================================================
// SCREEN
// ============================================================

@Composable
fun DigitSpanScreen(
    onFinished: (DigitSpanResult) -> Unit
) {

    // ========================================================
    // INSTRUCTION SCREEN
    // ========================================================

    var showInstructions by remember {
        mutableStateOf(true)
    }


    // ========================================================
    // CURRENT PART
    // ========================================================

    var part by remember {
        mutableStateOf(
            DigitSpanPart.FORWARD
        )
    }


    // ========================================================
    // CURRENT TRIAL
    // ========================================================

    var trialIndex by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // DIGIT DISPLAY
    // ========================================================

    var showingDigits by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // CURRENT DIGIT SEQUENCE
    // ========================================================

    var currentTrial by remember {

        mutableStateOf(
            DigitTrial(
                digits =
                    generateDigits(3)
            )
        )
    }


    // ========================================================
    // USER INPUT
    // ========================================================

    var userInput by remember {
        mutableStateOf("")
    }


    // ========================================================
    // FORWARD SCORE
    // ========================================================

    var forwardCorrect by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // BACKWARD SCORE
    // ========================================================

    var backwardCorrect by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // FORWARD LONGEST SPAN
    // ========================================================

    var forwardMaxSpan by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // BACKWARD LONGEST SPAN
    // ========================================================

    var backwardMaxSpan by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // RESULT STATES
    // ========================================================

    var showPartResult by remember {
        mutableStateOf(false)
    }

    var showFinalResult by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // TOTAL TRIALS
    // ========================================================

    val totalTrials = 7


    // ========================================================
    // GENERATE / DISPLAY NEW TRIAL
    // ========================================================

    LaunchedEffect(
        part,
        trialIndex,
        showInstructions
    ) {

        if (
            !showInstructions &&
            !showFinalResult &&
            !showPartResult &&
            trialIndex < totalTrials
        ) {

            // -----------------------------------------------
            // Preserve existing sequence-length logic
            // -----------------------------------------------

            val sequenceLength =
                3 + (trialIndex / 2)


            currentTrial =
                DigitTrial(
                    digits =
                        generateDigits(
                            sequenceLength
                        )
                )


            userInput = ""

            showingDigits = true


            // -----------------------------------------------
            // Existing timing:
            // 800 ms per digit + 500 ms preparation
            // -----------------------------------------------

            val displayTime =
                (
                        currentTrial.digits.size * 800L
                        ) + 500L


            delay(displayTime)


            showingDigits = false
        }
    }


    // ========================================================
    // INSTRUCTIONS
    // ========================================================

    if (showInstructions) {

        DigitSpanInstructionScreen(

            onStart = {

                showInstructions = false

                part =
                    DigitSpanPart.FORWARD

                trialIndex = 0

                showPartResult = false

                showFinalResult = false

                userInput = ""

                showingDigits = false
            }
        )

        return
    }


    // ========================================================
    // FINAL RESULT
    // ========================================================

    if (showFinalResult) {

        DigitSpanFinalResultScreen(

            forwardSpan =
                forwardMaxSpan,

            backwardSpan =
                backwardMaxSpan,

            forwardCorrect =
                forwardCorrect,

            backwardCorrect =
                backwardCorrect,

            onContinue = {

                val result =
                    DigitSpanResult(

                        forwardLongestSpan =
                            forwardMaxSpan,

                        backwardLongestSpan =
                            backwardMaxSpan,

                        forwardCorrect =
                            forwardCorrect,

                        backwardCorrect =
                            backwardCorrect
                    )


                onFinished(result)
            }
        )

        return
    }


    // ========================================================
    // PART RESULT
    // ========================================================

    if (showPartResult) {

        val currentSpan =
            if (
                part ==
                DigitSpanPart.FORWARD
            ) {

                forwardMaxSpan

            } else {

                backwardMaxSpan
            }


        val currentCorrect =
            if (
                part ==
                DigitSpanPart.FORWARD
            ) {

                forwardCorrect

            } else {

                backwardCorrect
            }


        DigitSpanPartResultScreen(

            part =
                part,

            span =
                currentSpan,

            correct =
                currentCorrect,

            onContinue = {

                if (
                    part ==
                    DigitSpanPart.FORWARD
                ) {

                    // ----------------------------------------
                    // Forward finished.
                    // Start Backward.
                    // ----------------------------------------

                    part =
                        DigitSpanPart.BACKWARD

                    trialIndex = 0

                    showPartResult = false

                    showingDigits = false

                    userInput = ""

                } else {

                    // ----------------------------------------
                    // Both parts finished.
                    // ----------------------------------------

                    showPartResult = false

                    showFinalResult = true
                }
            }
        )

        return
    }


    // ========================================================
    // MAIN TEST SCREEN
    // ========================================================

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(35.dp)
        )


        // ====================================================
        // PART TITLE
        // ====================================================

        Text(
            text =
                if (
                    part ==
                    DigitSpanPart.FORWARD
                ) {

                    "Part A — Forward"

                } else {

                    "Part B — Backward"
                },

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                Color.Black
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(
            text =
                if (
                    part ==
                    DigitSpanPart.FORWARD
                ) {

                    "Enter the numbers in the same order."

                } else {

                    "Enter the numbers in reverse order."
                },

            fontSize =
                14.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(28.dp)
        )


        // ====================================================
        // DIGIT DISPLAY
        // ====================================================

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(90.dp),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        CardColor
                )
        ) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                if (showingDigits) {

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(18.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        currentTrial.digits.forEach {
                                digit ->

                            Text(
                                text =
                                    digit.toString(),

                                fontSize =
                                    30.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    TextColor
                            )
                        }
                    }

                } else {

                    Text(
                        text =
                            "Enter your answer",

                        fontSize =
                            16.sp,

                        color =
                            Color.Gray
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(22.dp)
        )


        // ====================================================
        // USER ANSWER
        // ====================================================

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(65.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (
                            userInput.isEmpty()
                        ) {

                            "Your answer"

                        } else {

                            userInput
                        },

                    fontSize =
                        24.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        if (
                            userInput.isEmpty()
                        ) {

                            Color.Gray

                        } else {

                            Color.Black
                        }
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(20.dp)
        )


        // ====================================================
        // NUMBER PAD
        // ====================================================

        NumberPad(

            onDigitClick = {
                    digit ->

                if (!showingDigits) {

                    if (
                        userInput.length < 12
                    ) {

                        userInput +=
                            digit.toString()
                    }
                }
            },

            onBackspaceClick = {

                if (
                    !showingDigits &&
                    userInput.isNotEmpty()
                ) {

                    userInput =
                        userInput.dropLast(1)
                }
            }
        )


        Spacer(
            modifier =
                Modifier.height(18.dp)
        )


        // ====================================================
        // SUBMIT
        // ====================================================

        Button(
            onClick = {

                if (
                    !showingDigits &&
                    userInput.isNotEmpty()
                ) {

                    val expectedAnswer =

                        if (
                            part ==
                            DigitSpanPart.FORWARD
                        ) {

                            currentTrial.digits
                                .joinToString("")

                        } else {

                            currentTrial.digits
                                .reversed()
                                .joinToString("")
                        }


                    val isCorrect =
                        userInput ==
                                expectedAnswer


                    // ----------------------------------------
                    // Record result
                    // ----------------------------------------

                    if (isCorrect) {

                        if (
                            part ==
                            DigitSpanPart.FORWARD
                        ) {

                            forwardCorrect++


                            val span =
                                currentTrial
                                    .digits
                                    .size


                            if (
                                span >
                                forwardMaxSpan
                            ) {

                                forwardMaxSpan =
                                    span
                            }

                        } else {

                            backwardCorrect++


                            val span =
                                currentTrial
                                    .digits
                                    .size


                            if (
                                span >
                                backwardMaxSpan
                            ) {

                                backwardMaxSpan =
                                    span
                            }
                        }
                    }


                    // ----------------------------------------
                    // Move to next trial
                    // ----------------------------------------

                    if (
                        trialIndex <
                        totalTrials - 1
                    ) {

                        trialIndex++

                    } else {

                        showPartResult = true
                    }
                }
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(55.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        ButtonColor
                )
        ) {

            Text(
                text =
                    "SUBMIT",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// DIGIT SPAN INSTRUCTION SCREEN
// ============================================================

@Composable
private fun DigitSpanInstructionScreen(
    onStart: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                "Digit Span",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                TextColor
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(
            text =
                "Memory and attention",

            fontSize =
                16.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


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
                        .padding(24.dp)
            ) {

                Text(
                    text =
                        "How it works",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        TextColor
                )


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                Text(
                    text =
                        "You will see a sequence of numbers " +
                                "on the screen.",

                    fontSize =
                        15.sp,

                    color =
                        Color.DarkGray
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                Text(
                    text =
                        "Part A — Forward",

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Text(
                    text =
                        "Enter the numbers in the same order " +
                                "in which they were shown.",

                    fontSize =
                        14.sp,

                    color =
                        Color.DarkGray
                )


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                Text(
                    text =
                        "Part B — Backward",

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Text(
                    text =
                        "Enter the numbers in reverse order.",

                    fontSize =
                        14.sp,

                    color =
                        Color.DarkGray
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(30.dp)
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
                        ButtonColor
                )
        ) {

            Text(
                text =
                    "START DIGIT SPAN",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// NUMBER PAD
// ============================================================

@Composable
private fun NumberPad(
    onDigitClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit
) {

    val rows =
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )


    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        rows.forEach { row ->

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {

                row.forEach { number ->

                    NumberButton(
                        text =
                            number.toString(),

                        onClick = {
                            onDigitClick(number)
                        }
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )
        }


        Row(
            horizontalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            NumberButton(
                text =
                    "⌫",

                onClick =
                    onBackspaceClick
            )


            NumberButton(
                text =
                    "0",

                onClick = {
                    onDigitClick(0)
                }
            )


            Box(
                modifier =
                    Modifier.size(62.dp)
            )
        }
    }
}


// ============================================================
// NUMBER BUTTON
// ============================================================

@Composable
private fun NumberButton(
    text: String,
    onClick: () -> Unit
) {

    Button(
        onClick =
            onClick,

        modifier =
            Modifier.size(62.dp),

        shape =
            RoundedCornerShape(15.dp),

        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    ButtonColor
            )
    ) {

        Text(
            text =
                text,

            fontSize =
                18.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}


// ============================================================
// INDIVIDUAL PART RESULT
// ============================================================

@Composable
private fun DigitSpanPartResultScreen(
    part: DigitSpanPart,
    span: Int,
    correct: Int,
    onContinue: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .padding(30.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                if (
                    part ==
                    DigitSpanPart.FORWARD
                ) {

                    "Part A Complete"

                } else {

                    "Part B Complete"
                },

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold
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

                Text(
                    text =
                        if (
                            part ==
                            DigitSpanPart.FORWARD
                        ) {

                            "Forward Results"

                        } else {

                            "Backward Results"
                        },

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                Text(
                    text =
                        "Longest Span: $span",

                    fontSize =
                        17.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Correct Trials: $correct / 7",

                    fontSize =
                        17.sp
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
                        ButtonColor
                )
        ) {

            Text(
                text =
                    if (
                        part ==
                        DigitSpanPart.FORWARD
                    ) {

                        "CONTINUE TO PART B"

                    } else {

                        "VIEW DIGIT SPAN RESULTS"
                    },

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// FINAL DIGIT SPAN RESULT
// ============================================================

@Composable
private fun DigitSpanFinalResultScreen(
    forwardSpan: Int,
    backwardSpan: Int,
    forwardCorrect: Int,
    backwardCorrect: Int,
    onContinue: () -> Unit
) {

    val combinedSpan =
        forwardSpan +
                backwardSpan


    val combinedCorrect =
        forwardCorrect +
                backwardCorrect


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .padding(30.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                "Digit Span Complete",

            fontSize =
                24.sp,

            fontWeight =
                FontWeight.Bold
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

                Text(
                    text =
                        "Digit Span Results",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Text(
                    text =
                        "Forward Longest Span: $forwardSpan",

                    fontSize =
                        16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Backward Longest Span: $backwardSpan",

                    fontSize =
                        16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Forward Correct: $forwardCorrect / 7",

                    fontSize =
                        16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Backward Correct: $backwardCorrect / 7",

                    fontSize =
                        16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                Text(
                    text =
                        "Combined Span: $combinedSpan",

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Combined Correct: " +
                                "$combinedCorrect / 14",

                    fontSize =
                        18.sp,

                    fontWeight =
                        FontWeight.Bold
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
                        ButtonColor
                )
        ) {

            Text(
                text =
                    "CONTINUE",

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// RANDOM DIGIT GENERATION
// ============================================================

private fun generateDigits(
    length: Int
): List<Int> {

    val availableDigits =
        (0..9).toMutableList()


    availableDigits.shuffle(
        Random.Default
    )


    return availableDigits.take(length)
}