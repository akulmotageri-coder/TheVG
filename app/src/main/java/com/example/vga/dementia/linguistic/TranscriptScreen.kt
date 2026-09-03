package com.example.vga.dementia.linguistic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WarmBackground = Color(0xFFF9F8F6)
private val Slate = Color(0xFF2D3142)
private val Muted = Color(0xFF6C727F)

@Composable
fun TranscriptScreen(
    transcript: String,
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            text = "‹",
            color = Slate,
            fontSize = 34.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable {
                    onBack()
                }
        )

        Text(
            text = "Transcript",
            color = Slate,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = transcript.ifBlank {
                "No transcript available."
            },
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
