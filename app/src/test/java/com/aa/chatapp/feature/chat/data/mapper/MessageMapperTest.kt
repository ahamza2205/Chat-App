package com.aa.chatapp.feature.chat.data.mapper

import com.aa.chatapp.feature.chat.data.local.entity.MessageEntity
import com.aa.chatapp.feature.chat.domain.model.Attachment
import com.aa.chatapp.feature.chat.domain.model.Message
import com.aa.chatapp.feature.chat.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class  MessageMapperTest {

    @Test
    fun `toDomain maps entity to domain model correctly`() {
        val entity = MessageEntity(
            id = "msg1",
            senderId = "user1",
            senderName = "Alice",
            senderAvatarUrl = "http://alice.com/avatar.jpg",
            text = "Hello",
            attachments = listOf(Attachment(id = "att1", localUri = "file://local.jpg", remoteUrl = "http://remote.jpg", mimeType = "image/jpeg")),
            status = "SENDING",
            createdAt = 1000L,
            failedReason = "Network error"
        )

        val domain = entity.toDomain()

        assertEquals("msg1", domain.id)
        assertEquals("user1", domain.senderId)
        assertEquals("Alice", domain.senderName)
        assertEquals("http://alice.com/avatar.jpg", domain.senderAvatarUrl)
        assertEquals("Hello", domain.text)
        assertEquals(MessageStatus.SENDING, domain.status)
        assertEquals(1000L, domain.createdAt)
        assertEquals("Network error", domain.failedReason)
        assertEquals(1, domain.attachments.size)
        assertEquals("att1", domain.attachments[0].id)
        assertEquals("file://local.jpg", domain.attachments[0].localUri)
        assertEquals("http://remote.jpg", domain.attachments[0].remoteUrl)
    }

    @Test
    fun `toEntity maps domain model to entity correctly`() {
        val domain = Message(
            id = "msg2",
            senderId = "user2",
            senderName = "Bob",
            senderAvatarUrl = null,
            text = "Hi",
            attachments = listOf(Attachment(id = "att2", localUri = null, remoteUrl = "http://remote2.jpg", mimeType = "image/png")),
            status = MessageStatus.SENT,
            createdAt = 2000L,
            failedReason = null
        )

        val entity = domain.toEntity()

        assertEquals("msg2", entity.id)
        assertEquals("user2", entity.senderId)
        assertEquals("Bob", entity.senderName)
        assertEquals(null, entity.senderAvatarUrl)
        assertEquals("Hi", entity.text)
        assertEquals("SENT", entity.status)
        assertEquals(2000L, entity.createdAt)
        assertEquals(null, entity.failedReason)
        assertEquals(1, entity.attachments.size)
    }
}
