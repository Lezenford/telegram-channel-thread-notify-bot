package com.lezenford.telegram.chanelthreadbot.telegram.command

import com.lezenford.telegram.chanelthreadbot.service.db.ChannelStorageService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class UnbindCommand(
    private val channelStorageService: ChannelStorageService,
    private val botSender: BotSender
) : Command() {
    override val command: String = "unbind"
    override val description: String = "Unbind and forget channel"

    override suspend fun action(update: Update) {
        when {
            update.hasChannelPost() -> {
                channelStorageService.unbind(update.channelPost.chatId)

                botSender.sendMessage(
                    EditMessageText.builder()
                        .chatId(update.channelPost.chatId)
                        .messageId(update.channelPost.messageId)
                        .text("Bind successfully removed")
                        .build()
                )
            }
        }
    }
}
