package com.aa.chatapp.feature.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aa.chatapp.feature.chat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE hiddenForMe = 0 ORDER BY createdAt ASC")
    fun observeMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(message: MessageEntity)

    @Query("""
        INSERT OR REPLACE INTO messages (id, senderId, senderName, senderAvatarUrl, text, attachments, status, createdAt, failedReason, replyPreview, isDeletedForEveryone, hiddenForMe)
        VALUES (:id, :senderId, :senderName, :senderAvatarUrl, :text, :attachments, :status, :createdAt, :failedReason, :replyPreview, :isDeletedForEveryone,
            COALESCE((SELECT hiddenForMe FROM messages WHERE id = :id), 0))
    """)
    suspend fun upsertPreservingHidden(
        id: String, senderId: String, senderName: String, senderAvatarUrl: String?,
        text: String?, attachments: String, status: String, createdAt: Long,
        failedReason: String?, replyPreview: String?, isDeletedForEveryone: Boolean,
    )

    @Query("UPDATE messages SET status = :status, failedReason = :failedReason WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String, failedReason: String?)

    @Query("UPDATE messages SET status = :status, failedReason = NULL WHERE id = :messageId")
    suspend fun resetMessageStatus(messageId: String, status: String)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("UPDATE messages SET hiddenForMe = 1 WHERE id = :messageId")
    suspend fun hideMessage(messageId: String)

    @Query("UPDATE messages SET isDeletedForEveryone = 1, text = NULL, attachments = '[]' WHERE id = :messageId")
    suspend fun softDeleteForEveryone(messageId: String)
}
