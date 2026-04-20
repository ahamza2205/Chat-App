package com.aa.chatapp.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides Room database and DAO instances.
 *
 * TODO (Phase 2):
 *  - @Provides @Singleton fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase
 *  - @Provides fun provideMessageDao(db: AppDatabase): MessageDao
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
