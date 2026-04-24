package com.aa.chatapp.feature.chat.presentation.chat

import com.aa.chatapp.core.datastore.UserPreferencesDataSource
import com.aa.chatapp.feature.chat.domain.usecase.DeleteForEveryoneUseCase
import com.aa.chatapp.feature.chat.domain.usecase.DeleteForMeUseCase
import com.aa.chatapp.feature.chat.domain.usecase.InsertPendingMessageUseCase
import com.aa.chatapp.feature.chat.domain.usecase.ObserveMessagesUseCase
import com.aa.chatapp.feature.chat.domain.usecase.RetryMessageUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var observeMessages: ObserveMessagesUseCase
    private lateinit var insertPendingMessage: InsertPendingMessageUseCase
    private lateinit var retryMessage: RetryMessageUseCase
    private lateinit var deleteForMe: DeleteForMeUseCase
    private lateinit var deleteForEveryone: DeleteForEveryoneUseCase
    private lateinit var userPrefs: UserPreferencesDataSource
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        
        observeMessages = mockk()
        insertPendingMessage = mockk()
        retryMessage = mockk()
        deleteForMe = mockk()
        deleteForEveryone = mockk()
        userPrefs = mockk()

        every { observeMessages.invoke() } returns flowOf(emptyList())
        every { userPrefs.userId } returns flowOf("u1")
        every { userPrefs.userName } returns flowOf("u1-name")
        every { userPrefs.avatarUrl } returns flowOf(null)

        viewModel = ChatViewModel(observeMessages, insertPendingMessage, retryMessage, deleteForMe, deleteForEveryone, userPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onInputChanged updates inputText state`() = runTest {
        viewModel.onIntent(ChatIntent.OnInputChanged("typing..."))
        
        assertEquals("typing...", viewModel.state.value.inputText)
    }

    @Test
    fun `onSendClicked with empty input does not call insertPendingMessage`() = runTest {
        viewModel.onIntent(ChatIntent.OnInputChanged("   "))
        viewModel.onIntent(ChatIntent.OnSendClicked)

        coVerify(exactly = 0) { insertPendingMessage.invoke(any()) }
    }

    @Test
    fun `onSendClicked inserts message and clears input state`() = runTest {
        coEvery { insertPendingMessage.invoke(any()) } just Runs
        
        viewModel.onIntent(ChatIntent.OnInputChanged("Hello test"))
        viewModel.onIntent(ChatIntent.OnSendClicked)

        coVerify(exactly = 1) { insertPendingMessage.invoke(match { it.text == "Hello test" }) }
        assertEquals("", viewModel.state.value.inputText)
        assertEquals(emptyList<Any>(), viewModel.state.value.selectedAttachments)
    }

    @Test
    fun `onRetryMessage calls retryMessageUseCase`() = runTest {
        coEvery { retryMessage.invoke("msg_123") } just Runs
        
        viewModel.onIntent(ChatIntent.OnRetryMessage("msg_123"))

        coVerify { retryMessage.invoke("msg_123") }
    }
}
