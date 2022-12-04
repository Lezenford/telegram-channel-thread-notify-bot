package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.model.entity.UserHistory
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelAvailableUsersService
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelGroupBindService
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelTopicService
import com.lezenford.telegram.chanelthreadbot.service.db.TopicUsersBindService
import com.lezenford.telegram.chanelthreadbot.service.db.UserHistoryService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.CopyMessage
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.MessageId
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import kotlin.coroutines.CoroutineContext

@Service
class UpdateService(
    private val channelGroupBindService: ChannelGroupBindService,
    private val channelAvailableUsersService: ChannelAvailableUsersService,
    private val topicUsersBindService: TopicUsersBindService,
    private val userHistoryService: UserHistoryService,
    private val channelTopicService: ChannelTopicService,
    private val commandService: CommandService,
    private val botSender: BotSender,
    private val caceManager: CacheManager
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    suspend fun receiveUpdate(update: Update) {
        kotlin.runCatching {
            when {
                // Execute commands
                update.channelPost?.isCommand == true || update.message?.isCommand == true ||
                    update.editedMessage?.isCommand == true || update.editedChannelPost?.isCommand == true ->
                    commandService.executeCommand(update)

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
    }

    private suspend fun receiveNewThreadMessage(message: Message) {
        if (channelGroupBindService.findGroupIdByChannelId(message.forwardFromChat.id) == message.chatId) {
            message.context()?.also { (channelId, topicId) ->
                val userId = channelAvailableUsersService.findUserByUsername(
                    channelId = channelId, username = message.forwardSignature
                ) ?: updateAvailableUsersByChannelId(channelId).let {
                    channelAvailableUsersService.findUserByUsername(
                        channelId = channelId, username = message.forwardSignature
                    )
                }

                userId?.also {
                    topicUsersBindService.addUserToTopic(
                        channelId = channelId, topicId = topicId, userId = userId
                    )
                }
            }
        } else {
            log.info("We receive message from unregistered group")
            log.debug(message)
        }
    }

    private suspend fun receiveThreadMessage(message: Message) {
        message.context()?.also { (channelId, topicId) ->
            channelAvailableUsersService.listChannelAvailableUsers(channelId)
                .filterNot { it.userId == message.from.id }.onEmpty { updateAvailableUsersByChannelId(channelId) }
                .collect {}

            topicUsersBindService.findUsersByChannelAndTopic(channelId, topicId).onEach {
                sendNotification(it, message)
            }.filter {
                it == message.from.id
            }.onEmpty {
                if (message.from.isBot.not()) {
                    topicUsersBindService.addUserToTopic(
                        channelId = channelId, topicId = topicId, userId = message.from.id
                    )
                    sendNotification(message.from.id, message)
                }
            }.collect {}
        } ?: kotlin.run {
            log.info("We receive message from unregistered group")
            log.debug(message)
        }
    }

    private suspend fun receiveEditeMessage(message: Message) {
        message.context()?.also { (channelId, topicId) ->
            topicUsersBindService.findUsersByChannelAndTopic(channelId, topicId).collect { userId ->
                userHistoryService.findUserHistory(userId)?.also { userHistory ->
                    listOf(
                        userHistory.firstNotificationMessageId to userHistory.firstOriginalMessageId,
                        userHistory.secondNotificationMessageId to userHistory.secondOriginalMessageId,
                        userHistory.thirdNotificationMessageId to userHistory.thirdOriginalMessageId
                    ).find { it.second == message.messageId }?.also { (messageId, _) ->
                        when {
                            message.hasText() -> EditMessageText.builder()
                                .chatId(userId)
                                .messageId(messageId)
                                .text(message.replayText())
                                .build()

                            message.caption != null -> EditMessageCaption.builder().chatId(userId)
                                .messageId(messageId)
                                .caption(message.caption)
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

    private suspend fun sendNotification(userId: Long, message: Message) {
        coroutineScope {
            launch {
                message.context()?.also { (channelId, topicId) ->
                    val messageForSend = when {
                        message.hasText() -> SendMessage.builder()
                            .parseMode("MarkdownV2")
                            .text(message.replayText())
                            .chatId(userId)
                            .build()

                        else -> CopyMessage.builder()
                            .chatId(userId)
                            .fromChatId(message.chatId)
                            .messageId(message.messageId)
                            .build()
                    }

                    val userHistory = userHistoryService.findUserHistory(userId)
                        ?: userHistoryService.updateUserHistory(UserHistory(userId = userId))

                    if (userHistory.channelId == channelId && userHistory.topicId == topicId) {
                        userHistory.thirdNotificationMessageId?.let { userHistory.firstNotificationMessageId }?.also {
                            botSender.sendMessage(
                                DeleteMessage.builder().chatId(userId).messageId(it).build()
                            )
                        }
                        botSender.sendMessage(messageForSend)?.also {
                            when (it) {
                                is Message -> userHistory.addMessage(it.messageId.toInt(), message.messageId)
                                is MessageId -> userHistory.addMessage(it.messageId.toInt(), message.messageId)
                            }
                        }
                    } else {
                        botSender.sendMessage(ForwardMessage().apply {
                            chatId = userId.toString()
                            fromChatId = channelId.toString()
                            messageId = topicId
                            disableNotification = true
                        })

                        botSender.sendMessage(messageForSend)?.also {
                            when (it) {
                                is Message -> userHistory.switchTopic(
                                    channelId = channelId,
                                    topicId = topicId,
                                    notificationMessageId = it.messageId.toInt(),
                                    originalMessageId = message.messageId
                                )

                                is MessageId -> userHistory.switchTopic(
                                    channelId = channelId,
                                    topicId = topicId,
                                    notificationMessageId = it.messageId.toInt(),
                                    originalMessageId = message.messageId
                                )
                            }
                        }
                    }

                    userHistoryService.updateUserHistory(userHistory)
                }

            }
        }
    }

    @Scheduled(
        fixedRateString = "\${telegram.user_update.rate:${TelegramProperties.UserUpdate.DEFAULT_RATE}}",
        initialDelayString = "\${telegram.user_update.delay:${TelegramProperties.UserUpdate.DEFAULT_DELAY}}"
    )
    fun updateAvailableUsers() {
        runBlocking(coroutineContext) {
            channelGroupBindService.findAllChannels().onEach {
                updateAvailableUsersByChannelId(it)
            }
        }
    }

    private suspend fun updateAvailableUsersByChannelId(channelId: Long) {
        log.info("Initiate update user list for channel $channelId")

        botSender.sendMessage(GetChatAdministrators(channelId.toString()))?.filterNot { it.user.isBot }
            ?.associateBy { it.user.id }?.toMutableMap()?.also { chatMembers ->
                channelAvailableUsersService.listChannelAvailableUsers(channelId)
                    .onEach { chatAvailableUser ->
                        chatMembers[chatAvailableUser.userId]?.also {
                            if (chatAvailableUser.username != it.user.username()) {
                                chatAvailableUser.username = it.user.username()
                                channelAvailableUsersService.update(chatAvailableUser)
                            }
                        }
                    }
                    .filter { chatMembers.remove(it.userId) == null }.collect {
                        channelAvailableUsersService.removeUserFromChannel(channelId, it.userId)

                        log.info("Remove user ${it.userId} from channel $channelId")
                    }
                chatMembers.values.map { chatMember ->
                    channelAvailableUsersService.addUserToChannel(
                        channelId = channelId,
                        userId = chatMember.user.id,
                        username = chatMember.user.username()
                    )

                    log.info("Add user ${chatMember.user.id} to channel $channelId")
                }
            }
    }

    private suspend fun Message.context(): Context? {
        return channelGroupBindService.findChannelIdByGroupId(chatId)?.let { channelId ->
            if (messageThreadId == null) {
                channelTopicService.findChanelThreadId(channelId = channelId, groupThreadId = messageId)
                    ?: channelTopicService.addTopicToChannel(
                        channelId = channelId,
                        channelThreadId = forwardFromMessageId,
                        groupThreadId = messageId
                    )
            }

            channelTopicService.findChanelThreadId(channelId, messageThreadId ?: messageId)?.let { topicId ->
                Context(channelId = channelId, topicId = topicId)
            }
        }
    }

    private fun Message.replayText(): String {
        return """
            __from ${from.username()}__
            ${text?.escape() ?: ""}
            """.trimIndent()
    }

    private fun String.escape(): String = this.map { if (it.code in 1..125) "\\$it" else it }.joinToString("")

    private fun User.username() = listOf(firstName, lastName).mapNotNull { it }
        .joinToString(" ")

    private data class Context(
        val channelId: Long,
        val topicId: Int,
    )

    companion object {
        private val log by Logger()
    }
}