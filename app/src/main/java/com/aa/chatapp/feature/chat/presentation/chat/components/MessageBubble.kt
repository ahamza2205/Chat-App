package com.aa.chatapp.feature.chat.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.graphics.Color as ComposeColor

private val AVATAR_SIZE = 32.dp
private val CORNER_LARGE = 18.dp
private val CORNER_SMALL = 4.dp
private const val MAX_COLLAPSED_LINES = 5

@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    showAvatar: Boolean,
    showName: Boolean,
    onRetry: () -> Unit,
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

    // Max bubble width = 65% of screen width
    val maxBubbleWidth = LocalConfiguration.current.screenWidthDp.dp * 0.65f

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

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                // widthIn: wraps short messages, caps long ones at 65% screen width
                modifier = Modifier.widthIn(max = maxBubbleWidth),
            ) {
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
            }
        }
    }
}

@Composable
private fun ExpandableMessageText(
    text: String,
    contentColor: ComposeColor,
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
            // Only update while collapsed; once expanded we know it was long
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
