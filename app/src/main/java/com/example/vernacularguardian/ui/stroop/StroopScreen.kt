package com.example.vernacularguardian.ui.stroop

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
import com.example.vernacularguardian.model.StroopResult
import kotlinx.coroutines.delay
import kotlin.random.Random


// ============================================================
// STROOP SETTINGS
// ============================================================

private const val TOTAL_TRIALS = 20

private const val STROOP_TIMEOUT_MS = 3000L


// ============================================================
// APP COLORS
// ============================================================

private val BackgroundColor =
    Color(0xFFFFF8FC)

private val CardColor =
    Color.White

private val PrimaryColor =
    Color(0xFF5C5E6D)

private val SuccessColor =
    Color(0xFF3D8F7B)


// ============================================================
// SCREEN STATES
// ============================================================

private enum class StroopScreenState {

    INSTRUCTIONS,

    TEST,

    RESULTS
}


// ============================================================
// STROOP COLORS
// ============================================================

private enum class StroopColor(
    val displayName: String,
    val color: Color
) {

    RED(
        "Red",
        Color.Red
    ),

    BLUE(
        "Blue",
        Color.Blue
    ),

    GREEN(
        "Green",
        Color.Green
    ),

    YELLOW(
        "Yellow",
        Color(0xFFFFC107)
    )
}


// ============================================================
// STROOP TRIAL
// ============================================================

private data class StroopTrial(

    val word: String,

    val inkColor: StroopColor,

    val correctAnswer: StroopColor
)


// ============================================================
// MAIN STROOP SCREEN
// ============================================================

@Composable
fun StroopScreen(
    onFinished: (StroopResult) -> Unit
) {


    // ========================================================
    // SCREEN STATE
    // ========================================================

    var screenState by remember {

        mutableStateOf(
            StroopScreenState.INSTRUCTIONS
        )
    }


    // ========================================================
    // CURRENT TRIAL INDEX
    // ========================================================

    var trialIndex by remember {

        mutableIntStateOf(0)
    }


    // ========================================================
    // SCORES
    // ========================================================

    var correctAnswers by remember {

        mutableIntStateOf(0)
    }


    var errors by remember {

        mutableIntStateOf(0)
    }


    var timeouts by remember {

        mutableIntStateOf(0)
    }


    // ========================================================
    // RESPONSE TIMES
    // ========================================================

    var totalResponseTimeNs by remember {

        mutableLongStateOf(0L)
    }


    var responseCount by remember {

        mutableIntStateOf(0)
    }


    // ========================================================
    // CURRENT TRIAL
    // ========================================================

    var currentTrial by remember {

        mutableStateOf(
            generateStroopTrial()
        )
    }


    // ========================================================
    // TRIAL START TIME
    // ========================================================

    var trialStartTimeNs by remember {

        mutableLongStateOf(0L)
    }


    // ========================================================
    // RESPONSE LOCK
    // ========================================================

    var responseLocked by remember {

        mutableStateOf(false)
    }


    // ========================================================
    // FINAL RESULT
    //
    // This is saved internally first.
    // We DO NOT immediately call onFinished().
    // ========================================================

    var finalResult by remember {

        mutableStateOf<StroopResult?>(
            null
        )
    }


    // ========================================================
    // START NEW TRIAL
    // ========================================================

    fun startTrial() {

        currentTrial =
            generateStroopTrial()

        responseLocked =
            false

        trialStartTimeNs =
            System.nanoTime()
    }


    // ========================================================
    // FINISH TEST
    //
    // IMPORTANT:
    //
    // This saves the result and opens the RESULTS SCREEN.
    //
    // It DOES NOT return to Dashboard yet.
    // ========================================================

    fun finishTest() {


        val averageMs =

            if (responseCount > 0) {

                (
                        totalResponseTimeNs /
                                responseCount
                        ) / 1_000_000L

            } else {

                0L
            }


        finalResult =

            StroopResult(

                correct =
                    correctAnswers,

                errors =
                    errors,

                timeouts =
                    timeouts,

                totalTrials =
                    TOTAL_TRIALS,

                averageResponseTimeMs =
                    averageMs
            )


        screenState =
            StroopScreenState.RESULTS
    }


    // ========================================================
    // MOVE TO NEXT TRIAL
    // ========================================================

    fun moveToNextTrial() {

        if (
            trialIndex >=
            TOTAL_TRIALS - 1
        ) {

            finishTest()

        } else {

            trialIndex++

            startTrial()
        }
    }


    // ========================================================
    // START TEST
    // ========================================================

    fun startTest() {


        trialIndex =
            0

        correctAnswers =
            0

        errors =
            0

        timeouts =
            0

        totalResponseTimeNs =
            0L

        responseCount =
            0

        finalResult =
            null


        screenState =
            StroopScreenState.TEST


        startTrial()
    }


    // ========================================================
    // TIMEOUT MONITOR
    //
    // Runs only during TEST state.
    // ========================================================

    LaunchedEffect(

        screenState,

        trialIndex
    ) {

        if (
            screenState ==
            StroopScreenState.TEST
        ) {

            delay(
                STROOP_TIMEOUT_MS
            )


            if (

                !responseLocked &&

                screenState ==
                StroopScreenState.TEST

            ) {

                responseLocked =
                    true


                timeouts++

                errors++


                moveToNextTrial()
            }
        }
    }


    // ========================================================
    // SCREEN ROUTER
    // ========================================================

    when (screenState) {


        // ====================================================
        // INSTRUCTIONS
        // ====================================================

        StroopScreenState.INSTRUCTIONS -> {

            StroopInstructionScreen(

                onStart = {

                    startTest()
                }
            )
        }


        // ====================================================
        // TEST
        // ====================================================

        StroopScreenState.TEST -> {

            StroopTestScreen(

                trialIndex =
                    trialIndex,

                currentTrial =
                    currentTrial,

                responseLocked =
                    responseLocked,

                onColorSelected = {
                        selectedColor ->


                    if (
                        responseLocked
                    ) {

                        return@StroopTestScreen
                    }


                    responseLocked =
                        true


                    val responseTime =

                        System.nanoTime() -
                                trialStartTimeNs


                    totalResponseTimeNs +=
                        responseTime


                    responseCount++


                    if (

                        selectedColor ==
                        currentTrial.correctAnswer

                    ) {

                        correctAnswers++

                    } else {

                        errors++
                    }


                    moveToNextTrial()
                }
            )
        }


        // ====================================================
        // RESULTS
        // ====================================================

        StroopScreenState.RESULTS -> {

            finalResult?.let {
                    result ->


                StroopResultScreen(

                    result =
                        result,


                    onContinue = {


                        // NOW return result to MainActivity.
                        //
                        // MainActivity will save it
                        // and return to Dashboard.

                        onFinished(
                            result
                        )
                    }
                )
            }
        }
    }
}


// ============================================================
// STROOP TEST SCREEN
// ============================================================

@Composable
private fun StroopTestScreen(

    trialIndex: Int,

    currentTrial: StroopTrial,

    responseLocked: Boolean,

    onColorSelected:
        (StroopColor) -> Unit
) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundColor
                )
                .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        // ====================================================
        // PROGRESS
        // ====================================================

        Text(

            text =
                "Trial ${trialIndex + 1} / $TOTAL_TRIALS",

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        // ====================================================
        // INSTRUCTION
        // ====================================================

        Text(

            text =
                "Select the INK COLOR of the word",

            fontSize =
                15.sp,

            color =
                Color.DarkGray
        )


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        // ====================================================
        // STIMULUS CARD
        // ====================================================

        Card(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),

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
                    Modifier.fillMaxSize(),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Text(

                    text =
                        currentTrial.word,

                    fontSize =
                        42.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        currentTrial
                            .inkColor
                            .color
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(30.dp)
        )


        // ====================================================
        // RESPONSE BUTTONS
        // ====================================================

        StroopResponseButtons(

            enabled =
                !responseLocked,

            onColorSelected =
                onColorSelected
        )


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        Text(

            text =
                "Response time limit: 3 seconds",

            fontSize =
                12.sp,

            color =
                Color.Gray
        )
    }
}


// ============================================================
// STROOP RESULT SCREEN
// ============================================================

@Composable
private fun StroopResultScreen(

    result: StroopResult,

    onContinue: () -> Unit
) {

    val answeredTrials =

        result.correct +
                result.errors -
                result.timeouts


    val accuracy =

        if (
            result.totalTrials > 0
        ) {

            (
                    result.correct.toFloat() /
                            result.totalTrials.toFloat()
                    ) * 100f

        } else {

            0f
        }


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
                "Stroop Test Results",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Text(

            text =
                "Your individual assessment result",

            fontSize =
                14.sp,

            color =
                Color.DarkGray
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
                        CardColor
                )
        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
            ) {


                ResultRow(
                    label =
                        "Correct Answers",

                    value =
                        "${result.correct}"
                )


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                ResultRow(
                    label =
                        "Errors",

                    value =
                        "${result.errors}"
                )


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                ResultRow(
                    label =
                        "Timeouts",

                    value =
                        "${result.timeouts}"
                )


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                ResultRow(
                    label =
                        "Total Trials",

                    value =
                        "${result.totalTrials}"
                )


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                ResultRow(
                    label =
                        "Accuracy",

                    value =
                        String.format(
                            "%.1f%%",
                            accuracy
                        )
                )


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                ResultRow(
                    label =
                        "Average Response Time",

                    value =
                        "${result.averageResponseTimeMs} ms"
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(28.dp)
        )


        Button(

            onClick =
                onContinue,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        SuccessColor
                )
        ) {

            Text(

                text =
                    "Continue to Dashboard",

                fontSize =
                    16.sp,

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
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
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
                16.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )
    }
}


// ============================================================
// STROOP INSTRUCTION SCREEN
// ============================================================

@Composable
private fun StroopInstructionScreen(
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
                "Stroop Test",

            fontSize =
                28.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                PrimaryColor
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(

            text =
                "Attention and cognitive flexibility",

            fontSize =
                15.sp,

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
                        "Instructions",

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                Text(
                    text =
                        "A color word will appear on the screen.",
                    fontSize =
                        15.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Ignore the meaning of the word.",
                    fontSize =
                        15.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Select the color of the INK used to display the word.",
                    fontSize =
                        15.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "You have 3 seconds to respond to each trial.",
                    fontSize =
                        15.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "There are 20 trials.",
                    fontSize =
                        15.sp
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
                        PrimaryColor
                )
        ) {

            Text(

                text =
                    "START TEST",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// RESPONSE BUTTONS
// ============================================================

@Composable
private fun StroopResponseButtons(

    enabled: Boolean,

    onColorSelected:
        (StroopColor) -> Unit
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            StroopButton(

                text =
                    "RED",

                enabled =
                    enabled,

                modifier =
                    Modifier.weight(1f),

                onClick = {

                    onColorSelected(
                        StroopColor.RED
                    )
                }
            )


            StroopButton(

                text =
                    "BLUE",

                enabled =
                    enabled,

                modifier =
                    Modifier.weight(1f),

                onClick = {

                    onColorSelected(
                        StroopColor.BLUE
                    )
                }
            )
        }


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            StroopButton(

                text =
                    "GREEN",

                enabled =
                    enabled,

                modifier =
                    Modifier.weight(1f),

                onClick = {

                    onColorSelected(
                        StroopColor.GREEN
                    )
                }
            )


            StroopButton(

                text =
                    "YELLOW",

                enabled =
                    enabled,

                modifier =
                    Modifier.weight(1f),

                onClick = {

                    onColorSelected(
                        StroopColor.YELLOW
                    )
                }
            )
        }
    }
}


// ============================================================
// NEUTRAL STROOP BUTTON
// ============================================================

@Composable
private fun StroopButton(

    text: String,

    enabled: Boolean,

    modifier: Modifier,

    onClick: () -> Unit
) {

    Button(

        onClick =
            onClick,

        enabled =
            enabled,

        modifier =
            modifier.height(52.dp),

        shape =
            RoundedCornerShape(14.dp),

        colors =
            ButtonDefaults.buttonColors(

                containerColor =
                    Color.White,

                contentColor =
                    Color.DarkGray,

                disabledContainerColor =
                    Color(0xFFF2F2F2),

                disabledContentColor =
                    Color.Gray
            )
    ) {

        Text(

            text =
                text,

            fontSize =
                14.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}


// ============================================================
// RANDOM STROOP TRIAL
// ============================================================

private fun generateStroopTrial(): StroopTrial {

    val inkColor =

        StroopColor.entries
            .toList()
            .random()


    val isCongruent =

        Random.nextBoolean()


    val wordColor =

        if (
            isCongruent
        ) {

            inkColor

        } else {

            StroopColor.entries
                .filter {

                    it !=
                            inkColor
                }
                .random()
        }


    return StroopTrial(

        word =
            wordColor.displayName,

        inkColor =
            inkColor,

        correctAnswer =
            inkColor
    )
}