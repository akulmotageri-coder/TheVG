package com.example.vga.audioseparation.voice

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.util.Log
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.vga.audioseparation.audio.AudioPlayer
import com.example.vga.audioseparation.audio.VoiceRecorder
import com.example.vga.audioseparation.processing.ReferenceVoiceProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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


// ============================================================
// VOICE INPUT SCREEN
// ============================================================

@Composable
fun VoiceInputScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val voiceRecorder = remember {
        VoiceRecorder(context)
    }

    val audioPlayer = remember {
        AudioPlayer()
    }

    val outputFile = remember {
        context.getExternalFilesDir(null)
            ?.resolve("my_voice_sample.m4a")
    }

    var isRecording by remember {
        mutableStateOf(false)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var isProcessingReference by remember {
        mutableStateOf(false)
    }

    // NEW:
    // Controls the sample-text popup.
    var showSamplePopup by remember {
        mutableStateOf(false)
    }


    // ========================================================
    // SAMPLE TEXT POPUP
    // ========================================================

    if (showSamplePopup) {

        VoiceSampleDialog(
            onFinish = {

                // Close popup
                showSamplePopup = false

                // Stop recording
                if (isRecording) {

                    voiceRecorder.stopRecording()

                    isRecording = false

                    isProcessingReference = true

                    Log.d(
                        "VGA_REFERENCE",
                        "Reference recording stopped from popup"
                    )

                    // ----------------------------------------
                    // Process recorded voice
                    // ----------------------------------------

                    outputFile?.let { file ->

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            try {

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Processing: ${file.absolutePath}"
                                )

                                val embedding =
                                    ReferenceVoiceProcessor
                                        .processAndSave(
                                            context,
                                            file
                                        )

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Reference voice ready"
                                )

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Embedding size=${embedding.size}"
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    "VGA_REFERENCE",
                                    "Failed to process reference voice",
                                    e
                                )

                            } finally {

                                isProcessingReference = false
                            }
                        }
                    }
                }
            }
        )
    }

    if (showSamplePopup) {

        VoiceSampleDialog(
            onFinish = {

                showSamplePopup = false

                if (isRecording) {

                    voiceRecorder.stopRecording()

                    isRecording = false

                    isProcessingReference = true

                    outputFile?.let { file ->

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            try {

                                val embedding =
                                    ReferenceVoiceProcessor
                                        .processAndSave(
                                            context,
                                            file
                                        )

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Reference voice ready"
                                )

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Embedding size=${embedding.size}"
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    "VGA_REFERENCE",
                                    "Failed to process reference voice",
                                    e
                                )

                            } finally {

                                isProcessingReference = false
                            }
                        }
                    }
                }
            }
        )
    }
    // ========================================================
    // MAIN SCREEN
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(horizontal = 20.dp)
    ) {

        Spacer(
            modifier = Modifier.height(28.dp)
        )


        // ====================================================
        // TOP BAR
        // ====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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

                        if (!isRecording &&
                            !isProcessingReference
                        ) {
                            onBack()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "‹",
                    color = Slate,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Audio Input",
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
        // HEADER
        // ====================================================

        Text(
            text = "Add your audio.",
            color = Slate,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 37.sp
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = "Create a voice profile or choose a recording.",
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )


        Spacer(
            modifier = Modifier.height(28.dp)
        )


        // ====================================================
        // YOUR VOICE
        // ====================================================

        VoiceInputCard(
            title = "Your Voice",

            description =
                when {

                    isRecording ->
                        "Recording your voice sample..."

                    isProcessingReference ->
                        "Creating your voice profile..."

                    else ->
                        "Record a short sample so VGA can recognize your voice."
                },

            actionText =
                when {

                    isRecording ->
                        "Recording..."

                    isProcessingReference ->
                        "Processing..."

                    else ->
                        "Record My Voice"
                },

            accentColor = Rose,

            onActionClick = {

                if (isProcessingReference) {
                    return@VoiceInputCard
                }

                // --------------------------------------------
                // START RECORDING
                // --------------------------------------------

                if (!isRecording) {

                    outputFile?.let { file ->

                        voiceRecorder.startRecording(
                            file.absolutePath
                        )

                        isRecording = true

                        // Open sample text popup
                        showSamplePopup = true

                        Log.d(
                            "VGA_REFERENCE",
                            "Started recording reference voice"
                        )
                    }

                } else {

                    // This normally won't be used because
                    // the popup controls finishing recording.
                    voiceRecorder.stopRecording()

                    isRecording = false

                    isProcessingReference = true

                    outputFile?.let { file ->

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            try {

                                val embedding =
                                    ReferenceVoiceProcessor
                                        .processAndSave(
                                            context,
                                            file
                                        )

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Reference voice ready"
                                )

                                Log.d(
                                    "VGA_REFERENCE",
                                    "Embedding size=${embedding.size}"
                                )

                            } catch (e: Exception) {

                                Log.e(
                                    "VGA_REFERENCE",
                                    "Reference processing failed",
                                    e
                                )

                            } finally {

                                isProcessingReference = false
                            }
                        }
                    }
                }
            }
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // ====================================================
        // SAVED VOICE
        // ====================================================

        VoiceInputCard(
            title = "Your Saved Voice",

            description =
                if (outputFile?.exists() == true) {
                    "Your latest voice sample is ready to preview."
                } else {
                    "Your recorded voice sample will appear here."
                },

            actionText =
                if (isPlaying) {
                    "Stop Playback"
                } else {
                    "Play My Voice"
                },

            accentColor = Mint,

            onActionClick = {

                if (isProcessingReference) {
                    return@VoiceInputCard
                }

                if (!isPlaying) {

                    outputFile?.let { file ->

                        if (file.exists()) {

                            audioPlayer.play(
                                file.absolutePath
                            )

                            isPlaying = true
                        }
                    }

                } else {

                    audioPlayer.stop()

                    isPlaying = false
                }
            }
        )


        Spacer(
            modifier = Modifier.height(16.dp)
        )


        // ====================================================
        // CALL RECORDING
        // ====================================================

        VoiceInputCard(
            title = "Call Recording",

            description =
                "Choose a call recording you want to process.",

            actionText =
                "Add Recording",

            accentColor =
                Blue,

            onActionClick = {

                // Existing call recording logic
                // will be connected here.
            }
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // ====================================================
        // PRIVACY NOTE
        // ====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Mint,
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        White.copy(alpha = 0.75f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "✓",
                    color = MintText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Text(
                    text = "Private & local",
                    color = MintText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Your voice sample stays on this device.",
                    color = MintText.copy(alpha = 0.72f),
                    fontSize = 11.sp
                )
            }
        }
    }
}


// ============================================================
// VOICE SAMPLE POPUP
// ============================================================

@Composable
private fun VoiceSampleDialog(
    onFinish: () -> Unit
) {

    Dialog(
        onDismissRequest = {
            // Do nothing.
            // User must press "Close & Finish"
            // so recording is stopped properly.
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    White,
                    RoundedCornerShape(30.dp)
                )
                .border(
                    width = 1.dp,
                    color = Border,
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(24.dp)
        ) {

            // =================================================
            // HEADER
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Read this aloud",
                        color = Slate,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Speak naturally and clearly.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Rose,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "●",
                        color = Berry,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // =================================================
            // RECORDING STATUS
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Rose,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 11.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier.width(9.dp)
                )

                Text(
                    text = "●  Recording in progress",
                    color = Berry,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // SAMPLE TEXT
            // =================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        WarmBackground,
                        RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Border,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(20.dp)
            ) {

                Text(
                    text = """
Hello, this is my voice sample for VGA.

I am speaking clearly and naturally so that my voice can be recognized accurately.

The quick brown fox jumps over the lazy dog.

Today is a beautiful day, and I am ready to begin.
                    """.trimIndent(),

                    color = Slate,
                    fontSize = 16.sp,
                    lineHeight = 26.sp
                )
            }


            Spacer(
                modifier = Modifier.height(16.dp)
            )


            // =================================================
            // TIP
            // =================================================

            Text(
                text = "Tip: Keep a natural pace and stay close to the microphone.",
                color = SoftMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // =================================================
            // FINISH BUTTON
            // =================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Berry,
                        RoundedCornerShape(50.dp)
                    )
                    .clickable {
                        onFinish()
                    }
                    .padding(
                        vertical = 14.dp
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Close & Finish",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.height(9.dp)
            )


            Text(
                text = "Recording will stop when you finish.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = SoftMuted,
                fontSize = 10.sp
            )
        }
    }
}


// ============================================================
// VOICE INPUT CARD
// ============================================================

@Composable
private fun VoiceInputCard(
    title: String,
    description: String,
    actionText: String,
    accentColor: Color,
    onActionClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                White,
                RoundedCornerShape(23.dp)
            )
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(23.dp)
            )
            .padding(20.dp)
    ) {

        // ====================================================
        // TITLE
        // ====================================================

        Text(
            text = title,
            color = Slate,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = description,
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(
            modifier = Modifier.height(17.dp)
        )


        // ====================================================
        // ACTION BUTTON
        // ====================================================

        Box(
            modifier = Modifier
                .background(
                    accentColor,
                    RoundedCornerShape(50.dp)
                )
                .clickable {
                    onActionClick()
                }
                .padding(
                    horizontal = 18.dp,
                    vertical = 11.dp
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = actionText,
                color = Berry,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}