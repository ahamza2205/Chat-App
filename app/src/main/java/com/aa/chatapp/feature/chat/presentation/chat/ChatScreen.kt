package com.aa.chatapp.feature.chat.presentation.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aa.chatapp.feature.chat.presentation.chat.components.MessageBubble
import com.aa.chatapp.feature.chat.presentation.chat.components.MessageInputBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ChatEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.currentAvatarUrl != null) {
                            AsyncImage(
                                model = state.currentAvatarUrl,
                                contentDescription = "Your avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MessageInputBar(
                text = state.inputText,
                onTextChange = { viewModel.onIntent(ChatIntent.OnInputChanged(it)) },
                onSend = { viewModel.onIntent(ChatIntent.OnSendClicked) },
            )
        },
    ) { innerPadding ->
        val listState = rememberLazyListState()

        LaunchedEffect(state.messages.size) {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.lastIndex)
            }
        }

        if (state.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No messages yet. Say hello!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(items = state.messages, key = { _, msg -> msg.id }) { index, message ->
                    val isOwn = message.senderId == state.currentUserId
                    val prevSender = state.messages.getOrNull(index - 1)?.senderId
                    val nextSender = state.messages.getOrNull(index + 1)?.senderId
                    val isFirstInGroup = prevSender != message.senderId
                    val isLastInGroup  = nextSender != message.senderId
                    MessageBubble(
                        message = message,
                        isOwn = isOwn,
                        showName   = !isOwn && isFirstInGroup,
                        showAvatar = !isOwn && isLastInGroup,
                        onRetry = { viewModel.onIntent(ChatIntent.OnRetryMessage(message.id)) },
                    )
                }
            }
        }
    }
}
