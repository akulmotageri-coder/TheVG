
package com.example.vga.audioseparation

import com.example.vga.audioseparation.processing.AudioDecoder
import com.example.vga.dementia.linguistic.IndicWhisperTranscriber
import android.content.Context
import android.media.MediaPlayer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vga.audioseparation.voice.VoiceInputScreen
import com.example.vga.dementia.linguistic.TranscriptScreen
import com.example.vga.dementia.acoustic.AcousticFeatureStore
import com.example.vga.ui.FeatureListScreen
import java.io.File
import java.util.Locale

// ============================================================
// COLORS
// ============================================================

private val WarmBackground = Color(0xFFF9F8F6)

private val Berry = Color(0xFF9E2A4B)
private val BerryDark = Color(0xFF7F203B)

private val Slate = Color(0xFF2D3142)
private val Muted = Color(0xFF6C727F)
private val SoftMuted = Color(0xFF8D99AE)

private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFFF1ECE7)

private val Rose = Color(0xFFFFE5EC)
private val RoseStrong = Color(0xFFFFCAD4)

private val Blue = Color(0xFFE3F2FD)
private val BlueText = Color(0xFF1E6091)

private val Mint = Color(0xFFE6F4EA)
private val MintText = Color(0xFF137333)

private val Butter = Color(0xFFFFF3CD)
private val ButterText = Color(0xFF854D0E)


// ============================================================
// MAIN AUDIO SEPARATION SCREEN
// ============================================================

@Composable
fun AudioSeparationScreen(
    onBack: () -> Unit
) {

    // ========================================================
    // CONTEXT
    // ========================================================

    val context = LocalContext.current

    var showVoiceInput by remember {
        mutableStateOf(false)
    }

    var showCallRecordings by remember {
        mutableStateOf(false)
    }

    var showExtractedVoice by remember {
        mutableStateOf(false)
    }

    var showFeatureList by remember {
        mutableStateOf(false)
    }

    // ========================================================
    // NAVIGATION
    // ========================================================

    if (showVoiceInput) {

        VoiceInputScreen(
            onBack = {
                showVoiceInput = false
            }
        )

        return
    }

    if (showCallRecordings) {

        CallRecordingsScreen(
            onBack = {
                showCallRecordings = false
            }
        )

        return
    }

    if (showExtractedVoice) {

        ExtractedVoiceScreen(
            onBack = {
                showExtractedVoice = false
            }
        )

        return
    }

    if (showFeatureList) {

        // FIX: pass context
        val features =
            AcousticFeatureStore.get(context)

        FeatureListScreen(
            features = features?.toList() ?: emptyList(),
            onBack = {
                showFeatureList = false
            }
        )

        return
    }

    // ========================================================
    // HOME
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 28.dp,
                bottom = 28.dp
            )
    ) {

        // ====================================================
        // TOP BAR
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            CircleIconButton(
                icon = "‹",
                onClick = onBack
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Audio Separation",
                    color = Slate,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "VGA · Voice Intelligence",
                    color = SoftMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(
            modifier = Modifier.height(38.dp)
        )


        // ====================================================
        // HERO SECTION
        // ====================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = White,
                    shape = RoundedCornerShape(30.dp)
                )
                .border(
                    width = 1.dp,
                    color = Border,
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(23.dp)
        ) {

            Column {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                Berry,
                                CircleShape
                            )
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "VOICE SEPARATION",
                        color = Berry,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.3.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = "Keep your voice.",
                    color = Slate,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 37.sp
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Leave the rest behind. ♡",
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                )

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Mint,
                            shape = RoundedCornerShape(17.dp)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 11.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "✓",
                        color = MintText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.width(9.dp)
                    )

                    Column {

                        Text(
                            text = "Voice profile ready",
                            color = MintText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = "Ready for speaker matching",
                            color = MintText.copy(
                                alpha = 0.68f
                            ),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                White.copy(alpha = 0.75f),
                                RoundedCornerShape(50.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                    ) {

                        Text(
                            text = "READY",
                            color = MintText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(55.dp)
                    .background(
                        Rose,
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "♫",
                    color = Berry,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(27.dp)
        )


        // ====================================================
        // YOUR VOICE
        // ====================================================

        SectionLabel(
            text = "YOUR VOICE"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Berry,
                    RoundedCornerShape(25.dp)
                )
                .clickable {
                    showVoiceInput = true
                }
                .padding(19.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(51.dp)
                        .background(
                            White.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "●",
                        color = White,
                        fontSize = 21.sp
                    )
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Your Voice",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Manage your reference voice",
                        color = White.copy(
                            alpha = 0.78f
                        ),
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .background(
                            White,
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "→",
                        color = Berry,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(27.dp)
        )


        // ====================================================
        // PROCESS AUDIO
        // ====================================================

        SectionLabel(
            text = "PROCESS AUDIO"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            AudioActionCard(
                modifier = Modifier.weight(1f),
                title = "Call\nRecordings",
                subtitle = "Process audio",
                icon = "☎",
                background = Blue,
                iconColor = BlueText,
                onClick = {
                    showCallRecordings = true
                }
            )

            AudioActionCard(
                modifier = Modifier.weight(1f),
                title = "Extracted\nVoice",
                subtitle = "View results",
                icon = "♫",
                background = Rose,
                iconColor = Berry,
                onClick = {
                    showExtractedVoice = true
                }
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // ====================================================
        // ACOUSTIC FEATURES
        // ====================================================

        SectionLabel(
            text = "VOICE ANALYSIS"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    White,
                    RoundedCornerShape(22.dp)
                )
                .border(
                    1.dp,
                    Border,
                    RoundedCornerShape(22.dp)
                )
                .clickable {
                    showFeatureList = true
                }
                .padding(17.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(
                            Butter,
                            CircleShape
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = "≋",
                        color = ButterText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(13.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Acoustic Features",
                        color = Slate,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "View extracted eGeMAPSv02 features",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "→",
                    color = Berry,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )


        // ====================================================
        // HOW IT WORKS
        // ====================================================

        SectionLabel(
            text = "HOW IT WORKS"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            MiniStepCard(
                number = "01",
                title = "Record",
                description = "Your voice",
                background = Rose,
                textColor = Berry,
                modifier = Modifier.weight(1f)
            )

            MiniStepCard(
                number = "02",
                title = "Match",
                description = "Speaker ID",
                background = Blue,
                textColor = BlueText,
                modifier = Modifier.weight(1f)
            )

            MiniStepCard(
                number = "03",
                title = "Extract",
                description = "Clean audio",
                background = Mint,
                textColor = MintText,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )


        // ====================================================
        // PRIVATE INFO
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Butter,
                    RoundedCornerShape(19.dp)
                )
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(35.dp)
                    .background(
                        White.copy(alpha = 0.75f),
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "⌂",
                    color = ButterText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(11.dp)
            )

            Column {

                Text(
                    text = "Private & local",
                    color = ButterText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text =
                        "Your extracted audio stays on this device.",
                    color = ButterText.copy(
                        alpha = 0.72f
                    ),
                    fontSize = 11.sp
                )
            }
        }
    }
}


// ============================================================
// SECTION LABEL
// ============================================================

@Composable
private fun SectionLabel(
    text: String
) {

    Text(
        text = text,
        color = SoftMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.3.sp
    )
}


// ============================================================
// CIRCLE ICON BUTTON
// ============================================================

@Composable
private fun CircleIconButton(
    icon: String,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(White)
            .border(
                width = 1.dp,
                color = Border,
                shape = CircleShape
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = icon,
            color = Slate,
            fontSize = 30.sp,
            fontWeight = FontWeight.Light
        )
    }
}


// ============================================================
// AUDIO ACTION CARD
// ============================================================

@Composable
private fun AudioActionCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: String,
    background: Color,
    iconColor: Color,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .background(
                background,
                RoundedCornerShape(24.dp)
            )
            .clickable {
                onClick()
            }
            .padding(17.dp)
    ) {

        Box(
            modifier = Modifier
                .size(43.dp)
                .background(
                    White,
                    CircleShape
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = icon,
                color = iconColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(17.dp)
        )

        Text(
            text = title,
            color = Slate,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = subtitle,
            color = Muted,
            fontSize = 11.sp
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "Open",
                color = iconColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.width(5.dp)
            )

            Text(
                text = "→",
                color = iconColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ============================================================
// MINI STEP CARD
// ============================================================

@Composable
private fun MiniStepCard(
    number: String,
    title: String,
    description: String,
    background: Color,
    textColor: Color,
    modifier: Modifier
) {

    Column(
        modifier = modifier
            .background(
                background,
                RoundedCornerShape(19.dp)
            )
            .padding(12.dp)
    ) {

        Text(
            text = number,
            color = textColor.copy(alpha = 0.65f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = title,
            color = Slate,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = description,
            color = Muted,
            fontSize = 9.sp
        )
    }
}


// ============================================================
// EXTRACTED VOICE SCREEN
// ============================================================

@Composable
fun ExtractedVoiceScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val transcriber = remember {
        IndicWhisperTranscriber(context)
    }

    var files by remember {
        mutableStateOf(
            loadExtractedVoiceFiles(context)
        )
    }

    var currentlyPlaying by remember {
        mutableStateOf<String?>(null)
    }

    var fileToDelete by remember {
        mutableStateOf<File?>(null)
    }

    var showFeatureList by remember {
        mutableStateOf(false)
    }

    var showTranscript by remember {
        mutableStateOf(false)
    }

    var selectedTranscript by remember {
        mutableStateOf("")
    }

    var selectedFeatures by remember {
        mutableStateOf<List<Float>>(emptyList())
    }

    val mediaPlayer = remember {
        MediaPlayer()
    }

    // ========================================================
    // REFRESH FILE LIST WHEN SCREEN OPENS
    // ========================================================

    LaunchedEffect(Unit) {

        files =
            loadExtractedVoiceFiles(
                context
            )
    }

    // ========================================================
    // FEATURE LIST
    // ========================================================

    if (showFeatureList) {

        FeatureListScreen(
            features = selectedFeatures,
            onBack = {
                showFeatureList = false
            }
        )

        return
    }
    if (showTranscript) {

        TranscriptScreen(
            transcript = selectedTranscript,
            onBack = {
                showTranscript = false
            }
        )

        return
    }

    // ========================================================
    // DELETE CONFIRMATION
    // ========================================================

    if (fileToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                fileToDelete = null
            },

            title = {

                Text(
                    text = "Delete recording?",
                    color = Slate,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Text(
                    text =
                        "This extracted voice recording will be permanently removed from your device.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },

            confirmButton = {

                Text(
                    text = "Delete",
                    color = Berry,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,

                    modifier = Modifier
                        .clickable {

                            val file =
                                fileToDelete

                            if (file != null) {

                                if (
                                    currentlyPlaying ==
                                    file.absolutePath
                                ) {

                                    try {

                                        if (
                                            mediaPlayer.isPlaying
                                        ) {
                                            mediaPlayer.stop()
                                        }

                                    } catch (_: Exception) {
                                    }

                                    currentlyPlaying = null
                                }

                                file.delete()

                                files =
                                    loadExtractedVoiceFiles(
                                        context
                                    )
                            }

                            fileToDelete = null
                        }
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                )
            },

            dismissButton = {

                Text(
                    text = "Cancel",
                    color = Muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,

                    modifier = Modifier
                        .clickable {
                            fileToDelete = null
                        }
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                )
            },

            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ========================================================
    // CLEANUP
    // ========================================================

    DisposableEffect(Unit) {

        onDispose {

            try {

                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }

            } catch (_: Exception) {
            }

            mediaPlayer.release()
        }
    }

    // ========================================================
    // MAIN SCREEN
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 30.dp
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

            Text(
                text = "‹",
                color = Slate,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,

                modifier = Modifier
                    .clickable {

                        try {

                            if (
                                mediaPlayer.isPlaying
                            ) {
                                mediaPlayer.stop()
                            }

                        } catch (_: Exception) {
                        }

                        currentlyPlaying = null

                        onBack()
                    }
                    .padding(
                        horizontal = 8.dp,
                        vertical = 2.dp
                    )
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Text(
                    text = "Extracted Voice",
                    color = Slate,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Your processed recordings",
                    color = SoftMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // ====================================================
        // HEADER
        // ====================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    White,
                    RoundedCornerShape(28.dp)
                )
                .border(
                    1.dp,
                    Border,
                    RoundedCornerShape(28.dp)
                )
                .padding(22.dp)
        ) {

            Column {

                Text(
                    text = "Your extracted voice.",
                    color = Slate,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Clean voice recordings created by VGA.",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(
                    modifier = Modifier.height(17.dp)
                )

                Row(
                    modifier = Modifier
                        .background(
                            Rose,
                            RoundedCornerShape(50.dp)
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "♫",
                        color = Berry,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text =
                            "${files.size} recording(s)",
                        color = Berry,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        // ====================================================
        // EMPTY / FILE LIST
        // ====================================================

        if (files.isEmpty()) {

            EmptyExtractedVoiceCard()

        } else {

            files.forEach { file ->

                ExtractedVoiceCard(
                    file = file,

                    isPlaying =
                        currentlyPlaying ==
                                file.absolutePath,

                    onPlay = {

                        try {

                            if (
                                currentlyPlaying ==
                                file.absolutePath
                            ) {

                                if (
                                    mediaPlayer.isPlaying
                                ) {
                                    mediaPlayer.stop()
                                }

                                currentlyPlaying =
                                    null

                            } else {

                                mediaPlayer.reset()

                                mediaPlayer.setDataSource(
                                    file.absolutePath
                                )

                                mediaPlayer.prepare()

                                mediaPlayer.start()

                                currentlyPlaying =
                                    file.absolutePath

                                mediaPlayer
                                    .setOnCompletionListener {

                                        currentlyPlaying =
                                            null
                                    }
                            }

                        } catch (_: Exception) {

                            currentlyPlaying = null
                        }
                    },

                    onDelete = {

                        fileToDelete = file
                    },

                    onFeatures = {

                        val features =
                            AcousticFeatureStore.get(
                                context
                            )

                        selectedFeatures =
                            features?.toList()
                                ?: emptyList()

                        showFeatureList = true
                    },

                    onTranscript = {

                        try {

                            val audioData =
                                AudioDecoder.decodeToMonoFloat(file)

                            selectedTranscript =
                                transcriber.transcribe(
                                    samples = audioData.samples,
                                    sampleRate = audioData.sampleRate
                                )

                            showTranscript = true

                        } catch (e: Exception) {

                            selectedTranscript =
                                "Transcription failed: ${e.message}"

                            showTranscript = true
                        }
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }
}


// ============================================================
// EXTRACTED VOICE CARD
// ============================================================

@Composable
private fun ExtractedVoiceCard(
    file: File,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onFeatures: () -> Unit,
    onTranscript: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                White,
                RoundedCornerShape(23.dp)
            )
            .border(
                1.dp,
                Border,
                RoundedCornerShape(23.dp)
            )
            .padding(17.dp)
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isPlaying)
                            Berry
                        else
                            Rose,
                        CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (isPlaying)
                            "■"
                        else
                            "♫",

                    color =
                        if (isPlaying)
                            White
                        else
                            Berry,

                    fontSize = 19.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = file.name,
                    color = Slate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "WAV  •  ${formatFileSize(file.length())}",
                    color = SoftMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isPlaying)
                            Berry
                        else
                            Rose,

                        RoundedCornerShape(50.dp)
                    )
                    .clickable {
                        onPlay()
                    }
                    .padding(
                        vertical = 10.dp
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (isPlaying)
                            "■  Stop"
                        else
                            "▶  Play",

                    color =
                        if (isPlaying)
                            White
                        else
                            Berry,

                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .background(
                        White,
                        RoundedCornerShape(50.dp)
                    )
                    .border(
                        1.dp,
                        RoseStrong,
                        RoundedCornerShape(50.dp)
                    )
                    .clickable {
                        onDelete()
                    }
                    .padding(
                        vertical = 10.dp
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "Delete",
                    color = Berry,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Butter,
                    RoundedCornerShape(50.dp)
                )
                .clickable {
                    onFeatures()
                }
                .padding(
                    vertical = 10.dp
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "View Acoustic Features  →",
                color = ButterText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

// ⬇️ ADD THE NEW TRANSCRIPT BUTTON HERE

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Blue,
                    RoundedCornerShape(50.dp)
                )
                .clickable {
                    onTranscript()
                }
                .padding(
                    vertical = 10.dp
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "View Transcript  →",
                color = BlueText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ============================================================
// EMPTY EXTRACTED VOICE
// ============================================================

@Composable
private fun EmptyExtractedVoiceCard() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                White,
                RoundedCornerShape(25.dp)
            )
            .border(
                1.dp,
                Border,
                RoundedCornerShape(25.dp)
            )
            .padding(25.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Rose,
                    CircleShape
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = "♫",
                color = Berry,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Text(
            text = "No extracted recordings yet",
            color = Slate,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Process a call recording and your\nvoice-only audio will appear here.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}


// ============================================================
// LOAD EXTRACTED VOICE FILES
// ============================================================

private fun loadExtractedVoiceFiles(
    context: Context
): List<File> {

    val directory =
        File(
            context.filesDir,
            "extracted_voice"
        )

    if (!directory.exists()) {
        return emptyList()
    }

    return directory
        .listFiles()
        ?.filter {
            it.isFile &&
                    it.extension.equals(
                        "wav",
                        ignoreCase = true
                    )
        }
        ?.sortedByDescending {
            it.lastModified()
        }
        ?: emptyList()
}


// ============================================================
// FILE SIZE
// ============================================================

private fun formatFileSize(
    bytes: Long
): String {

    return when {

        bytes < 1024 ->
            "$bytes B"

        bytes < 1024 * 1024 ->
            "${bytes / 1024} KB"

        else ->
            String.format(
                Locale.US,
                "%.2f MB",
                bytes /
                        (1024.0 * 1024.0)
            )
    }
}
