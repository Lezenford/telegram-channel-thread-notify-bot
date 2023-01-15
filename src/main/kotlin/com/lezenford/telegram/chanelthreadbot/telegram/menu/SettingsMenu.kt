package com.lezenford.telegram.chanelthreadbot.telegram.menu

import com.lezenford.telegram.chanelthreadbot.extensions.PARSE_MODE
import com.lezenford.telegram.chanelthreadbot.service.db.UserStorageService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class SettingsMenu(
    private val userStorageService: UserStorageService
) : Menu() {
    override val id: String = "settings"

    override suspend fun initMessage(message: Message): SendMessage {
        val user = userStorageService.findById(message.from.id)
            ?: throw IllegalArgumentException("Unknown user ${message.from.id}")
        return SendMessage.builder()
            .chatId(message.chatId)
            .parseMode(PARSE_MODE)
            .text(
                """
                User settings:
                Receive own message as notify: ${user.replyOwnMessage}
                """.trimIndent()
            )
            .replyMarkup(
                InlineKeyboardMarkup.builder()
                    .keyboardRow(
                        listOf(
                            InlineKeyboardButton.builder()
                                .text("${if (user.replyOwnMessage) "Disable" else " Enable"} receive own messages")
                                .callbackData("$id:$REPLAY_OWN_MESSAGES_PROPERTY:${user.replyOwnMessage.not()}")
                                .build()
                        )
                    ).build()
            ).build()
    }

    override suspend fun receiveRequest(query: CallbackQuery): BotApiMethod<*>? {
        val data = query.data.split(":").drop(1)
        return when (data.first()) {
            REPLAY_OWN_MESSAGES_PROPERTY -> {
                val user = userStorageService.findById(query.from.id)
                    ?: throw IllegalArgumentException("Unknown user ${query.from.id}")
                val status = data.getOrNull(1)?.toBoolean()
                    ?: throw IllegalArgumentException("Incorrect query data ${query.data}")
                user.replyOwnMessage = status
                userStorageService.save(user)

                EditMessageText.builder()
                    .chatId(query.message.chatId)
                    .messageId(query.message.messageId)
                    .parseMode(PARSE_MODE)
                    .text(
                        """
                        User settings:
                        Receive own message as notify: ${user.replyOwnMessage}
                        """.trimIndent()
                    )
                    .replyMarkup(
                        InlineKeyboardMarkup.builder()
                            .keyboardRow(
                                listOf(
                                    InlineKeyboardButton.builder()
                                        .text("${if (user.replyOwnMessage) "Disable" else " Enable"} receive own messages")
                                        .callbackData("$id:$REPLAY_OWN_MESSAGES_PROPERTY:${user.replyOwnMessage.not()}")
                                        .build()
                                )
                            ).build()
                    ).build()
            }

            else -> null
        }
    }

    companion object {
        private const val REPLAY_OWN_MESSAGES_PROPERTY = "reply"
    }
}
