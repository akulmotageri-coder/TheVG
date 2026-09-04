package com.example.vernacularguardian.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// COLORS
//
// Sourced from the Cognitive Check-In design system
// (cognitive_test_designs/1._cognitive_tests_dashboard).
// ============================================================

private val ScreenBackground =
    Color(0xFFF8F9FA)

private val Plum =
    Color(0xFF8C2B43)

private val CardTitleColor =
    Color(0xFF202224)

private val BodyMuted =
    Color(0xFF676D75)

private val BorderMuted =
    Color(0xFFE5E7EB)

private val White =
    Color(0xFFFFFFFF)

private val StatusMuted =
    Color(0xFF9CA3AF)

private val CompletedColor =
    Color(0xFF3D8F7B)


// Card surfaces

private val StroopSurface =
    Color(0xFFFCEDEE)

private val DigitSpanSurface =
    Color(0xFFEAF3FB)

private val TrailMakingSurface =
    Color(0xFFFDF1E7)


// Card dot indicators

private val StroopDot =
    Color(0xFF8C2B43)

private val DigitSpanDot =
    Color(0xFF3B82F6)

private val TrailMakingDot =
    Color(0xFFE06D53)


// ============================================================
// CATEGORY TAG
// ============================================================

private data class CategoryTag(
    val label: String,
    val background: Color,
    val textColor: Color
)


// Tag palettes exactly as specified in the design.

private val TagAttention =
    CategoryTag(
        label = "Attention",
        background = Color(0xFF86C1D9).copy(alpha = 0.30f),
        textColor = Color(0xFF1B4B66)
    )

private val TagExecutiveFunction =
    CategoryTag(
        label = "Executive Function",
        background = Color(0xFFE6BC5C).copy(alpha = 0.35f),
        textColor = Color(0xFF5C4509)
    )

private val TagWorkingMemory =
    CategoryTag(
        label = "Working Memory",
        background = Color(0xFF70C5DD).copy(alpha = 0.35f),
        textColor = Color(0xFF134D62)
    )

private val TagVisualScanning =
    CategoryTag(
        label = "Visual Scanning",
        background = Color(0xFF78C8DF).copy(alpha = 0.35f),
        textColor = Color(0xFF144E61)
    )

private val TagProcessingSpeed =
    CategoryTag(
        label = "Processing Speed",
        background = Color(0xFFE7B863).copy(alpha = 0.35f),
        textColor = Color(0xFF543E07)
    )


// ============================================================
// DASHBOARD SCREEN
//
// Cognitive test selection. Contains no test logic itself -
// each card simply starts the matching assessment.
// ============================================================

@Composable
fun DashboardScreen(

    stroopCompleted: Boolean = false,

    digitSpanCompleted: Boolean = false,

    trailMakingCompleted: Boolean = false,

    onStroopClick: () -> Unit = {},

    onDigitSpanClick: () -> Unit = {},

    onTrailMakingClick: () -> Unit = {},

    onViewResultsClick: () -> Unit = {},

    onBack: () -> Unit = {}
) {

    val allCompleted =
        stroopCompleted &&
                digitSpanCompleted &&
                trailMakingCompleted


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp,
                    bottom = 28.dp
                )
    ) {


        // ====================================================
        // TOP BAR
        // ====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(
                            width = 1.dp,
                            color = BorderMuted,
                            shape = CircleShape
                        )
                        .clickable { onBack() },

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "‹",
                    color = CardTitleColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                text = "Tests Library",
                color = CardTitleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }


        Spacer(
            modifier = Modifier.height(22.dp)
        )


        // ====================================================
        // HEADER
        // ====================================================

        Text(
            text = "Cognitive Tests",
            color = Color(0xFF111827),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Select an assessment to begin. Ensure you are in a " +
                        "quiet, distraction-free environment.",
            color = BodyMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )


        Spacer(
            modifier = Modifier.height(22.dp)
        )


        // ====================================================
        // STROOP TEST
        // ====================================================

        AssessmentCard(
            title = "Stroop Test",
            duration = "3 min",
            description =
                "Evaluates selective attention, cognitive flexibility, " +
                        "and processing speed.",
            surface = StroopSurface,
            dotColor = StroopDot,
            tags = listOf(TagAttention, TagExecutiveFunction),
            completed = stroopCompleted,
            onClick = onStroopClick
        )


        Spacer(
            modifier = Modifier.height(14.dp)
        )


        // ====================================================
        // DIGIT SPAN
        // ====================================================

        AssessmentCard(
            title = "Digit Span",
            duration = "4 min",
            description =
                "Measures working memory capacity by asking you to " +
                        "recall sequences of digits.",
            surface = DigitSpanSurface,
            dotColor = DigitSpanDot,
            tags = listOf(TagWorkingMemory),
            completed = digitSpanCompleted,
            onClick = onDigitSpanClick
        )


        Spacer(
            modifier = Modifier.height(14.dp)
        )


        // ====================================================
        // TRAIL MAKING
        // ====================================================

        AssessmentCard(
            title = "Trail Making",
            duration = "4 min",
            description =
                "Assesses visual search speed, scanning, and speed of " +
                        "processing.",
            surface = TrailMakingSurface,
            dotColor = TrailMakingDot,
            tags = listOf(TagVisualScanning, TagProcessingSpeed),
            completed = trailMakingCompleted,
            onClick = onTrailMakingClick
        )


        // ====================================================
        // COMBINED RESULTS
        // ====================================================

        if (allCompleted) {

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onViewResultsClick,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                shape =
                    RoundedCornerShape(50.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Plum
                    )
            ) {

                Text(
                    text = "View Combined Results",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }


        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // ====================================================
        // FOOTER
        // ====================================================

        Text(
            text = "Complete each assessment at your own pace.",

            modifier =
                Modifier.fillMaxWidth(),

            fontSize = 11.sp,
            color = StatusMuted
        )
    }
}


// ============================================================
// ASSESSMENT CARD
// ============================================================

@Composable
private fun AssessmentCard(

    title: String,

    duration: String,

    description: String,

    surface: Color,

    dotColor: Color,

    tags: List<CategoryTag>,

    completed: Boolean,

    onClick: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(16.dp)
    ) {


        // ====================================================
        // TITLE + DURATION
        // ====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .background(dotColor, CircleShape)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = title,
                color = CardTitleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Box(
                modifier =
                    Modifier
                        .background(
                            color = White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 3.dp
                        )
            ) {

                Text(
                    text = duration,
                    color = Color(0xFF4B5563),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        // ====================================================
        // DESCRIPTION
        // ====================================================

        Text(
            text = description,
            color = Color(0xFF4B5563),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(start = 18.dp)
        )


        Spacer(
            modifier = Modifier.height(11.dp)
        )


        // ====================================================
        // CATEGORY TAGS
        // ====================================================

        Row(
            modifier =
                Modifier.padding(start = 18.dp),

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            tags.forEach { tag ->

                Box(
                    modifier =
                        Modifier
                            .background(
                                color = tag.background,
                                shape = RoundedCornerShape(50.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 3.dp
                            )
                ) {

                    Text(
                        text = tag.label,
                        color = tag.textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        // ====================================================
        // STATUS
        // ====================================================

        Row(
            modifier =
                Modifier.padding(start = 18.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .then(
                            if (completed) {
                                Modifier.background(
                                    CompletedColor,
                                    CircleShape
                                )
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = StatusMuted,
                                    shape = CircleShape
                                )
                            }
                        )
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text(
                text =
                    if (completed) {
                        "Completed"
                    } else {
                        "Not Started"
                    },

                color =
                    if (completed) {
                        CompletedColor
                    } else {
                        StatusMuted
                    },

                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
