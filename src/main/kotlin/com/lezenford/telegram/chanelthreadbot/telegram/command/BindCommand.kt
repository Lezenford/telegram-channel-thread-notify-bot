package com.lezenford.telegram.chanelthreadbot.telegram.command

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.service.ChannelService
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelStorageService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.UUID

@Component
class BindCommand(
    private val channelStorageService: ChannelStorageService,
    private val channelService: ChannelService,
    private val botSender: BotSender
) : Command() {
    override val command: String = "bind"
    override val description: String = "Bind channel with replay group"

    override suspend fun action(update: Update) {
        when {
            update.hasChannelPost() -> {
                val editMessage =
                    if (channelStorageService.findById(update.channelPost.chatId) == null) {
                        val key = UUID.randomUUID().toString()
                        channelStorageService.registerInvitation(
                            channelId = update.channelPost.chatId,
                            messageId = update.channelPost.messageId,
                            key = key
                        )

                        log.info("Has received new bind request from channel ${update.channelPost.chat.title}")

                        EditMessageText.builder()
                            .chatId(update.channelPost.chatId)
                            .messageId(update.channelPost.messageId)
                            .text("$COMMAND_INIT_CHARACTER$command $key")
                            .build()
                    } else {
                        EditMessageText.builder()
                            .chatId(update.channelPost.chatId)
                            .messageId(update.channelPost.messageId)
                            .text("Channel has been already bound")
                            .build()
                    }
                botSender.sendMessage(editMessage)
            }

            update.hasEditedMessage() || update.hasMessage() -> {
                val message = update.message ?: update.editedMessage ?: return
                val messageKey = message.text.split(" ").last()
                val channelId = message.senderChat?.id ?: return
                channelStorageService.findInvitation(channelId)?.also { (messageId, key) ->
                    if (key == messageKey && message.forwardFromMessageId == messageId) {
                        channelStorageService.bind(channelId, message.chatId)

                        log.info("Has bound new channel with name ${message.senderChat.title}")

                        botSender.sendMessage(
                            EditMessageText.builder()
                                .chatId(channelId)
                                .messageId(messageId)
                                .text("Bind successfully completed")
                                .build()
                        )
                        channelService.updateAvailableUsersByChannelId(channelId)
                    }
                }
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}
