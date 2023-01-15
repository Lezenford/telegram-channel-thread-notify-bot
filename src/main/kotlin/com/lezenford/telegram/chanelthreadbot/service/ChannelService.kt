package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.extensions.USER_LOGIN_LINK_PREFIX
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelStorageService
import com.lezenford.telegram.chanelthreadbot.service.db.TopicService
import com.lezenford.telegram.chanelthreadbot.service.db.UserStorageService
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.objects.EntityType
import org.telegram.telegrambots.meta.api.objects.Message
import java.util.TreeSet
import kotlin.coroutines.CoroutineContext

@Service
class ChannelService(
    override val channelStorageService: ChannelStorageService,
    override val topicService: TopicService,
    private val userStorageService: UserStorageService,
    private val notifyService: NotifyService,
    private val botSender: BotSender
) : MessageContextService(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    @Scheduled(
        fixedRateString = "\${telegram.user_update.rate:${TelegramProperties.UserUpdate.DEFAULT_RATE}}",
        initialDelayString = "\${telegram.user_update.delay:${TelegramProperties.UserUpdate.DEFAULT_DELAY}}"
    )
    fun updateAvailableUsers() {
        runBlocking(coroutineContext) {
            channelStorageService.findAllChannels().onEach {
                updateAvailableUsersByChannelId(it.id)
            }.collect()
        }
    }

    suspend fun updateAvailableUsersByChannelId(channelId: Long) {
        log.info("Initiate update user list for channel $channelId")
        botSender.sendMessage(GetChatAdministrators(channelId.toString()))?.also {
            channelStorageService.updateUsers(channelId, it)
        }
    }

    suspend fun receiveNewThreadMessage(message: Message) {
        message.context()?.also { (channel, topic) ->
            val user = channelStorageService.findByIdAndUsername(channel.id, message.forwardSignature)
                ?: updateAvailableUsersByChannelId(channel.id).let {
                    channelStorageService.findByIdAndUsername(channel.id, message.forwardSignature)
                }
            user?.also {
                topic.users.add(it.id)
                topicService.save(topic)
            }
        } ?: kotlin.run {
            log.warn("We receive message from unregistered group")
        }
    }

    suspend fun receiveThreadMessage(message: Message) {
        message.context()?.also { (channel, topic) ->
            channelStorageService.findAllChannelUsers(channel.id).filter { it.id == message.from.id }
                .onEmpty { updateAvailableUsersByChannelId(channel.id) }.collect()

            if (topic.users.add(message.from.id)) {
                topicService.save(topic)
            }

            channelStorageService.findAllChannelUsers(channel.id).filter {
                topic.users.contains(it.id)
            }.collect { notifyService.sendNotification(it, message) }
        } ?: kotlin.run {
            log.warn("We receive message from unregistered group")
        }
    }

    suspend fun inviteMembersToTopic(message: Message) {
        message.context()?.also { (channel, topic) ->
            val allUsers = userStorageService.findAll().toList()
            val mentionedUsers = message.entities
                .filter {
                    it.type == EntityType.TEXTMENTION && it.user.isBot.not() || it.type == EntityType.TEXTLINK && it.url.startsWith(
                        USER_LOGIN_LINK_PREFIX
                    )
                }
                .mapNotNull { entity ->
                    when (entity.type) {
                        EntityType.TEXTMENTION -> allUsers.find { it.id == entity.user.id }
                        EntityType.TEXTLINK -> allUsers.find {
                            it.username == entity.url.replace(
                                USER_LOGIN_LINK_PREFIX,
                                ""
                            )
                        }

                        else -> null
                    }
                }
            val channelUsers = TreeSet(Comparator.comparing(User::id)).also { set ->
                var users = channelStorageService.findAllChannelUsers(channel.id).toList()
                if (mentionedUsers.all { users.contains(it) }.not()) {
                    updateAvailableUsersByChannelId(channel.id)
                    users = channelStorageService.findAllChannelUsers(channel.id).toList()
                }
                set.addAll(users)
            }
            mentionedUsers.filterNot { topic.users.contains(it.id) }.partition { channelUsers.contains(it) }
                .also { (channelUsers, outsideUsers) ->
                    channelUsers.forEach {
                        topicService.addUserToTopic(channel.id, topic.groupThreadId, it.id)
                        notifyService.sendNotification(it, message)
                    }

                    // TODO send invitations
                    // outsideUsers.forEach {
                    //
                    // }
                }
        }
    }

    companion object {
        private val log by Logger()
    }
}
