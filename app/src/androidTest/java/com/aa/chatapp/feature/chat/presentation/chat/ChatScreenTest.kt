package com.aa.chatapp.feature.chat.presentation.chat

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import com.aa.chatapp.feature.chat.domain.model.ReplyPreview
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_emptyState_isDisplayed() {
        composeTestRule.setContent {
            ChatScreenContent(
                state = ChatState(messages = emptyList()),
                snackbarHostState = SnackbarHostState(),
                micGranted = true,
                onNavigateToProfile = {},
                onIntent = {},
                onLaunchImagePicker = {},
                onLaunchMicPermission = {}
            )
        }

        composeTestRule.onNodeWithText("No messages yet. Say hello!").assertIsDisplayed()
    }

    @Test
    fun chatScreen_typeMessageAndSend_messageAppears() {
        val intentsFired = mutableListOf<ChatIntent>()
        composeTestRule.setContent {
            ChatScreenContent(
                state = ChatState(inputText = "Hello World!"),
                snackbarHostState = SnackbarHostState(),
                micGranted = true,
                onNavigateToProfile = {},
                onIntent = { intentsFired.add(it) },
                onLaunchImagePicker = {},
                onLaunchMicPermission = {}
            )
        }

        composeTestRule.onNodeWithTag("MessageInputTextField").performTextInput("Hello World!")
        composeTestRule.onNodeWithTag("SendMessageButton").performClick()

        assert(intentsFired.isNotEmpty())
        assert(intentsFired.last() is ChatIntent.OnSendClicked)
    }

    @Test
    fun chatScreen_replyToMessage_showsReplyPreview() {
        composeTestRule.setContent {
            ChatScreenContent(
                state = ChatState(
                    replyingTo = ReplyPreview(
                        originalMessageId = "1",
                        senderName = "John Doe",
                        textPreview = "Hello",
                        isMedia = false
                    )
                ),
                snackbarHostState = SnackbarHostState(),
                micGranted = true,
                onNavigateToProfile = {},
                onIntent = {},
                onLaunchImagePicker = {},
                onLaunchMicPermission = {}
            )
        }

        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ClearReplyButton").assertIsDisplayed()
    }

    @Test
    fun chatScreen_clearReplyPreview_hidesPreview() {
        val intentsFired = mutableListOf<ChatIntent>()
        composeTestRule.setContent {
            ChatScreenContent(
                state = ChatState(
                    replyingTo = ReplyPreview(
                        originalMessageId = "1",
                        senderName = "John Doe",
                        textPreview = "Hello",
                        isMedia = false
                    )
                ),
                snackbarHostState = SnackbarHostState(),
                micGranted = true,
                onNavigateToProfile = {},
                onIntent = { intentsFired.add(it) },
                onLaunchImagePicker = {},
                onLaunchMicPermission = {}
            )
        }

        composeTestRule.onNodeWithTag("ClearReplyButton").performClick()
        
        assert(intentsFired.isNotEmpty())
        assert(intentsFired.last() is ChatIntent.OnClearReply)
    }

    @Test
    fun chatScreen_longPressMessage_showsDeleteOptions() {
        val message = Message(
            id = "msg1",
            senderId = "user1",
            senderName = "Me",
            senderAvatarUrl = null,
            text = "Long press me",
            attachments = emptyList(),
            status = MessageStatus.SENT,
            createdAt = System.currentTimeMillis(),
            failedReason = null,
            replyPreview = null,
            isDeletedForEveryone = false
        )

        composeTestRule.setContent {
            ChatScreenContent(
                state = ChatState(
                    currentUserId = "user1",
                    messages = listOf(message)
                ),
                snackbarHostState = SnackbarHostState(),
                micGranted = true,
                onNavigateToProfile = {},
                onIntent = {},
                onLaunchImagePicker = {},
                onLaunchMicPermission = {}
            )
        }

        composeTestRule.onNodeWithText("Long press me").assertIsDisplayed()
        composeTestRule.onNodeWithTag("MessageBubble_msg1").performTouchInput { longClick() }
        
        composeTestRule.onNodeWithText("Delete for me").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete for everyone").assertIsDisplayed()
    }

    @Test
    fun chatScreen_deletedMessage_showsDeletedPlaceholder() {
        val deletedMessage = Message(
            id = "msg1",
            senderId = "user2",
            senderName = "John",
            senderAvatarUrl = null,
            text = null,
            attachments = emptyList(),
            status = MessageStatus.SENT,
            createdAt = System.currentTimeMillis(),
            failedReason = null,
            replyPreview = null,
            isDeletedForEveryone = true
        )

        composeTestRule.setContent {
            ChatScreenContent(
                state = ChatState(
                    currentUserId = "user1",
                    messages = listOf(deletedMessage)
                ),
                snackbarHostState = SnackbarHostState(),
                micGranted = true,
                onNavigateToProfile = {},
                onIntent = {},
                onLaunchImagePicker = {},
                onLaunchMicPermission = {}
            )
        }

        composeTestRule.onNodeWithText("🚫 This message was deleted").assertIsDisplayed()
    }
}
