package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class UpdateService(
    private val commandService: CommandService,
    private val inlineService: InlineService,
    private val channelService: ChannelService,
    private val menuService: MenuService,
    private val notifyService: NotifyService,
    properties: TelegramProperties,
) {
    private val botId = properties.token.split(":").first().toLong()

    suspend fun receiveUpdate(update: Update): BotApiMethod<*>? {
        kotlin.runCatching {
            when {
                // Inline query answer
                update.hasInlineQuery() -> return inlineService.receiveQuery(update.inlineQuery)

                // Execute commands
                update.channelPost?.isCommand == true || update.message?.isCommand == true ||
                    update.editedMessage?.isCommand == true || update.editedChannelPost?.isCommand == true ->
                    commandService.executeCommand(update)

                // Callback query
                update.hasCallbackQuery() -> return menuService.executeCallbackQuery(update)

                // Receive new message
                update.hasMessage() && update.message.hasProtectedContent != true -> when {
                    // Invite from bot
                    update.message.viaBot?.id == botId -> {
                        channelService.inviteMembersToTopic(update.message)
                    }

                    // Receive new thread
                    update.message.replyToMessage == null && update.message.forwardFromChat != null && update.message.forwardSignature != null ->
                        channelService.receiveNewThreadMessage(message = update.message)

                    // Receive new thread message
                    update.message.replyToMessage != null && update.message.messageThreadId != null ->
                        channelService.receiveThreadMessage(update.message)
                }

                // Receive update message
                update.hasEditedMessage() && update.editedMessage.hasProtectedContent != true ->
                    notifyService.receiveEditeMessage(update.editedMessage)
            }
        }.onFailure {
            log.error("Error while processing update :$update", it)
        }

        return null
    }

    companion object {
        private val log by Logger()
    }
}
