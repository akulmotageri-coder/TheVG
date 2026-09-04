package com.example.vernacularguardian.model

enum class CognitiveTest(
    val title: String,
    val description: String
) {
    STROOP(
        title = "Stroop Test",
        description = "Focus and inhibition"
    ),

    DIGIT_SPAN(
        title = "Digit Span",
        description = "Memory and attention"
    ),

    TRAIL_MAKING(
        title = "Trail Making",
        description = "Attention and sequencing"
    ),

    KEYBOARD(
        title = "Keyboard Processing",
        description = "Typing behaviour"
    ),

    AUDIO(
        title = "Audio Processing",
        description = "Speech and audio behaviour"
    )
}