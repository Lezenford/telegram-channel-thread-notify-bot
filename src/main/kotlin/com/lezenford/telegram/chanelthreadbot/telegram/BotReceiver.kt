@file:OptIn(DelicateCoroutinesApi::class)

package com.lezenford.telegram.chanelthreadbot.telegram

import com.lezenford.telegram.chanelthreadbot.service.UpdateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlin.coroutines.CoroutineContext

abstract class BotReceiver : CoroutineScope {
    protected abstract val updateService: UpdateService

    override val coroutineContext: CoroutineContext = newSingleThreadContext("longPollingPool")
}