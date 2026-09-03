package com.example.vga.dementia.acoustic

data class AcousticFeatures(
    val values: FloatArray
) {
    val size: Int
        get() = values.size
}
