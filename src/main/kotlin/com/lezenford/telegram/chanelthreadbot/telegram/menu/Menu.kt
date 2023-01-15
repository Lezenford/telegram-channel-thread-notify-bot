package com.lezenford.telegram.chanelthreadbot.telegram.menu

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message

abstract class Menu {
    abstract val id: String

    abstract suspend fun initMessage(message: Message): SendMessage

    abstract suspend fun receiveRequest(query: CallbackQuery): BotApiMethod<*>?
}
