package com.aa.chatapp.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * General application-level bindings.
 *
 * TODO (Phase 2):
 *  - @Binds @Singleton fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
 *
 * Note: Use @Binds (not @Provides) for interface→implementation bindings —
 * it avoids generating an unnecessary factory class.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule
