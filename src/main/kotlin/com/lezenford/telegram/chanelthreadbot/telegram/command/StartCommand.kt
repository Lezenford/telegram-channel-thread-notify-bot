package com.lezenford.telegram.chanelthreadbot.telegram.command

import com.lezenford.telegram.chanelthreadbot.extensions.username
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import com.lezenford.telegram.chanelthreadbot.service.db.UserService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class StartCommand(
    private val userService: UserService,
    private val botSender: BotSender
) : Command() {
    override val command: String = "start"
    override val description: String = "Register in bot"

    override suspend fun action(update: Update) {
        update.message?.also { message ->
            if (message.chat?.isUserChat == true) {
                userService.findById(message.from.id)?.also {
                    botSender.sendMessage(
                        SendMessage.builder()
                            .chatId(message.chatId)
                            .text("You are already registered")
                            .build()
                    )
                } ?: kotlin.run {
                    userService.save(
                        User(
                            id = message.from.id,
                            username = message.from.username()
                        )
                    )
                    botSender.sendMessage(
                        SendMessage.builder()
                            .chatId(message.chatId)
                            .text("You successfully registered")
                            .build()
                    )
                }
            }
        }
    }
}