package com.aa.chatapp.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides the Supabase client singleton.
 *
 * TODO (Phase 2):
 *  - @Provides @Singleton fun provideSupabaseClient(): SupabaseClient
 *      → built from SupabaseClientProvider using BuildConfig.SUPABASE_URL / ANON_KEY
 *  - @Provides @Singleton fun provideSupabaseStorageSource(client: SupabaseClient): SupabaseStorageSource
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule
