package com.aa.chatapp.feature.chat.presentation.chat.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.ReplyPreview
import com.aa.chatapp.feature.chat.presentation.chat.audio.AudioRecorder
import kotlinx.coroutines.delay

@Composable
fun MessageInputBar(
    text: String,
    attachments: List<Attachment>,
    replyingTo: ReplyPreview?,
    onClearReply: () -> Unit,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onMicClick: () -> Unit,
    onVoiceNoteReady: (Attachment) -> Unit,
    micPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val hasContent = text.isNotBlank() || attachments.isNotEmpty()

    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    val amplitudes = remember { mutableStateListOf<Float>() }

    // Poll amplitude + timer while recording
    LaunchedEffect(isRecording, recordingPaused) {
        if (!isRecording) return@LaunchedEffect
        while (isRecording && !recordingPaused) {
            delay(100)
            val amp = recorder.getMaxAmplitude()
            amplitudes.add((amp.toFloat() / 32767f).coerceIn(0.05f, 1f))
            recordingSeconds = amplitudes.size.toLong() / 10
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    LaunchedEffect(replyingTo) {
        if (replyingTo != null) focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier.fillMaxWidth().imePadding(),
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            replyingTo?.let {
                ReplyPreviewBar(reply = it, onDismiss = onClearReply)
            }
            if (attachments.isNotEmpty() && !isRecording) {
                AttachmentPreviewStrip(attachments = attachments, onRemove = onRemoveAttachment)
            }

            if (isRecording) {
                RecordingBar(
                    seconds = recordingSeconds,
                    amplitudes = amplitudes,
                    isPaused = recordingPaused,
                    onPauseResume = {
                        if (recordingPaused) {
                            recorder.resume()
                            recordingPaused = false
                        } else {
                            recorder.pause()
                            recordingPaused = true
                        }
                    },
                    onDelete = {
                        isRecording = false
                        recordingPaused = false
                        recorder.cancel()
                        amplitudes.clear()
                    },
                    onSend = {
                        isRecording = false
                        recordingPaused = false
                        val waveformSnapshot = amplitudes.toList()
                        val result = recorder.stop()
                        amplitudes.clear()
                        if (result != null) {
                            val (file, durationMs) = result
                            onVoiceNoteReady(
                                Attachment(
                                    id = java.util.UUID.randomUUID().toString(),
                                    localUri = android.net.Uri.fromFile(file).toString(),
                                    mimeType = "audio/mp4",
                                    fileName = file.name,
                                    durationMs = durationMs,
                                    waveform = waveformSnapshot,
                                )
                            )
                        }
                    },
                )
            } else {
                NormalInputRow(
                    text = text,
                    hasContent = hasContent,
                    focusRequester = focusRequester,
                    micPermissionGranted = micPermissionGranted,
                    onTextChange = onTextChange,
                    onSend = onSend,
                    onAttach = onAttach,
                    onMicClick = {
                        if (micPermissionGranted) {
                            recorder.start()
                            amplitudes.clear()
                            isRecording = true
                            recordingPaused = false
                        } else {
                            onMicClick()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NormalInputRow(
    text: String,
    hasContent: Boolean,
    focusRequester: FocusRequester,
    micPermissionGranted: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onMicClick: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAttach) {
            Icon(Icons.Default.AttachFile, "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Message") },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            maxLines = 4,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        if (hasContent) {
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            IconButton(onClick = onMicClick) {
                Icon(
                    if (micPermissionGranted) Icons.Default.Mic else Icons.Default.MicOff,
                    "Record voice note",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RecordingBar(
    seconds: Long,
    amplitudes: List<Float>,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit,
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { 3.dp.toPx() }
    val barGapPx = with(density) { 2.dp.toPx() }
    val cornerRadius = with(density) { 1.5.dp.toPx() }
    val waveColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        // Timer + Waveform
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulsing dot
            val dotColor = if (!isPaused && seconds % 2 == 0L)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.error.copy(alpha = if (isPaused) 0.4f else 0.5f)
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(dotColor)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${seconds / 60}:%02d".format(seconds % 60),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(10.dp))
            // Live waveform
            Canvas(modifier = Modifier.weight(1f).height(36.dp)) {
                val barStep = barWidthPx + barGapPx
                val maxBars = (size.width / barStep).toInt()
                val visible = if (amplitudes.size > maxBars) {
                    amplitudes.subList(amplitudes.size - maxBars, amplitudes.size)
                } else amplitudes

                visible.forEachIndexed { i, amp ->
                    val barH = (amp.coerceIn(0.05f, 1f) * (size.height - 4)).coerceAtLeast(3f)
                    val x = i * barStep
                    val y = (size.height - barH) / 2
                    drawRoundRect(
                        color = waveColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidthPx, barH),
                        cornerRadius = CornerRadius(cornerRadius),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action buttons: Delete | Pause/Resume | Send
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Delete recording",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            // Pause / Resume
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(2.dp, MaterialTheme.colorScheme.error, CircleShape)
                    .clip(CircleShape)
                    .clickable { onPauseResume() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    if (isPaused) "Resume" else "Pause",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
            }
            // Send
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clip(CircleShape)
                    .clickable { onSend() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send voice note",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AttachmentPreviewStrip(
    attachments: List<Attachment>,
    onRemove: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        items(items = attachments, key = { it.id }) { attachment ->
            if (attachment.mimeType.startsWith("audio/")) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(64.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Mic, null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            val dur = attachment.durationMs ?: 0L
                            Text(
                                "${dur / 1000 / 60}:%02d".format(dur / 1000 % 60),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .clickable { onRemove(attachment.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Close, "Remove",
                            tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(14.dp))
                    }
                }
            } else {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    AsyncImage(
                        model = attachment.localUri ?: attachment.remoteUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .clickable { onRemove(attachment.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Close, "Remove",
                            tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        item {
            Text("${attachments.size}/10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.height(64.dp).padding(start = 4.dp))
        }
    }
}

@Composable
private fun ReplyPreviewBar(reply: ReplyPreview, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(3.dp).height(36.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(reply.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            val preview = when {
                reply.isMedia -> "📷 Photo"
                reply.textPreview != null -> reply.textPreview
                else -> ""
            }
            Text(preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, "Clear reply",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
