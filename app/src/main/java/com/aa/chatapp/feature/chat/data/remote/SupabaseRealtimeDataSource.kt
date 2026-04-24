package com.aa.chatapp.feature.chat.data.remote

import com.aa.chatapp.core.network.supabaseClient
import com.aa.chatapp.feature.chat.data.local.dao.MessageDao
import io.github.jan.supabase.realtime.HasRecord
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject

import io.github.jan.supabase.postgrest.postgrest

class SupabaseRealtimeDataSource @Inject constructor(
    private val dao: MessageDao,
) {
    fun start(scope: CoroutineScope) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val remotes = supabaseClient.postgrest["messages"].select().decodeList<RemoteMessage>()
                remotes.forEach { remote ->
                    dao.upsertPreservingHidden(
                        id = remote.id,
                        senderId = remote.senderId,
                        senderName = remote.senderName,
                        senderAvatarUrl = remote.senderAvatarUrl,
                        text = remote.text,
                        attachments = remote.attachments,
                        status = remote.status,
                        createdAt = remote.createdAt,
                        failedReason = null,
                        replyPreview = remote.replyPreview,
                        isDeletedForEveryone = remote.isDeletedForEveryone,
                    )
                }
            }
        }

        val channel = supabaseClient.channel("messages-channel")

        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }.mapNotNull { action ->
            if (action is HasRecord) {
                runCatching {
                    remoteJson.decodeFromJsonElement(RemoteMessage.serializer(), action.record)
                }.getOrNull()
            } else null
        }.onEach { remote ->
            dao.upsertPreservingHidden(
                id = remote.id,
                senderId = remote.senderId,
                senderName = remote.senderName,
                senderAvatarUrl = remote.senderAvatarUrl,
                text = remote.text,
                attachments = remote.attachments,
                status = remote.status,
                createdAt = remote.createdAt,
                failedReason = null,
                replyPreview = remote.replyPreview,
                isDeletedForEveryone = remote.isDeletedForEveryone,
            )
        }.launchIn(scope)

        scope.launch {
            supabaseClient.realtime.connect()
            channel.subscribe()
        }
    }
}
