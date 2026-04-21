package com.aa.chatapp.feature.chat.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (isOwn) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (isOwn) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    val time = remember(message.createdAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            if (!isOwn) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = bubbleColor,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    message.text?.let { text ->
                        Text(
                            text = text,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = time,
                            color = contentColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (isOwn) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (message.status) {
                                    MessageStatus.SENDING -> "⏳"
                                    MessageStatus.SENT -> "✓"
                                    MessageStatus.FAILED -> "✗"
                                },
                                color = contentColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    if (isOwn && message.status == MessageStatus.FAILED) {
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
