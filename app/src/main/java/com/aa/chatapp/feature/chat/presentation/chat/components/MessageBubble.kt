package com.aa.chatapp.feature.chat.presentation.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import com.aa.chatapp.feature.chat.domain.model.ReplyPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AVATAR_SIZE = 32.dp
private val CORNER_LARGE = 18.dp
private val CORNER_SMALL = 4.dp
private const val MAX_COLLAPSED_LINES = 5
private const val GRID_VISIBLE_COUNT = 4

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    showAvatar: Boolean,
    showName: Boolean,
    onRetry: () -> Unit,
    onReply: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onImageClick: (attachments: List<Attachment>, startIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isOwn) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (isOwn) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(message.createdAt))

    val bubbleShape = if (isOwn) {
        RoundedCornerShape(CORNER_LARGE, CORNER_LARGE, CORNER_SMALL, CORNER_LARGE)
    } else {
        when {
            showName && showAvatar -> RoundedCornerShape(CORNER_SMALL, CORNER_LARGE, CORNER_LARGE, CORNER_LARGE)
            showName               -> RoundedCornerShape(CORNER_SMALL, CORNER_LARGE, CORNER_LARGE, CORNER_SMALL)
            showAvatar             -> RoundedCornerShape(CORNER_SMALL, CORNER_LARGE, CORNER_LARGE, CORNER_LARGE)
            else                   -> RoundedCornerShape(CORNER_SMALL, CORNER_LARGE, CORNER_LARGE, CORNER_SMALL)
        }
    }

    val topPadding    = if (showName || isOwn) 6.dp else 1.dp
    val bottomPadding = if (showAvatar || isOwn) 2.dp else 1.dp
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.65f

    SwipeToReply(
        isOwn = isOwn,
        onReply = onReply,
        enabled = !message.isDeletedForEveryone
    ) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (isOwn) 56.dp else 8.dp, end = if (isOwn) 8.dp else 56.dp)
            .padding(top = topPadding, bottom = bottomPadding),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isOwn) {
            Box(modifier = Modifier.size(AVATAR_SIZE)) {
                if (showAvatar) SenderAvatar(message.senderName, message.senderAvatarUrl)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            if (!isOwn && showName) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            val haptic = LocalHapticFeedback.current

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (!message.isDeletedForEveryone) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenu = true
                            }
                        },
                    ),
            ) {
                Box {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete for me") },
                            onClick = { showMenu = false; onDeleteForMe() },
                        )
                        if (isOwn) {
                            DropdownMenuItem(
                                text = { Text("Delete for everyone", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDeleteForEveryone() },
                            )
                        }
                    }
                Column {
                    if (message.isDeletedForEveryone) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "\uD83D\uDEAB This message was deleted",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = contentColor.copy(alpha = 0.6f),
                            )
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(time, color = contentColor.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    } else {
                    val audioAttachments = message.attachments.filter { it.mimeType.startsWith("audio/") }
                    val imageAttachments = message.attachments.filter { !it.mimeType.startsWith("audio/") }

                    message.replyPreview?.let { reply ->
                        ReplyBlock(reply = reply, isOwn = isOwn)
                    }

                    audioAttachments.forEach { audio ->
                        AudioBubble(
                            attachment = audio,
                            contentColor = contentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }

                    if (imageAttachments.isNotEmpty()) {
                        ImageGrid(
                            attachments = imageAttachments,
                            bubbleShape = bubbleShape,
                            hasText = message.text != null,
                            onImageClick = { index -> onImageClick(imageAttachments, index) },
                        )
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        message.text?.let { text ->
                            ExpandableMessageText(
                                text = text,
                                contentColor = contentColor,
                            )
                        }

                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = time,
                                color = contentColor.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                            )
                            if (isOwn) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = when (message.status) {
                                        MessageStatus.SENDING -> "⏳"
                                        MessageStatus.SENT    -> "✓"
                                        MessageStatus.FAILED  -> "✗"
                                    },
                                    color = contentColor.copy(alpha = 0.65f),
                                    fontSize = 10.sp,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }

                        }

                        if (isOwn && message.status == MessageStatus.FAILED) {
                            TextButton(
                                onClick = onRetry,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Retry", style = MaterialTheme.typography.labelSmall, color = contentColor)
                            }
                        }
                        }
                    } // else (not deleted)
                }
                } // Box
            }
        }
    }
    } // end SwipeToReply
}



@Composable
private fun ReplyBlock(
    reply: ReplyPreview,
    isOwn: Boolean,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val bgColor = if (isOwn)
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(accentColor, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = reply.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
            )
            val preview = when {
                reply.isMedia -> "📷 Photo"
                reply.textPreview != null -> reply.textPreview
                else -> ""
            }
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpandableMessageText(
    text: String,
    contentColor: Color,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by rememberSaveable { mutableStateOf(false) }

    Text(
        text = text,
        color = contentColor,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = if (expanded) Int.MAX_VALUE else MAX_COLLAPSED_LINES,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result: TextLayoutResult ->
            if (!expanded) overflows = result.hasVisualOverflow
        },
    )

    if (overflows || expanded) {
        Text(
            text = if (expanded) "Read less" else "Read more",
            color = contentColor.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable { expanded = !expanded },
        )
    }
}

@Composable
private fun SenderAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ImageGrid(
    attachments: List<Attachment>,
    bubbleShape: RoundedCornerShape,
    hasText: Boolean,
    onImageClick: (index: Int) -> Unit,
) {
    val zeroCorner = CornerSize(0.dp)
    val shape = if (hasText) {
        RoundedCornerShape(
            topStart = bubbleShape.topStart,
            topEnd = bubbleShape.topEnd,
            bottomStart = zeroCorner,
            bottomEnd = zeroCorner,
        )
    } else bubbleShape

    val visible = attachments.take(GRID_VISIBLE_COUNT)
    val remaining = if (attachments.size > 4) attachments.size - 3 else 0

    when (attachments.size) {
        1 -> {
            AsyncImage(
                model = attachments[0].localUri ?: attachments[0].remoteUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .clip(shape)
                    .clickable { onImageClick(0) },
            )
        }
        2 -> {
            Row(
                modifier = Modifier.clip(shape),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                visible.forEachIndexed { i, att ->
                    AsyncImage(
                        model = att.localUri ?: att.remoteUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable { onImageClick(i) },
                    )
                }
            }
        }
        3 -> {
            Column(
                modifier = Modifier.clip(shape),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AsyncImage(
                    model = visible[0].localUri ?: visible[0].remoteUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { onImageClick(0) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..2) {
                        AsyncImage(
                            model = visible[i].localUri ?: visible[i].remoteUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clickable { onImageClick(i) },
                        )
                    }
                }
            }
        }
        else -> {
            // 4+ images: 2x2 grid, 4th slot shows "+N" overlay if >4
            Column(
                modifier = Modifier.clip(shape),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Top row
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 0..1) {
                        AsyncImage(
                            model = visible[i].localUri ?: visible[i].remoteUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clickable { onImageClick(i) },
                        )
                    }
                }
                // Bottom row
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    AsyncImage(
                        model = visible[2].localUri ?: visible[2].remoteUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { onImageClick(2) },
                    )
                    // 4th slot: either plain image or "+N" overlay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable {
                                if (remaining > 0) onImageClick(3)
                                else onImageClick(3)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = visible[3].localUri ?: visible[3].remoteUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = if (remaining > 0) {
                                Modifier.fillMaxSize().blur(4.dp)
                            } else {
                                Modifier.fillMaxSize()
                            },
                        )
                        if (remaining > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+$remaining",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
