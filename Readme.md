# VGA — Multimodal Cognitive Monitoring System

VGA is a **privacy-first, local cognitive monitoring application for Android** that combines speech, language, typing behavior, and cognitive-test data to identify **patterns that may require attention** over time.

The system is designed as an **early-warning and monitoring tool**, not as a medical diagnostic system.

Instead of asking an AI model to directly diagnose a condition, VGA extracts measurable features locally, compares them against the user's personal baseline, visualizes changes through charts and timelines, and provides a local AI assistant that helps explain the results.

---

## Overview

VGA combines multiple sources of behavioral and cognitive information:

```text
                 ┌─────────────────────┐
                 │     Call Audio      │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │ Speaker Separation  │
                 └──────────┬──────────┘
                            ↓
                    ┌───────┴───────┐
                    ↓               ↓
              Clean Voice       Transcript
                    ↓               ↓
             Acoustic/NLP       NLP Analysis
                    │               │
                    └───────┬───────┘
                            ↓
                 ┌─────────────────────┐
                 │  Feature Extraction │
                 └──────────┬──────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
   Keyboard Data       Cognitive Tests     Historical Data
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ↓
                 ┌─────────────────────┐
                 │ Personal Baseline   │
                 │ & Change Detection  │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │ Pattern Detection   │
                 └──────────┬──────────┘
                            ↓
              ┌─────────────┴─────────────┐
              ↓                           ↓
       Charts & Timeline             AI Assistant
       Deterministic NLP              Local Qwen
              │                           │
              ↓                           ↓
       Visual Monitoring            Explanation / Q&A
```

---

# Core Modules

## 1. Audio Processing

The Audio Processing module handles call recordings and extracts the user's voice from mixed conversations.

### Pipeline

```text
Call Recording
      ↓
Audio Preprocessing
      ↓
Speaker Separation
      ↓
User Voice
      ↓
Acoustic Analysis
      ↓
Transcript Generation
      ↓
NLP Analysis
```

### Speaker Separation

The application uses a speaker encoder based on **Resemblyzer**, exported to ONNX for Android execution.

The speaker encoder produces a 256-dimensional speaker embedding.

The Android implementation runs through ONNX Runtime.

### Audio Processing

The system normalizes and resamples audio to:

* Sample rate: 16 kHz
* Mono audio
* Mel-spectrogram based processing

---

# 2. Acoustic Analysis

The cleaned voice is analyzed using the **eGeMAPSv02** acoustic feature set through openSMILE.

The system extracts:

* Prosodic characteristics
* Spectral characteristics
* Voice quality characteristics
* Energy-related characteristics
* Temporal speech characteristics

The eGeMAPSv02 configuration produces **88 acoustic features**.

These features are used as measurable speech signals rather than direct disease indicators.

### Concept

> Acoustic analysis focuses on **how the person speaks**.

---

# 3. Speech Transcription

The cleaned voice is transcribed locally using **IndicWhisper**.

The transcript can contain English and supported Indian-language speech depending on the model and input.

Example:

```text
Audio
 ↓
IndicWhisper
 ↓
Transcript
```

The transcript becomes the input for local linguistic/NLP analysis.

---

# 4. NLP-Based Linguistic Analysis

VGA does not use the AI model to generate the underlying cognitive metrics.

Instead, measurable linguistic features are calculated locally.

Possible features include:

### Repetition

Detection of:

* Repeated words
* Repeated phrases
* Repeated sentences
* Repeated questions
* Repeated information

Example:

```text
"I went to the hospital yesterday."

"I went to the hospital yesterday."

"I went to the hospital yesterday."
```

The system can identify this as an increase in repetition.

---

### Vocabulary Diversity

Measures the relationship between unique words and total words.

This can help monitor changes in language usage over time.

---

### Vague Word Usage

The system can monitor usage of general/vague words such as:

* thing
* stuff
* that
* something
* place

These are treated as **linguistic indicators**, not diagnostic evidence.

---

### Sentence Characteristics

The system can calculate:

* Number of sentences
* Average sentence length
* Sentence-length variability
* Very short sentence frequency
* Fragmented sentence indicators

---

### Filler Words

Examples:

* um
* uh
* you know
* like

The system can track their frequency across sessions.

---

### Self-Correction Indicators

Where reliably detectable, the system can identify patterns such as:

```text
"I went to... I mean, I visited... yesterday."
```

These can be stored as linguistic observations.

---

# 5. Personal Baseline

A major part of VGA is **personalized longitudinal monitoring**.

Instead of comparing every person against a generic threshold, VGA can compare the user's current measurements with their own historical behavior.

Example:

```text
Previous baseline

Repetition:          3.1%
Vocabulary diversity: 0.72


Current session

Repetition:          8.4%
Vocabulary diversity: 0.61
```

The system can then display:

> Repetition is higher than your recent baseline.

rather than assigning an arbitrary disease probability.

If insufficient historical data exists:

> Baseline not yet established.

No baseline values are fabricated.

---

# 6. Keyboard Processing

The Keyboard Processing module analyzes typing behavior and cognitive-related typing signals.

Possible measurements include:

* Typing speed
* Inter-key timing
* Timing variability
* Backspace rate
* Correction behavior
* Typing pauses
* Session duration
* Other behavioral typing patterns

The keyboard module is kept separate from the audio processing implementation.

---

# 7. Cognitive Tests

VGA includes an independent Cognitive Tests module.

Tests are maintained separately and provide structured cognitive measurements.

The results can later contribute to the overall longitudinal monitoring system.

The application does not interpret an individual test result as a medical diagnosis.

---

# 8. Pattern Detection

The system converts measurable changes into understandable observations.

Examples:

```text
Increased repetition

Reduced vocabulary diversity

Increased filler-word frequency

Change in average sentence length

Increased typing variability

Change in cognitive-test performance
```

These are called **patterns requiring attention**, rather than disease diagnoses.

---

# 9. Charts & Visualization

VGA uses visual analytics to make longitudinal changes easier to understand.

The dashboard can contain:

### Cognitive Trend Chart

Line charts can display changes over time.

Example:

```text
Repetition
  ^
  |          ●
  |       ●
  |    ●
  | ●
  +--------------------> Time
```

Possible metrics:

* Repetition frequency
* Vocabulary diversity
* Filler-word frequency
* Average sentence length
* Typing speed
* Backspace rate
* Cognitive-test scores

---

### Baseline Comparison

Visual comparison between:

```text
Current Value
      vs
Personal Baseline
```

This allows users to understand whether a measurement has changed relative to their own history.

---

### Multimodal Overview

The dashboard can visually show available information from:

```text
Speech
Language
Keyboard
Cognitive Tests
```

The interface focuses on **measured changes**, rather than producing an unexplained overall "dementia score."

---

# 10. Visual Timeline

The Main Dashboard contains a chronological visual timeline.

Example:

```text
● Sep 01
│
├── Call analyzed
│
● Sep 03
│
├── Transcript processed
│
● Sep 05
│
├── Increased repetition detected
│
● Sep 07
│
├── Cognitive test completed
│
● Sep 09
│
└── Vocabulary diversity changed
```

Timeline events are generated from actual application data.

The timeline does not rely on the AI model to invent events.

---

# 11. Local AI Assistant

VGA includes an optional local AI assistant.

The AI model is:

**Qwen2.5-1.5B-Instruct**

The AI runs on a user-configured local server using an OpenAI-compatible API.

Example server:

```text
http://10.152.3.58:8080
```

However, the URL is **not hardcoded**.

The user can enter and save their current server URL directly inside the application.

---

## AI Server Configuration

The application provides:

```text
AI Assistant Settings

Server URL
[ http://10.152.3.58:8080 ]

[ Test Connection ]

Status: Connected

Model:
Qwen2.5-1.5B-Instruct

[ Save ]
```

The saved URL is stored locally and can be changed whenever the server address changes.

---

# 12. AI API

The application uses the OpenAI-compatible endpoint:

```text
GET /v1/models
```

for connection testing.

Chat requests use:

```text
POST /v1/chat/completions
```

Example request:

```json
{
  "model": "Qwen2.5-1.5B-Instruct",
  "messages": [
    {
      "role": "user",
      "content": "Explain my recent speech pattern."
    }
  ],
  "stream": false
}
```

The application should only send the information required for the requested explanation.

---

# 13. Role of AI

The AI is **not responsible for generating the underlying measurements**.

The architecture is:

```text
Raw Data
   ↓
Local Feature Extraction
   ↓
NLP / Statistical Analysis
   ↓
Measured Results
   ↓
Baseline Comparison
   ↓
Pattern Detection
   ↓
Charts + Timeline
```

The AI sits above this layer:

```text
Stored Results
      ↓
Qwen AI Assistant
      ↓
Explanation / Q&A
```

This makes the system more transparent and reproducible.

---

# 14. Example AI Assistant Questions

Users can ask:

> What changed recently?

> Why is repetition highlighted?

> What does vocabulary diversity mean?

> Explain my speech analysis.

> How am I doing compared with my baseline?

> What does this graph mean?

> Why was this event added to my timeline?

> Explain my cognitive-test results.

> What patterns should I monitor?

> What can I do next?

The AI explains the existing measurements instead of creating new measurements.

---

# 15. Non-Diagnostic Design

VGA is designed as a **monitoring and early-warning system**, not a diagnostic tool.

The application must never claim:

```text
"You have Alzheimer's disease."

"You have dementia."

"You have Lewy body dementia."

"You have vascular dementia."

"You have frontotemporal dementia."
```

Instead, the system uses language such as:

```text
Observed pattern

Pattern requiring attention

Possible cognitive concern

Change compared with personal baseline

Further professional evaluation may be appropriate
```

A single linguistic, acoustic, typing, or cognitive feature is not sufficient to establish a medical diagnosis.

---

# 16. Multimodal Architecture

The long-term goal is to combine multiple independent signals.

```text
                  ┌───────────────┐
                  │ Audio Signals │
                  └───────┬───────┘
                          │
                  ┌───────▼───────┐
                  │ Acoustic Data  │
                  └───────┬───────┘
                          │
                          │
┌──────────────┐    ┌─────▼─────┐    ┌─────────────────┐
│ Transcript   │───►│  Feature  │◄───│ Keyboard Data   │
│ NLP Features │    │   Store    │    └─────────────────┘
└──────────────┘    └─────┬─────┘
                          │
                  ┌───────▼────────┐
                  │ Cognitive Tests│
                  └───────┬────────┘
                          │
                  ┌───────▼─────────┐
                  │ Personal Baseline│
                  └───────┬─────────┘
                          │
                  ┌───────▼─────────┐
                  │ Pattern Detection│
                  └───────┬─────────┘
                          │
              ┌───────────┴───────────┐
              ↓                       ↓
        Visualization            AI Assistant
        Charts/Timeline          Qwen Local LLM
```

---

# 17. Privacy-First Architecture

The application is designed around local processing.

Whenever possible:

* Audio processing runs locally
* Speaker separation runs locally
* Transcription runs locally
* NLP processing runs locally
* Feature extraction runs locally
* Baseline calculations run locally
* Charts run locally
* Timeline generation runs locally
* AI inference uses the user's configured local server

No external cloud AI service is required for the core analysis pipeline.

---

# 18. Technology Stack

### Android

* Kotlin
* Jetpack Compose
* Material 3
* Android SDK

### Audio

* Resemblyzer
* ONNX Runtime
* Sherpa-ONNX
* IndicWhisper
* openSMILE

### Acoustic Features

* eGeMAPSv02
* 88 standardized acoustic features

### AI

* Qwen2.5-1.5B-Instruct
* OpenAI-compatible local inference server

### Networking

* Java `HttpURLConnection`
* HTTP/JSON

### Data

* Local Android storage/database depending on module requirements

---

# 19. Project Principles

VGA follows several core principles:

### Local First

Keep sensitive processing on-device or within the user's local network.

### Explainable

Show the underlying measurements rather than hiding everything behind a single AI-generated score.

### Personal Baseline

Focus on changes in the user's own behavior over time.

### Multimodal

Combine speech, language, typing, and cognitive-test signals.

### Non-Diagnostic

Identify patterns requiring attention without claiming a medical diagnosis.

### Reproducible

The same input should produce the same deterministic NLP measurements.

### AI as an Assistant

Use the LLM to explain results and answer questions rather than replacing the analytical pipeline.

---

# 20. Example End-to-End Session

```text
1. User shares a call recording
        ↓
2. Speaker separation extracts user's voice
        ↓
3. Audio is normalized/resampled
        ↓
4. Acoustic features are extracted
        ↓
5. IndicWhisper generates transcript
        ↓
6. Local NLP calculates linguistic features
        ↓
7. Results are stored
        ↓
8. Current results are compared with personal baseline
        ↓
9. Measurable patterns are identified
        ↓
10. Timeline event is generated
        ↓
11. Charts are updated
        ↓
12. User opens AI Assistant
        ↓
13. User asks a question
        ↓
14. Qwen explains the stored results
```

---

# 21. Example Result

A session might produce:

```text
Speech Analysis

Words: 184

Vocabulary diversity: 0.61

Repetition rate: 8.4%

Filler-word frequency: 4.2%

Average sentence length: 9.8 words
```

Historical baseline:

```text
Vocabulary diversity: 0.72
Repetition rate: 3.1%
```

The application may display:

> **Pattern requiring attention**
>
> Repetition is higher than the recent personal baseline.

And the graph can show the progression:

```text
Repetition Rate

10% |              ●
 8% |           ●
 6% |        ●
 4% | ●  ●
 2% | ●
    +-------------------
      S1 S2 S3 S4 S5
```

The AI Assistant can then explain:

> "Your recent sessions show more repetition than your previous baseline. This is a change worth monitoring, but this pattern alone cannot determine a medical diagnosis."

---

# 22. Future Scope

Potential future improvements include:

* More sophisticated linguistic analysis
* Improved multilingual NLP
* Better personal-baseline modeling
* Longitudinal trend detection
* Additional cognitive tests
* More keyboard behavioral features
* Improved multimodal feature fusion
* On-device lightweight NLP models
* Offline AI assistant
* More detailed visualization
* Exportable reports
* Clinician-oriented summaries
* User-controlled data deletion
* Secure encrypted local storage

---

# 23. Disclaimer

VGA is a **research/technology prototype for cognitive monitoring and early-warning pattern detection**.

The observations generated by the application are not medical diagnoses and should not be used as a substitute for professional medical evaluation.

Changes in speech, language, typing behavior, or cognitive-test performance can have many possible causes.

If persistent or concerning changes are observed, users should consider discussing them with an appropriately qualified healthcare professional.

---

## Project Philosophy

> **Don't make the AI guess the diagnosis.**
>
> **Measure the behavior. Detect the change. Visualize the pattern. Let AI explain it.**

VGA aims to make cognitive monitoring more continuous, understandable, privacy-conscious, and accessible while keeping the distinction between **technology-assisted monitoring** and **medical diagnosis** clear.
