package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.extensions.PARSE_MODE
import com.lezenford.telegram.chanelthreadbot.extensions.toLink
import com.lezenford.telegram.chanelthreadbot.service.db.UserService
import com.lezenford.telegram.chanelthreadbot.telegram.command.Command
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle

@Service
class InlineService(
    private val userService: UserService,
    private val commands: List<Command>
) {

    suspend fun receiveQuery(inlineQuery: InlineQuery): AnswerInlineQuery? {
        return when {
            inlineQuery.query.startsWith("@") && inlineQuery.chatType == "supergroup" -> {
                userService.findById(inlineQuery.from.id)?.let {
                    val prefix = inlineQuery.query.drop(1).lowercase()
                    userService.findAll()
                        .filterNot { it.id == inlineQuery.from.id }
                        .filter { user ->
                            user.fullName.split(" ").plus(user.username)
                                .any { it?.lowercase()?.startsWith(prefix) == true }
                        }
                        .take(6).map { user ->
                            InlineQueryResultArticle.builder()
                                .id(user.id.toString())
                                .inputMessageContent(
                                    InputTextMessageContent.builder()
                                        .messageText("Invite ${user.toLink()}")
                                        .parseMode(PARSE_MODE)
                                        .build()
                                ).title(user.fullName + (user.username?.let { " @$it" } ?: ""))
                                .build()

                        }.let {
                            AnswerInlineQuery.builder()
                                .cacheTime(0)
                                .inlineQueryId(inlineQuery.id)
                                .results(it.toList())
                                .build()
                        }
                }
            }

            inlineQuery.query.startsWith(Command.COMMAND_INIT_CHARACTER) -> null //TODO

            else -> null
        }
    }
}