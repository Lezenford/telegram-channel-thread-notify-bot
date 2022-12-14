package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.extensions.PARSE_MODE
import com.lezenford.telegram.chanelthreadbot.extensions.escape
import com.lezenford.telegram.chanelthreadbot.extensions.toLink
import com.lezenford.telegram.chanelthreadbot.model.entity.Channel
import com.lezenford.telegram.chanelthreadbot.model.entity.History
import com.lezenford.telegram.chanelthreadbot.model.entity.Topic
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelService
import com.lezenford.telegram.chanelthreadbot.service.db.HistoryService
import com.lezenford.telegram.chanelthreadbot.service.db.TopicService
import com.lezenford.telegram.chanelthreadbot.service.db.UserService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.CopyMessage
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.EntityType
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.MessageId
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

@Service
class UpdateService(
    private val channelService: ChannelService,
    private val topicService: TopicService,
    private val userService: UserService,
    private val historyService: HistoryService,
    private val commandService: CommandService,
    private val inlineService: InlineService,
    private val botSender: BotSender,
    properties: TelegramProperties,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default
    private val userMutexes: ConcurrentHashMap<Long, Mutex> = ConcurrentHashMap()
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

                update.message?.viaBot?.id == botId -> {
                }

                // Receive new message
                update.hasMessage() && update.message.hasProtectedContent != true -> when {
                    update.message.replyToMessage == null && update.message.forwardFromChat != null && update.message.forwardSignature != null ->
                        receiveNewThreadMessage(message = update.message)

                    update.message.replyToMessage != null && update.message.messageThreadId != null ->
                        receiveThreadMessage(update.message)
                }

                //Receive update message
                update.hasEditedMessage() && update.editedMessage.hasProtectedContent != true ->
                    receiveEditeMessage(update.editedMessage)
            }
        }.onFailure {
            log.error("Error while processing update :$update", it)
        }

        return null
    }

    private suspend fun receiveNewThreadMessage(message: Message) {
        message.context()?.also { (channel, topic) ->
            val user = channelService.findByIdAndUsername(channel.id, message.forwardSignature)
                ?: updateAvailableUsersByChannelId(channel).let {
                    channelService.findByIdAndUsername(channel.id, message.forwardSignature)
                }
            user?.also {
                topic.users.add(it.id)
                topicService.save(topic)
            }
        } ?: kotlin.run {
            log.info("We receive message from unregistered group")
            log.debug(message)
        }
    }

    private suspend fun receiveThreadMessage(message: Message) {
        message.context()?.also { (channel, topic) ->
            channelService.findAllChannelUsers(channel.id).filter { it.id == message.from.id }
                .onEmpty { updateAvailableUsersByChannelId(channel) }.collect()

            if (topic.users.add(message.from.id)) {
                topicService.save(topic)
            }

            val mentionUsers = if (message.viaBot?.id == botId) {
                inviteMembersToTopic(message)
                message.entities.filter { it.type == EntityType.TEXTMENTION && it.user.isBot.not() }.map { it.user.id }
            } else emptyList()

            channelService.findAllChannelUsers(channel.id).filter {
                if (mentionUsers.isNotEmpty()) {
                    mentionUsers.contains(it.id)
                } else {
                    topic.users.contains(it.id)
                }
            }.collect {
                sendNotification(it, message)
            }

        } ?: kotlin.run {
            log.info("We receive message from unregistered group")
            log.debug(message)
        }
    }

    private suspend fun receiveEditeMessage(message: Message) {
        message.context()?.also { (channel, topic) ->
            channelService.findAllChannelUsers(channel.id).filter { topic.users.contains(it.id) }.collect { user ->
                CoroutineScope(coroutineContext).launch {
                    userMutexes.getOrPut(user.id) { Mutex() }.withLock {
                        historyService.findByUserIdAndTopicId(user.id, topic.id)?.also { history ->
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

    private suspend fun sendNotification(user: User, message: Message) {
        CoroutineScope(coroutineContext).launch {
            userMutexes.getOrPut(user.id) { Mutex() }.withLock {
                message.context()?.apply {
                    val history = historyService.findByUserIdAndTopicId(user.id, topic.id)
                        ?: historyService.save(History(user = user, topic = topic))
                    if (history == null) {
                        log.info("Can't create user history for userId ${user.id} and message $message")
                        return@launch
                    }

                    //Change active topic
                    if (user.lastTopic?.id != history.topic.id) {
                        startTopicNotification(message, user, history)
                    } else {
                        //Topic owner message
                        if (message.from.id == user.id) {
                            userAnswerNotification(message, user, history)
                        } else {
                            continueTopicNotification(message, user, history)
                        }
                    }

                    historyService.save(history)
                }
            }
        }
    }

    private suspend fun Context.startTopicNotification(message: Message, user: User, history: History) {
        //Send topic head message as forward
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

            //copy last notification message
            val copyMessageId = history.notificationMessageId?.let { copyMessage(user.id, it) }
            history.notificationMessageId = copyMessageId
        } else {
            resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId > notificationMessageId }

            //copy last notification message
            val copyMessageId = history.ownerNotificationMessageId?.let { copyMessage(user.id, it) }
            history.ownerNotificationMessageId = copyMessageId
        }

        history.listExistMessages().forEach {
            botSender.sendMessage(
                DeleteMessage.builder().chatId(user.id).messageId(it).build()
            )
        }
        history.removeExistMessages()

        history.topicNotificationMessageId = topicMessageId

        //Send notification
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
        userService.save(user)
    }

    private suspend fun Context.userAnswerNotification(message: Message, user: User, history: History) {
        resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId < notificationMessageId }

        //Send notification
        sendNotificationMessage(message, user, history)?.also { (notificationMessageId, originalMessageId) ->
            history.ownerNotificationMessageId?.also {
                botSender.sendMessage(
                    DeleteMessage.builder().chatId(user.id).messageId(it).build()
                )
            }

            history.ownerNotificationMessageId = notificationMessageId
            history.ownerOriginalMessageId = originalMessageId
        }
    }

    private suspend fun Context.continueTopicNotification(message: Message, user: User, history: History) {
        resetCounter(history) { ownerNotificationMessageId, notificationMessageId -> ownerNotificationMessageId > notificationMessageId }

        //Send notification
        sendNotificationMessage(message, user, history)?.also { (notificationMessageId, originalMessageId) ->
            history.notificationMessageId?.also {
                botSender.sendMessage(
                    DeleteMessage.builder().chatId(user.id).messageId(it).build()
                )
            }

            history.notificationMessageId = notificationMessageId
            history.originalMessageId = originalMessageId
        }
    }

    private suspend fun resetCounter(
        history: History,
        predicate: (ownerNotificationMessageId: Int, notificationMessageId: Int) -> Boolean
    ) {
        val needResetCounter = history.ownerNotificationMessageId?.let { ownerNotificationMessageId ->
            history.notificationMessageId?.let { notificationMessageId ->
                predicate(ownerNotificationMessageId, notificationMessageId)
            }
        } ?: false

        if (needResetCounter) {
            history.unreadMessagesCount = 0
        } else {
            history.unreadMessagesCount++
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
            } ?: run {
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
            //Remove unread count message if counter reset
            history.unreadCountMessageId?.also {
                botSender.sendMessage(DeleteMessage.builder().chatId(user.id).messageId(it).build())
                history.unreadCountMessageId = null
            }
        }
    }

    private suspend fun sendNotificationMessage(
        message: Message,
        user: User,
        history: History
    ): Pair<Int, Int>? {
        //send counter message if need
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

            //Remove button if it exists
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

    private suspend fun copyMessage(chatId: Long, messageId: Int): Int? {
        return botSender.sendMessage(
            CopyMessage.builder()
                .chatId(chatId)
                .fromChatId(chatId)
                .messageId(messageId)
                .build()
        )?.messageId?.toInt()
    }

    @Scheduled(
        fixedRateString = "\${telegram.user_update.rate:${TelegramProperties.UserUpdate.DEFAULT_RATE}}",
        initialDelayString = "\${telegram.user_update.delay:${TelegramProperties.UserUpdate.DEFAULT_DELAY}}"
    )
    fun updateAvailableUsers() {
        runBlocking(coroutineContext) {
            channelService.findAllChannels().onEach {
                updateAvailableUsersByChannelId(it)
            }.collect()
        }
    }

    private suspend fun updateAvailableUsersByChannelId(channel: Channel) {
        log.info("Initiate update user list for channel ${channel.id}")
        botSender.sendMessage(GetChatAdministrators(channel.id.toString()))?.also {
            channelService.updateUsers(channel.id, it)
        }
    }

    private suspend fun inviteMembersToTopic(message: Message) {
        message.context()?.also { (channel, topic) ->
            val allUsers = userService.findAll().toList()
            val channelUsers = TreeSet(Comparator.comparing(User::id)).also {
                it.addAll(channelService.findAllChannelUsers(channel.id).toList())
            }
            message.entities.filter { it.type == EntityType.TEXTMENTION && it.user.isBot.not() }
                .mapNotNull { entity ->
                    allUsers.find { it.id == entity.user.id }
                }.filterNot { topic.users.contains(it.id) }.partition { channelUsers.contains(it) }
                .also { (channelUsers, outsideUsers) ->
                    channelUsers.forEach {
                        topicService.addUserToTopic(channel.id, topic.groupThreadId, it.id)
                    }

                    //TODO send invitations
                    // outsideUsers.forEach {
                    //
                    // }
                }
        }
    }

    private suspend fun Message.context(): Context? {
        return channelService.findByGroupId(chatId)?.let { channel ->

            val topic = messageThreadId?.let {
                topicService.findTopicByGroupThreadId(channelId = channel.id, groupThreadId = messageThreadId)
            } ?: topicService.save(
                Topic(
                    channel = channel,
                    channelThreadId = forwardFromMessageId,
                    groupThreadId = messageId
                )
            )

            topic?.let { Context(channel = channel, topic = topic) }
        }
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

    private data class Context(
        val channel: Channel,
        val topic: Topic
    )

    companion object {
        private val log by Logger()
        private const val BUTTON_TEXT = "Thread"
    }
}