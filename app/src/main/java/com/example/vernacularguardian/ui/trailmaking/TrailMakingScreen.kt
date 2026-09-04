package com.example.vernacularguardian.ui.trailmaking

import com.example.vernacularguardian.model.TrailMakingResult

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random


// ============================================================
// COLORS
// ============================================================

private val BackgroundColor = Color(0xFFFFF8FC)
private val CircleColor = Color(0xFF626274)
private val CompletedColor = Color(0xFF3C8D62)
private val LineColor = Color(0xFF3C8D62)
private val ErrorColor = Color(0xFFC94C4C)
private val TextColor = Color.White


// ============================================================
// BOARD SETTINGS
// ============================================================

private val CircleSize = 56.dp
private val CircleRadius = 28.dp

private val MinimumDistance = 64f

private val MinimumAxisGap = 12f


// ============================================================
// TEST PART
// ============================================================

private enum class TrailPart {
    PART_A,
    PART_B
}


// ============================================================
// TRAIL POINT
// ============================================================

private data class TrailPoint(
    val id: Int,
    val label: String,
    val x: Dp,
    val y: Dp,
    val isNumber: Boolean
)


// ============================================================
// MAIN SCREEN
// ============================================================

@Composable
fun TrailMakingScreen(
    onFinished: (TrailMakingResult) -> Unit
) {

    // ========================================================
    // INSTRUCTION STATE
    // ========================================================

    var showInstructions by remember {
        mutableStateOf(true)
    }


    // ========================================================
    // CURRENT PART
    // ========================================================

    var part by remember {
        mutableStateOf(TrailPart.PART_A)
    }


    // ========================================================
    // CURRENT EXPECTED POINT
    // ========================================================

    var currentIndex by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // ERRORS
    // ========================================================

    var errors by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // START TIME
    // ========================================================

    var startTime by remember {
        mutableLongStateOf(0L)
    }


    // ========================================================
    // PART A RESULT
    // ========================================================

    var partATime by remember {
        mutableLongStateOf(0L)
    }

    var partAErrors by remember {
        mutableIntStateOf(0)
    }


    // ========================================================
    // PART B RESULT
    // ========================================================

    var partBTime by remember {
        mutableLongStateOf(0L)
    }

    var partBErrors by remember {
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
    // BOARD POINTS
    // ========================================================

    var trailPoints by remember {
        mutableStateOf<List<TrailPoint>>(emptyList())
    }


    // ========================================================
    // INSTRUCTIONS
    // ========================================================

    if (showInstructions) {

        TrailMakingInstructionScreen(
            onStart = {

                showInstructions = false

                part = TrailPart.PART_A

                currentIndex = 0

                errors = 0

                trailPoints = emptyList()

                showPartResult = false

                showFinalResult = false
            }
        )

        return
    }


    // ========================================================
    // FINAL RESULT
    // ========================================================

    if (showFinalResult) {

        TrailFinalResultScreen(
            partATime = partATime,
            partBTime = partBTime,
            partAErrors = partAErrors,
            partBErrors = partBErrors,

            onContinue = {

                val result = TrailMakingResult(
                    partATimeMs = partATime,
                    partBTimeMs = partBTime,
                    partAErrors = partAErrors,
                    partBErrors = partBErrors
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

        TrailPartResultScreen(
            part = part,

            time = if (part == TrailPart.PART_A) {
                partATime
            } else {
                partBTime
            },

            errors = if (part == TrailPart.PART_A) {
                partAErrors
            } else {
                partBErrors
            },

            onContinue = {

                if (part == TrailPart.PART_A) {

                    part = TrailPart.PART_B

                    currentIndex = 0

                    errors = 0

                    trailPoints = emptyList()

                    showPartResult = false

                } else {

                    showPartResult = false

                    showFinalResult = true
                }
            }
        )

        return
    }


    // ========================================================
    // DENSITY
    // ========================================================

    val density = LocalDensity.current


    // ========================================================
    // MAIN UI
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(
                horizontal = 20.dp,
                vertical = 18.dp
            )
    ) {

        Spacer(
            modifier = Modifier.height(10.dp)
        )


        // ====================================================
        // TITLE
        // ====================================================

        Text(
            text = if (part == TrailPart.PART_A) {
                "Part A — Numbers"
            } else {
                "Part B — Numbers + Letters"
            },

            modifier = Modifier.fillMaxWidth(),

            fontSize = 22.sp,

            fontWeight = FontWeight.Bold,

            color = Color.Black
        )


        Spacer(
            modifier = Modifier.height(7.dp)
        )


        // ====================================================
        // INSTRUCTION
        // ====================================================

        Text(
            text = if (part == TrailPart.PART_A) {
                "Connect the numbers in ascending order."
            } else {
                "Connect numbers and letters alternately."
            },

            modifier = Modifier.fillMaxWidth(),

            fontSize = 13.sp,

            color = Color.DarkGray
        )


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // ====================================================
        // STATUS
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = if (
                    currentIndex < trailPoints.size
                ) {
                    "Next: ${trailPoints[currentIndex].label}"
                } else {
                    "Complete"
                },

                fontSize = 14.sp,

                fontWeight = FontWeight.Bold
            )


            Text(
                text = "Errors: $errors",

                fontSize = 14.sp,

                color = if (errors > 0) {
                    ErrorColor
                } else {
                    Color.DarkGray
                }
            )
        }


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        // ====================================================
        // BOARD
        // ====================================================

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .background(Color.White)
        ) {

            // ------------------------------------------------
            // GENERATE BOARD
            // ------------------------------------------------

            LaunchedEffect(
                part,
                maxWidth,
                maxHeight
            ) {

                delay(50)

                trailPoints =
                    generateTrailPoints(
                        part = part,
                        width = maxWidth,
                        height = maxHeight
                    )

                currentIndex = 0

                errors = 0

                startTime =
                    System.currentTimeMillis()
            }


            // ------------------------------------------------
            // CONNECTING LINES + TOUCH DETECTION
            // ------------------------------------------------

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        trailPoints,
                        currentIndex,
                        part
                    ) {

                        detectTapGestures { tapPosition ->

                            if (trailPoints.isEmpty()) {
                                return@detectTapGestures
                            }


                            // --------------------------------
                            // Convert pixels to dp
                            // --------------------------------

                            val tapX =
                                tapPosition.x /
                                        density.density

                            val tapY =
                                tapPosition.y /
                                        density.density


                            // --------------------------------
                            // Find clicked point
                            // --------------------------------

                            val clickedPoint =
                                trailPoints.firstOrNull { point ->

                                    val dx =
                                        tapX -
                                                point.x.value

                                    val dy =
                                        tapY -
                                                point.y.value

                                    hypot(
                                        dx.toDouble(),
                                        dy.toDouble()
                                    ) <=
                                            CircleRadius.value
                                }


                            if (clickedPoint == null) {
                                return@detectTapGestures
                            }


                            // --------------------------------
                            // Expected point
                            // --------------------------------

                            val expectedPoint =
                                trailPoints.getOrNull(
                                    currentIndex
                                )
                                    ?: return@detectTapGestures


                            // =================================
                            // CORRECT
                            // =================================

                            if (
                                clickedPoint.id ==
                                expectedPoint.id
                            ) {

                                // --------------------------------
                                // Last point
                                // --------------------------------

                                if (
                                    currentIndex ==
                                    trailPoints.lastIndex
                                ) {

                                    val elapsed =
                                        System.currentTimeMillis() -
                                                startTime


                                    if (
                                        part ==
                                        TrailPart.PART_A
                                    ) {

                                        partATime =
                                            elapsed

                                        partAErrors =
                                            errors

                                    } else {

                                        partBTime =
                                            elapsed

                                        partBErrors =
                                            errors
                                    }


                                    showPartResult = true

                                } else {

                                    currentIndex++
                                }

                            } else {

                                // =================================
                                // WRONG CLICK
                                // =================================

                                errors++
                            }
                        }
                    }
            ) {

                // ------------------------------------------------
                // DRAW COMPLETED CONNECTIONS
                // ------------------------------------------------

                if (currentIndex > 0) {

                    for (
                    i in 0 until currentIndex
                    ) {

                        val first =
                            trailPoints[i]

                        val second =
                            trailPoints[i + 1]


                        drawLine(
                            color = LineColor,

                            start = Offset(
                                x = first.x.toPx(),
                                y = first.y.toPx()
                            ),

                            end = Offset(
                                x = second.x.toPx(),
                                y = second.y.toPx()
                            ),

                            strokeWidth =
                                5.dp.toPx()
                        )
                    }
                }
            }


            // =================================================
            // DRAW CIRCLES
            // =================================================

            trailPoints.forEach { point ->

                val isCompleted =
                    point.id < currentIndex


                val circleColor =
                    if (isCompleted) {
                        CompletedColor
                    } else {
                        CircleColor
                    }


                Box(
                    modifier = Modifier
                        .offset(
                            x =
                                point.x -
                                        CircleRadius,

                            y =
                                point.y -
                                        CircleRadius
                        )
                        .size(CircleSize)
                        .background(
                            color = circleColor,
                            shape = CircleShape
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = point.label,

                        fontSize =
                            if (
                                point.label.length > 1
                            ) {
                                13.sp
                            } else {
                                16.sp
                            },

                        fontWeight =
                            FontWeight.Bold,

                        color = TextColor
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        // ----------------------------------------------------
        // PROGRESS
        // ----------------------------------------------------

        Text(
            text =
                "$currentIndex / ${trailPoints.size}",

            modifier =
                Modifier.fillMaxWidth(),

            fontSize = 13.sp,

            color = Color.Gray
        )
    }
}


// ============================================================
// TRAIL MAKING INSTRUCTION SCREEN
// ============================================================

@Composable
private fun TrailMakingInstructionScreen(
    onStart: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "Trail Making",

            fontSize = 28.sp,

            fontWeight = FontWeight.Bold,

            color = Color(0xFF30303A)
        )


        Spacer(
            modifier = Modifier.height(10.dp)
        )


        Text(
            text = "Visual attention and processing speed",

            fontSize = 15.sp,

            color = Color.DarkGray
        )


        Spacer(
            modifier = Modifier.height(25.dp)
        )


        Card(
            modifier = Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
            ) {

                Text(
                    text = "How it works",

                    fontSize = 20.sp,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Text(
                    text = "Part A — Numbers",

                    fontSize = 17.sp,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                Text(
                    text =
                        "Connect the numbered circles in " +
                                "ascending order: 1, 2, 3 and so on.",

                    fontSize = 14.sp,

                    color = Color.DarkGray
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Text(
                    text =
                        "Part B — Numbers + Letters",

                    fontSize = 17.sp,

                    fontWeight = FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                Text(
                    text =
                        "Connect the circles by alternating " +
                                "numbers and letters: 1, A, 2, B, 3, C and so on.",

                    fontSize = 14.sp,

                    color = Color.DarkGray
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Text(
                    text =
                        "The circles are displayed in a " +
                                "scattered arrangement. Follow the required sequence.",

                    fontSize = 14.sp,

                    color = Color.DarkGray
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(30.dp)
        )


        Button(
            onClick = onStart,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(55.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        CircleColor
                )
        ) {

            Text(
                text = "START TRAIL MAKING",

                fontSize = 16.sp,

                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ============================================================
// GENERATE LABELS + RANDOM POSITIONS
// ============================================================

private fun generateTrailPoints(
    part: TrailPart,
    width: Dp,
    height: Dp
): List<TrailPoint> {

    val labels: List<String>


    if (part == TrailPart.PART_A) {

        labels =
            (1..15).map {
                it.toString()
            }

    } else {

        val result =
            mutableListOf<String>()


        for (i in 1..8) {

            result.add(
                i.toString()
            )


            if (i < 8) {

                result.add(
                    ('A'.code + i - 1)
                        .toChar()
                        .toString()
                )
            }
        }


        labels = result
    }


    val positions =
        generateScatteredPositions(
            count = labels.size,
            width = width,
            height = height
        )


    return labels.mapIndexed { index, label ->

        TrailPoint(
            id = index,

            label = label,

            x = positions[index].first,

            y = positions[index].second,

            isNumber =
                label.all {
                    it.isDigit()
                }
        )
    }
}


// ============================================================
// RANDOM SCATTERED POSITIONS
// ============================================================

private fun generateScatteredPositions(
    count: Int,
    width: Dp,
    height: Dp
): List<Pair<Dp, Dp>> {

    val random =
        Random(System.nanoTime())


    val positions =
        mutableListOf<Pair<Dp, Dp>>()


    val minX =
        CircleRadius.value + 10f


    val maxX =
        width.value -
                CircleRadius.value -
                10f


    val minY =
        CircleRadius.value + 10f


    val maxY =
        height.value -
                CircleRadius.value -
                10f


    if (
        maxX <= minX ||
        maxY <= minY
    ) {

        return List(count) {

            Pair(
                width / 2,
                height / 2
            )
        }
    }


    for (index in 0 until count) {

        var chosenX = 0f

        var chosenY = 0f

        var found = false

        var attempts = 0


        while (
            !found &&
            attempts < 50000
        ) {

            attempts++


            val candidateX =
                minX +
                        random.nextFloat() *
                        (maxX - minX)


            val candidateY =
                minY +
                        random.nextFloat() *
                        (maxY - minY)


            var valid = true


            for (existing in positions) {

                val dx =
                    candidateX -
                            existing.first.value


                val dy =
                    candidateY -
                            existing.second.value


                val distance =
                    hypot(
                        dx.toDouble(),
                        dy.toDouble()
                    )


                if (
                    distance <
                    MinimumDistance
                ) {

                    valid = false

                    break
                }


                if (
                    abs(dx) <
                    MinimumAxisGap
                ) {

                    valid = false

                    break
                }


                if (
                    abs(dy) <
                    MinimumAxisGap
                ) {

                    valid = false

                    break
                }
            }


            if (valid) {

                chosenX =
                    candidateX

                chosenY =
                    candidateY

                found = true
            }
        }


        if (!found) {

            chosenX =
                minX +
                        random.nextFloat() *
                        (maxX - minX)


            chosenY =
                minY +
                        random.nextFloat() *
                        (maxY - minY)
        }


        positions.add(
            Pair(
                chosenX.dp,
                chosenY.dp
            )
        )
    }


    return positions
}


// ============================================================
// PART RESULT SCREEN
// ============================================================

@Composable
private fun TrailPartResultScreen(
    part: TrailPart,
    time: Long,
    errors: Int,
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
                if (
                    part ==
                    TrailPart.PART_A
                ) {
                    "Part A Complete"
                } else {
                    "Part B Complete"
                },

            fontSize = 24.sp,

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
                            TrailPart.PART_A
                        ) {
                            "Part A — Numbers"
                        } else {
                            "Part B — Numbers + Letters"
                        },

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(15.dp)
                )


                Text(
                    text =
                        "Completion Time: " +
                                formatTime(time),

                    fontSize = 16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Errors: $errors",

                    fontSize = 16.sp
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(30.dp)
        )


        Button(
            onClick = onContinue,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(55.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        CircleColor
                )
        ) {

            Text(
                text =
                    if (
                        part ==
                        TrailPart.PART_A
                    ) {
                        "CONTINUE TO PART B"
                    } else {
                        "VIEW TRAIL MAKING RESULTS"
                    },

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// FINAL RESULT SCREEN
// ============================================================

@Composable
private fun TrailFinalResultScreen(
    partATime: Long,
    partBTime: Long,
    partAErrors: Int,
    partBErrors: Int,
    onContinue: () -> Unit
) {

    val totalTime =
        partATime + partBTime


    val totalErrors =
        partAErrors + partBErrors


    val switchingCost =
        partBTime - partATime


    val ratio =
        if (partATime > 0L) {

            partBTime.toDouble() /
                    partATime.toDouble()

        } else {

            0.0
        }


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
                "Trail Making Complete",

            fontSize = 24.sp,

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
                        "Part A — Numbers",

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Time: ${formatTime(partATime)}",

                    fontSize = 16.sp
                )


                Text(
                    text =
                        "Errors: $partAErrors",

                    fontSize = 16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                Text(
                    text =
                        "Part B — Numbers + Letters",

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Time: ${formatTime(partBTime)}",

                    fontSize = 16.sp
                )


                Text(
                    text =
                        "Errors: $partBErrors",

                    fontSize = 16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )


                Text(
                    text =
                        "Combined Results",

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                Text(
                    text =
                        "Total Time: ${formatTime(totalTime)}",

                    fontSize = 16.sp
                )


                Text(
                    text =
                        "Total Errors: $totalErrors",

                    fontSize = 16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                Text(
                    text =
                        "Part B − Part A: " +
                                formatTime(switchingCost),

                    fontSize = 16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        String.format(
                            Locale.US,
                            "Part B / Part A: %.2fx",
                            ratio
                        ),

                    fontSize = 16.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(25.dp)
        )


        Button(
            onClick = onContinue,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(55.dp),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        CircleColor
                )
        ) {

            Text(
                text = "CONTINUE",

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// TIME FORMAT
// ============================================================

private fun formatTime(
    milliseconds: Long
): String {

    val seconds =
        milliseconds / 1000


    val remainingMilliseconds =
        milliseconds % 1000


    return String.format(
        Locale.US,
        "%d.%03d s",
        seconds,
        remainingMilliseconds
    )
}