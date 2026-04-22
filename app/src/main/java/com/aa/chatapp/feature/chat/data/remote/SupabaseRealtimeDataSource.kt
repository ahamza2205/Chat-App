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
import javax.inject.Inject

class SupabaseRealtimeDataSource @Inject constructor(
    private val dao: MessageDao,
) {
    fun start(scope: CoroutineScope) {
        val channel = supabaseClient.channel("messages-channel")

        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }.mapNotNull { action ->
            if (action is HasRecord) {
                runCatching {
                    remoteJson.decodeFromJsonElement(RemoteMessage.serializer(), action.record)
                        .toEntity()
                }.getOrNull()
            } else null
        }.onEach { entity ->
            dao.insertOrReplace(entity)
        }.launchIn(scope)

        scope.launch {
            supabaseClient.realtime.connect()
            channel.subscribe()
        }
    }
}
