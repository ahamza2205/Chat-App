package com.aa.chatapp.feature.chat.presentation.chat.components

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aa.chatapp.feature.chat.domain.model.Attachment
import kotlinx.coroutines.delay

private const val BAR_WIDTH_DP = 3f
private const val BAR_GAP_DP = 2f
private const val THUMB_RADIUS_DP = 5f

@Composable
fun AudioBubble(
    attachment: Attachment,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val source = attachment.localUri ?: attachment.remoteUrl
    val totalDuration = attachment.durationMs ?: 0L
    val waveform = attachment.waveform ?: generateFallbackWaveform()

    val player = remember { MediaPlayer() }
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableLongStateOf(0L) }

    // Update progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying && isPrepared) {
            elapsed = player.currentPosition.toLong()
            progress = (elapsed.toFloat() / player.duration.coerceAtLeast(1)).coerceIn(0f, 1f)
            delay(50)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { player.release() } catch (_: Exception) {}
        }
    }

    fun ensurePrepared(): Boolean {
        if (isPrepared) return true
        if (source == null) return false
        return try {
            player.reset()
            player.setDataSource(source)
            player.prepare()
            player.setOnCompletionListener {
                isPlaying = false
                progress = 0f
                elapsed = 0L
            }
            isPrepared = true
            true
        } catch (_: Exception) { false }
    }

    fun togglePlay() {
        if (!ensurePrepared()) return
        if (isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.start()
            isPlaying = true
        }
    }

    fun seekTo(fraction: Float) {
        if (!ensurePrepared()) return
        val pos = (fraction * player.duration).toInt()
        player.seekTo(pos)
        progress = fraction
        elapsed = pos.toLong()
    }

    val playedColor = contentColor
    val unplayedColor = contentColor.copy(alpha = 0.3f)
    val thumbColor = contentColor

    Column(modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Play/Pause button
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f),
                onClick = { togglePlay() },
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            // Waveform with seek
            WaveformWithSeek(
                amplitudes = waveform,
                progress = progress,
                playedColor = playedColor,
                unplayedColor = unplayedColor,
                thumbColor = thumbColor,
                onSeek = { seekTo(it) },
                modifier = Modifier.weight(1f).height(32.dp),
            )
        }
        // Elapsed / total duration
        Text(
            text = formatDuration(if (isPlaying || elapsed > 0) elapsed else totalDuration),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.6f),
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 44.dp, top = 2.dp),
        )
    }
}

@Composable
private fun WaveformWithSeek(
    amplitudes: List<Float>,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    thumbColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { BAR_WIDTH_DP.dp.toPx() }
    val barGapPx = with(density) { BAR_GAP_DP.dp.toPx() }
    val thumbRadiusPx = with(density) { THUMB_RADIUS_DP.dp.toPx() }
    val cornerRadius = with(density) { 1.5.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                },
        ) {
            val totalWidth = size.width
            val height = size.height
            val barStep = barWidthPx + barGapPx
            val barCount = ((totalWidth - thumbRadiusPx) / barStep).toInt().coerceAtLeast(1)
            val resampled = resample(amplitudes, barCount)
            val thumbX = progress * totalWidth

            resampled.forEachIndexed { i, amp ->
                val x = i * barStep
                val barH = (amp.coerceIn(0.05f, 1f) * (height - 4)).coerceAtLeast(3f)
                val y = (height - barH) / 2
                val color = if (x + barWidthPx <= thumbX) playedColor else unplayedColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidthPx, barH),
                    cornerRadius = CornerRadius(cornerRadius),
                )
            }

            // Thumb circle
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(thumbX.coerceIn(thumbRadiusPx, totalWidth - thumbRadiusPx), height / 2),
            )
        }
    }
}

private fun resample(data: List<Float>, targetCount: Int): List<Float> {
    if (data.isEmpty()) return List(targetCount) { 0.1f }
    if (data.size <= targetCount) return data
    val window = data.size.toFloat() / targetCount
    return List(targetCount) { i ->
        val start = (i * window).toInt()
        val end = ((i + 1) * window).toInt().coerceAtMost(data.size)
        data.subList(start, end).maxOrNull() ?: 0.05f
    }
}

private fun generateFallbackWaveform(): List<Float> {
    return List(40) { ((it * 7 + 13) % 17) / 17f * 0.7f + 0.1f }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:%02d".format(seconds)
}
