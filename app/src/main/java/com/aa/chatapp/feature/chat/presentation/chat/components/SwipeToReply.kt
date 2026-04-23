package com.aa.chatapp.feature.chat.presentation.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 80f
private const val DIRECTION_SLOP = 12f

@Composable
fun SwipeToReply(
    isOwn: Boolean,
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(isOwn) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var decided = false
                    var horizontal = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed) {
                            val dx = change.positionChange().x
                            val dy = change.positionChange().y
                            totalX += dx
                            totalY += dy

                            if (!decided && (abs(totalX) > DIRECTION_SLOP || abs(totalY) > DIRECTION_SLOP)) {
                                horizontal = abs(totalX) > abs(totalY)
                                decided = true
                            }

                            if (decided && horizontal) {
                                change.consume()
                                val next = (offsetX.value + dx).coerceIn(
                                    if (isOwn) -SWIPE_THRESHOLD else 0f,
                                    if (isOwn) 0f else SWIPE_THRESHOLD,
                                )
                                scope.launch { offsetX.snapTo(next) }
                            } else if (decided && !horizontal) {
                                break
                            }
                        } else {
                            if (horizontal) {
                                scope.launch {
                                    if (!isOwn && offsetX.value > SWIPE_THRESHOLD * 0.75f) onReply()
                                    if (isOwn && offsetX.value < -SWIPE_THRESHOLD * 0.75f) onReply()
                                    offsetX.animateTo(0f)
                                }
                            }
                            break
                        }
                    }
                }
            },
    ) {
        val iconAlpha = (abs(offsetX.value) / SWIPE_THRESHOLD).coerceIn(0f, 1f)
        if (iconAlpha > 0.05f) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Reply",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha),
                modifier = Modifier
                    .align(if (isOwn) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 12.dp),
            )
        }
        Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) {
            content()
        }
    }
}


