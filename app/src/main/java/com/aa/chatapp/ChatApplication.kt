package com.aa.chatapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * @HiltAndroidApp triggers Hilt's code generation and sets up the
 * application-level dependency container.
 *
 * TODO (Phase 3): launch RealtimeMessageSource coroutine here.
 */
@HiltAndroidApp
class ChatApplication : Application()
