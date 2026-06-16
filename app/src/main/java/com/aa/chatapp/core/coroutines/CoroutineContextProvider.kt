package com.aa.chatapp.core.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

interface CoroutineContextProvider {
    val main: CoroutineContext
    val io: CoroutineContext
    val default: CoroutineContext
}

@Singleton
class DefaultCoroutineContextProvider @Inject constructor() : CoroutineContextProvider {
    override val main: CoroutineContext get() = Dispatchers.Main
    override val io: CoroutineContext get() = Dispatchers.IO
    override val default: CoroutineContext get() = Dispatchers.Default
}
