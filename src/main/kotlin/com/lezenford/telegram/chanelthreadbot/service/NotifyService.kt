package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.extensions.PARSE_MODE
import com.lezenford.telegram.chanelthreadbot.extensions.escape
import com.lezenford.telegram.chanelthreadbot.extensions.toLink
import com.lezenford.telegram.chanelthreadbot.model.entity.History
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelStorageService
import com.lezenford.telegram.chanelthreadbot.service.db.HistoryStorageService
import com.lezenford.telegram.chanelthreadbot.service.db.TopicService
import com.lezenford.telegram.chanelthreadbot.service.db.UserStorageService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.CopyMessage
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.MessageId
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

@Service
class NotifyService(
    override val channelStorageService: ChannelStorageService,
    override val topicService: TopicService,
    private val userStorageService: UserStorageService,
    private val historyStorageService: HistoryStorageService,
    private val botSender: BotSender,
) : MessageContextService(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default
    private val userMutexes: ConcurrentHashMap<Long, Mutex> = ConcurrentHashMap()

    suspend fun receiveEditeMessage(message: Message) {
        message.context()?.also { (channel, topic) ->
            channelStorageService.findAllChannelUsers(channel.id).filter { topic.users.contains(it.id) }
                .collect { user ->
                    CoroutineScope(coroutineContext).launch {
                        userMutexes.getOrPut(user.id) { Mutex() }.withLock {
                            historyStorageService.findByUserIdAndTopicId(user.id, topic.id)?.also { history ->
                                listOf(
                                    history.ownerNotificationMessageId to history.ownerOriginalMessageId,
                                    history.notificationMessageId to history.originalMessageId
                                ).find { it.second == message.messageId }?.first?.also { messageId ->
                                    when {
                                        message.hasText() -> EditMessageText.builder()
                                            .chatId(user.id)
                                            .messageId(messageId)
                                            .text(message.replayText(message.text))
                                            .parseMode(PARSE_MODE)
                                            .also {
                                                if (messageId == history.notificationButtonMessageId) {
                                                    it.replyMarkup(threadUrlKeyboard(message, history))
                                                }
                                            }
                                            .build()

                                        message.caption != null -> EditMessageCaption.builder().chatId(user.id)
                                            .messageId(messageId)
                                            .caption(message.replayText(message.caption).chunked(1024).first())
                                            .also {
                                                if (messageId == history.notificationButtonMessageId) {
                                                    it.replyMarkup(threadUrlKeyboard(message, history))
                                                }
                                            }
                                            .build()

                                        else -> null
                                    }?.also {
                                        botSender.sendMessage(it)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    suspend fun sendNotification(user: User, message: Message) {
        CoroutineScope(coroutineContext).launch {
            userMutexes.getOrPut(user.id) { Mutex() }.withLock {
                message.context()?.apply {
                    val history = historyStorageService.findByUserIdAndTopicId(user.id, topic.id)
                        ?: historyStorageService.save(History(user = user, topic = topic))
                    if (history == null) {
                        log.info("Can't create user history for userId ${user.id} and message $message")
                        return@launch
                    }

                    // Change active topic
                    if (user.lastTopic?.id != history.topic.id) {
                        startTopicNotification(message, user, history)
                    } else {
                        // Topic owner message
                        if (message.from.id == user.id) {
                            userAnswerNotification(message, user, history)
                        } else {
                            continueTopicNotification(message, user, history)
                        }
                    }

                    historyStorageService.save(history)
                }
            }
        }
    }

    private suspend fun Context.startTopicNotification(message: Message, user: User, history: History) {
        // Skip start thread if own messages disable
        if (user.id == message.from.id && user.replyOwnMessage.not()) {
            return
        }

        // Send topic head message as forward
        val topicMessageId = botSender.sendMessage(
            ForwardMessage.builder()
                .chatId(user.id)
                .fromChatId(channel.groupId)
                .messageId(topic.groupThreadId)
                .disableNotification(true)
                .build()
        )?.messageId

        if (user.id == message.from.id) {
            resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId < notificationMessageId }

            // copy last notification message
            val copyMessageId = history.notificationMessageId?.let { copyMessage(user.id, it) }
            history.notificationMessageId = copyMessageId
        } else {
            resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId > notificationMessageId }

            // copy last notification message
            val copyMessageId = history.ownerNotificationMessageId?.let { copyMessage(user.id, it) }
            history.ownerNotificationMessageId = copyMessageId
        }

        history.listExistMessages().forEach {
            botSender.sendMessage(DeleteMessage.builder().chatId(user.id).messageId(it).build())
        }
        history.cleanExistMessages()

        history.topicNotificationMessageId = topicMessageId

        // Send notification
        sendNotificationMessage(message, user, history)?.also { (notificationMessageId, originalMessageId) ->
            if (user.id == message.from.id) {
                history.ownerNotificationMessageId = notificationMessageId
                history.ownerOriginalMessageId = originalMessageId
            } else {
                history.notificationMessageId = notificationMessageId
                history.originalMessageId = originalMessageId
            }
        }

        user.lastTopic = history.topic
        userStorageService.save(user)
    }

    private suspend fun userAnswerNotification(message: Message, user: User, history: History) {
        resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId < notificationMessageId }
        if (user.replyOwnMessage) {
            // Send notification
            sendNotificationMessage(message, user, history)?.also { (notificationMessageId, originalMessageId) ->
                history.ownerNotificationMessageId?.also {
                    botSender.sendMessage(DeleteMessage.builder().chatId(user.id).messageId(it).build())
                }

                history.ownerNotificationMessageId = notificationMessageId
                history.ownerOriginalMessageId = originalMessageId
            }
        } else {
            // Drop unread count after own answer
            history.unreadMessagesCount = -1
            sendThreadCountMessage(user, history)
        }
    }

    private suspend fun continueTopicNotification(message: Message, user: User, history: History) {
        resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId > notificationMessageId - 1 }

        // Send notification
        sendNotificationMessage(message, user, history)?.also { (notificationMessageId, originalMessageId) ->
            history.notificationMessageId?.also {
                botSender.sendMessage(DeleteMessage.builder().chatId(user.id).messageId(it).build())
            }

            history.notificationMessageId = notificationMessageId
            history.originalMessageId = originalMessageId
        }
    }

    private suspend fun resetCounter(
        history: History,
        predicate: (ownerNotificationMessageId: Int, notificationMessageId: Int) -> Boolean
    ) {
        val ownerNotificationMessageId = history.ownerNotificationMessageId
        val notificationMessageId = history.notificationMessageId
        when {
            // New history every reset count
            ownerNotificationMessageId == null && notificationMessageId == null -> history.unreadMessagesCount = 0

            // If history has only users messages
            ownerNotificationMessageId != null && notificationMessageId == null -> history.unreadMessagesCount = 0

            // If history has user's and other's messages
            ownerNotificationMessageId != null && notificationMessageId != null ->
                if (predicate(ownerNotificationMessageId, notificationMessageId)) {
                    history.unreadMessagesCount = 0
                } else {
                    history.unreadMessagesCount++
                }

            // If history has only other's messages
            else -> history.unreadMessagesCount++
        }
    }

    private suspend fun sendNotificationMessage(
        message: Message,
        user: User,
        history: History
    ): Pair<Int, Int>? {
        // send counter message if need
        sendThreadCountMessage(user, history)

        val notificationMessage = when {
            message.hasText() -> SendMessage.builder()
                .parseMode(PARSE_MODE)
                .text(message.replayText(message.text))
                .chatId(user.id)
                .replyMarkup(threadUrlKeyboard(message, history))
                .disableNotification(user.id == message.from.id)
                .entities(message.entities)
                .build()

            else -> CopyMessage.builder()
                .chatId(user.id)
                .fromChatId(message.chatId)
                .messageId(message.messageId)
                .parseMode(PARSE_MODE)
                .caption(message.replayText(message.caption).chunked(1024).first())
                .replyMarkup(threadUrlKeyboard(message, history))
                .disableNotification(user.id == message.from.id)
                .build()
        }
        return botSender.sendMessage(notificationMessage)?.let {
            when (it) {
                is Message -> it.messageId to message.messageId

                is MessageId -> it.messageId.toInt() to message.messageId

                else -> null
            }
        }?.also {
            // Remove button if it exists
            history.notificationButtonMessageId?.also { messageId ->
                botSender.sendMessage(
                    EditMessageReplyMarkup.builder()
                        .chatId(user.id)
                        .messageId(messageId)
                        .build()
                )
            }
            history.notificationButtonMessageId = it.first
        }
    }

    private suspend fun sendThreadCountMessage(user: User, history: History) {
        if (history.unreadMessagesCount > 0) {
            history.unreadCountMessageId?.also {
                botSender.sendMessage(
                    EditMessageText.builder()
                        .chatId(user.id)
                        .messageId(it)
                        .text("_Thread messages: ${history.unreadMessagesCount}_")
                        .parseMode(PARSE_MODE)
                        .build()
                )
            } ?: kotlin.run {
                history.unreadCountMessageId = botSender.sendMessage(
                    SendMessage.builder()
                        .chatId(user.id)
                        .text("_Thread messages: ${history.unreadMessagesCount}_")
                        .parseMode(PARSE_MODE)
                        .disableNotification(true)
                        .build()
                )?.messageId
            }
        } else {
            // Remove unread count message if counter reset
            history.unreadCountMessageId?.also {
                botSender.sendMessage(DeleteMessage.builder().chatId(user.id).messageId(it).build())
                history.unreadCountMessageId = null
            }
        }
    }

    private suspend fun copyMessage(chatId: Long, messageId: Int): Int? {
        return botSender.sendMessage(
            CopyMessage.builder()
                .chatId(chatId)
                .fromChatId(chatId)
                .messageId(messageId)
                .build()
        )?.messageId?.toInt()
    }

    private fun Message.replayText(text: String?): String {
        return """
            |__${from.toLink()}__
            |${text?.escape() ?: ""}
        """.trimMargin()
    }

    private fun threadUrlKeyboard(message: Message, history: History): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(
                listOf(
                    InlineKeyboardButton.builder()
                        .text(BUTTON_TEXT)
                        .url(historyLink(message, history))
                        .build()
                )
            ).build()
    }

    private fun historyLink(message: Message, history: History): String {
        val linkChatId = message.chatId.toString().replace("-100", "")
        return "https://t.me/c/$linkChatId/${message.messageId}?thread=${history.topic.groupThreadId}"
    }

    companion object {
        private val log by Logger()
        private const val BUTTON_TEXT = "Thread"
    }
}
