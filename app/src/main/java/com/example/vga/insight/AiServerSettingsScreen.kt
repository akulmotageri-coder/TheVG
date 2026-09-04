package com.example.vga.insight

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


private val WarmBackground = Color(0xFFF9F8F6)
private val Berry = Color(0xFF9E2A4B)
private val Slate = Color(0xFF2D3142)
private val Muted = Color(0xFF6C727F)
private val SoftMuted = Color(0xFF8D99AE)
private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFFF1ECE7)
private val Mint = Color(0xFFE6F4EA)
private val MintText = Color(0xFF137333)
private val Butter = Color(0xFFFFF3CD)
private val ButterText = Color(0xFF854D0E)
private val TrackGrey = Color(0xFFF3F1EE)


/**
 * Lets the user point the app at their own local AI server.
 *
 * The address is stored in SharedPreferences and used for every subsequent
 * request, so a changed DHCP lease only needs editing here rather than a
 * rebuild.
 */
@Composable
fun AiServerSettingsScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urlText by remember { mutableStateOf(AiServerConfig.baseUrl(context)) }
    var modelText by remember { mutableStateOf(AiServerConfig.model(context)) }

    var status by remember { mutableStateOf("Not tested") }
    var connected by remember { mutableStateOf<Boolean?>(null) }
    var detail by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }

    fun normalisedUrl(): String = urlText.trim().trimEnd('/')

    fun isValidUrl(value: String): Boolean =
        (value.startsWith("http://") || value.startsWith("https://")) &&
            value.removePrefix("http://").removePrefix("https://").isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp)
        ) {

            // ---------- top bar ----------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(1.dp, Border, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Slate, fontSize = 26.sp, fontWeight = FontWeight.Light)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "AI Server Settings",
                        color = Slate,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Local, on-device model",
                        color = SoftMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ---------- server url ----------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .border(1.dp, Border, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {

                Text(
                    text = "Server URL",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TrackGrey, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    if (urlText.isEmpty()) {
                        Text(
                            text = "http://192.168.0.10:8080",
                            color = SoftMuted,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = urlText,
                        onValueChange = {
                            urlText = it
                            savedNote = null
                        },
                        textStyle = TextStyle(color = Slate, fontSize = 13.sp),
                        cursorBrush = SolidColor(Berry),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Model",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TrackGrey, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                ) {
                    BasicTextField(
                        value = modelText,
                        onValueChange = {
                            modelText = it
                            savedNote = null
                        },
                        textStyle = TextStyle(color = Slate, fontSize = 13.sp),
                        cursorBrush = SolidColor(Berry),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "The exact id is matched against the server's /v1/models, " +
                        "correcting for capitalisation automatically.",
                    color = SoftMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ---------- status ----------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .border(1.dp, Border, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {

                Text(
                    text = "Status",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                val chipBg = when (connected) {
                    true -> Mint
                    false -> Butter
                    null -> TrackGrey
                }
                val chipFg = when (connected) {
                    true -> MintText
                    false -> ButterText
                    null -> Muted
                }

                Row(
                    modifier = Modifier
                        .background(chipBg, RoundedCornerShape(50.dp))
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(chipFg, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status,
                        color = chipFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                detail?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = it, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }

                savedNote?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = it, color = MintText, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- actions ----------

            PillButton(
                label = "Test Connection",
                background = White,
                textColor = Slate,
                bordered = true,
                enabled = !isBusy,
                onClick = {
                    val candidate = normalisedUrl()

                    if (!isValidUrl(candidate)) {
                        connected = false
                        status = "Invalid URL"
                        detail = "Enter a full address including http:// or https://, " +
                            "for example http://192.168.0.10:8080"
                        return@PillButton
                    }

                    scope.launch {
                        isBusy = true
                        status = "Testing…"
                        connected = null
                        detail = null

                        LocalAiClient(candidate).listModels().fold(
                            onSuccess = { models ->
                                connected = true
                                status = "Connected"
                                detail =
                                    if (models.isEmpty()) {
                                        "Server reachable, but it listed no models."
                                    } else {
                                        "Available: ${models.joinToString()}"
                                    }
                            },
                            onFailure = { error ->
                                connected = false
                                status = "Not connected"
                                detail =
                                    "Unable to connect to AI server. Check the URL and " +
                                        "make sure the server is running on the same " +
                                        "reachable network.\n\n" +
                                        "${error.javaClass.simpleName}: ${error.message}"
                            }
                        )

                        isBusy = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PillButton(
                label = "Save",
                background = Berry,
                textColor = White,
                enabled = !isBusy,
                onClick = {
                    val candidate = normalisedUrl()

                    if (!isValidUrl(candidate)) {
                        connected = false
                        status = "Invalid URL"
                        detail = "Enter a full address including http:// or https://."
                        return@PillButton
                    }

                    AiServerConfig.setBaseUrl(context, candidate)
                    AiServerConfig.setModel(context, modelText)
                    urlText = candidate
                    savedNote = "Saved. All AI requests will use this address."
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Requests stay on your local network. Only the measurements " +
                    "needed for an explanation are sent, never raw audio, " +
                    "transcripts or keystrokes.",
                color = SoftMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}


@Composable
private fun PillButton(
    label: String,
    background: Color,
    textColor: Color,
    bordered: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (enabled) background else background.copy(alpha = 0.5f),
                RoundedCornerShape(50.dp)
            )
            .then(
                if (bordered) Modifier.border(1.dp, Border, RoundedCornerShape(50.dp))
                else Modifier
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
