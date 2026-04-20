package com.aa.chatapp.feature.chat.data.worker

// TODO (Phase 2): Implement SendMessageWorker.
//
// class SendMessageWorker @AssistedInject constructor(
//     @Assisted appContext: Context,
//     @Assisted workerParams: WorkerParameters,
//     private val messageDao: MessageDao,
//     private val supabaseStorageSource: SupabaseStorageSource,
// ) : CoroutineWorker(appContext, workerParams) {
//
//     override suspend fun doWork(): Result {
//         val messageId = inputData.getString(KEY_MESSAGE_ID) ?: return Result.failure()
//         // 1. Read pending entity from Room.
//         // 2. Upload local attachments → get CDN URLs.
//         // 3. POST to Supabase `messages` table.
//         // 4. messageDao.updateStatus(id, SENT) on success, FAILED on error.
//     }
//
//     companion object {
//         const val KEY_MESSAGE_ID = "message_id"
//     }
// }
