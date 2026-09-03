package com.example.vga.audioseparation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vga.audioseparation.audio.AudioPlayer
import com.example.vga.audioseparation.repository.CallRecordingRepository
import java.io.File
import androidx.compose.foundation.layout.Box
// ============================================================
// COLORS
// ============================================================

private val WarmBackground =
    Color(0xFFF9F8F6)

private val Slate =
    Color(0xFF2D3142)

private val Muted =
    Color(0xFF6C727F)

private val SoftMuted =
    Color(0xFF8D99AE)

private val White =
    Color(0xFFFFFFFF)

private val Border =
    Color(0xFFF1ECE7)

private val Berry =
    Color(0xFF9E2A4B)

private val Rose =
    Color(0xFFFFE5EC)


// ============================================================
// CALL RECORDINGS SCREEN
// ============================================================

@Composable
fun CallRecordingsScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val repository =
        remember {
            CallRecordingRepository(context)
        }

    val audioPlayer =
        remember {
            AudioPlayer()
        }

    var recordings by remember {
        mutableStateOf(
            repository.getRecordings()
        )
    }

    var playingFilePath by remember {
        mutableStateOf<String?>(null)
    }

    var fileToDelete by remember {
        mutableStateOf<File?>(null)
    }


    // ========================================================
    // DELETE CONFIRMATION DIALOG
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
                        "This recording will be permanently removed from your device.",
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

                                // Stop playback if this file
                                // is currently playing.

                                if (
                                    playingFilePath ==
                                    file.absolutePath
                                ) {

                                    audioPlayer.stop()

                                    playingFilePath =
                                        null
                                }

                                // Delete the physical file.

                                val deleted =
                                    file.delete()

                                if (deleted) {

                                    Toast
                                        .makeText(
                                            context,
                                            "Recording deleted",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()

                                } else {

                                    Toast
                                        .makeText(
                                            context,
                                            "Could not delete recording",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }

                                // Refresh list.

                                recordings =
                                    repository.getRecordings()
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
    // MAIN SCREEN
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 24.dp
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

            // Back button without Icons dependency.

            Text(
                text = "‹",
                color = Slate,
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable {
                        audioPlayer.stop()
                        playingFilePath = null
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
                    text = "Call Recordings",
                    color = Slate,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Choose a recording to process",
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    White,
                    RoundedCornerShape(26.dp)
                )
                .border(
                    width = 1.dp,
                    color = Border,
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(22.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Your recordings",
                    color = Slate,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${recordings.size}",
                    color = Berry,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Rose,
                            RoundedCornerShape(50.dp)
                        )
                        .padding(
                            horizontal = 11.dp,
                            vertical = 6.dp
                        )
                )
            }

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    "Select a call recording to listen to it or process it.",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }


        Spacer(
            modifier = Modifier.height(22.dp)
        )


        // ====================================================
        // RECORDINGS
        // ====================================================

        if (recordings.isEmpty()) {

            EmptyCallRecordings()

        } else {

            recordings.forEach { recording ->

                CallRecordingCard(
                    name = recording.displayName,
                    file = recording.file,
                    isPlaying =
                        playingFilePath ==
                                recording.file.absolutePath,

                    onPlay = {

                        if (
                            playingFilePath ==
                            recording.file.absolutePath
                        ) {

                            audioPlayer.stop()

                            playingFilePath =
                                null

                        } else {

                            audioPlayer.stop()

                            audioPlayer.play(
                                recording.file.absolutePath
                            )

                            playingFilePath =
                                recording.file.absolutePath
                        }
                    },

                    onDelete = {

                        fileToDelete =
                            recording.file
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
// CALL RECORDING CARD
// ============================================================

@Composable
private fun CallRecordingCard(
    name: String,
    file: File,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
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
            .padding(17.dp)
    ) {


        // ====================================================
        // FILE INFO
        // ====================================================

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = name,
                    color = Slate,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "WAV recording  •  ${formatFileSize(file.length())}",
                    color = SoftMuted,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "CALL",
                color = Berry,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(
                        Rose,
                        RoundedCornerShape(50.dp)
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 5.dp
                    )
            )
        }


        Spacer(
            modifier = Modifier.height(15.dp)
        )


        // ====================================================
        // ACTIONS
        // ====================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {

            // PLAY BUTTON

            BoxButton(
                text =
                    if (isPlaying)
                        "■  Stop"
                    else
                        "▶  Play",

                textColor =
                    if (isPlaying)
                        White
                    else
                        Berry,

                background =
                    if (isPlaying)
                        Berry
                    else
                        Rose,

                modifier =
                    Modifier.weight(1f),

                onClick = onPlay
            )


            // DELETE BUTTON

            BoxButton(
                text = "Delete",

                textColor = Berry,

                background = White,

                modifier =
                    Modifier
                        .weight(0.55f)
                        .border(
                            width = 1.dp,
                            color = Rose,
                            shape = RoundedCornerShape(50.dp)
                        ),

                onClick = onDelete
            )
        }
    }
}


// ============================================================
// BUTTON
// ============================================================

@Composable
private fun BoxButton(
    text: String,
    textColor: Color,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier = modifier
            .background(
                background,
                RoundedCornerShape(50.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                vertical = 10.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


// ============================================================
// EMPTY STATE
// ============================================================

@Composable
private fun EmptyCallRecordings() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                White,
                RoundedCornerShape(25.dp)
            )
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(25.dp)
            )
            .padding(28.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "☎",
            color = Berry,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(13.dp)
        )

        Text(
            text = "No call recordings yet",
            color = Slate,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text =
                "Import a WAV call recording to get started.",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
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
                "%.2f MB",
                bytes /
                        (1024.0 * 1024.0)
            )
    }
}