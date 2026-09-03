package com.example.vga.audioseparation.models

import java.io.File

data class CallRecording(
    val file: File,
    val displayName: String,
    val durationMs: Long? = null
)