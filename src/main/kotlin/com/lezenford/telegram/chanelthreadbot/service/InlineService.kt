package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.extensions.PARSE_MODE
import com.lezenford.telegram.chanelthreadbot.extensions.toLink
import com.lezenford.telegram.chanelthreadbot.service.db.UserService
import com.lezenford.telegram.chanelthreadbot.telegram.command.Command
import kotlinx.coroutines.flow.filter
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
                val prefix = inlineQuery.query.drop(1).lowercase()
                userService.findAll()
                    .filter { user -> user.username.split(" ").any { it.lowercase().startsWith(prefix) } }
                    .take(6).map {
                        InlineQueryResultArticle.builder()
                            .id(it.id.toString())
                            .inputMessageContent(
                                InputTextMessageContent.builder()
                                    .messageText(it.toLink())
                                    .parseMode(PARSE_MODE)
                                    .build()
                            ).title(it.username)
                            .build()

                    }.let {
                        AnswerInlineQuery.builder()
                            .inlineQueryId(inlineQuery.id)
                            .results(it.toList())
                            .build()
                    }
            }

            else -> null
        }
    }
}