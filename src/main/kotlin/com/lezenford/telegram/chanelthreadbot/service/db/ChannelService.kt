package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.extensions.init
import com.lezenford.telegram.chanelthreadbot.extensions.username
import com.lezenford.telegram.chanelthreadbot.model.entity.Channel
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember
import javax.persistence.EntityManagerFactory

@Service
class ChannelService(
    private val userService: UserService,
    private val channelRepository: ChannelRepository,
    override val entityManagerFactory: EntityManagerFactory
) : TransactionService() {

    final suspend inline fun findByGroupId(groupId: Long): Channel? {
        return findChannelIdByGroupId(groupId)?.let { findById(it) }
    }

    @Cacheable(
        value = [CacheConfiguration.CHANNEL_GROUP_CACHE],
        key = "T(java.lang.Long).toString(#groupId)",
        unless = "#result == null"
    )
    suspend fun findChannelIdByGroupId(groupId: Long): Long? {
        //TODO написать тест на работу unless
        return call { channelRepository.findByGroupId(groupId)?.id }
    }

    @Cacheable(
        value = [CacheConfiguration.CHANNEL_CACHE],
        key = "T(java.lang.Long).toString(#id)",
        unless = "#result == null"
    )
    suspend fun findById(id: Long): Channel? {
        //TODO написать тест на работу unless
        return call { channelRepository.findByIdOrNull(id) }
    }

    suspend fun findAllChannels(): Flow<Channel> {
        return callTransactional { channelRepository.findAll().onEach { it.users.init() } }?.asFlow() ?: emptyFlow()
    }

    @Cacheable(value = [CacheConfiguration.CHANNEL_USERS_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun findAllChannelUsers(channelId: Long): Flow<User> {
        return callTransactional {
            channelRepository.findByIdOrNull(channelId)
                ?.users?.filter { it.active } ?: emptyList()
        }?.asFlow() ?: emptyFlow()
    }

    final suspend inline fun findByIdAndUsername(channelId: Long, username: String): User? {
        return findAllChannelUsers(channelId).firstOrNull { it.username == username }
    }

    @CacheEvict(value = [CacheConfiguration.CHANNEL_USERS_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun updateUsers(channelId: Long, channelAdmins: List<ChatMember>) {
        val chatUsers = channelAdmins.filterNot { it.user.isBot }.mapNotNull { chatMember ->
            userService.findById(chatMember.user.id)?.also {
                if (it.username != chatMember.user.username()) {
                    it.username = chatMember.user.username()
                    userService.save(it)
                }
            }
        }.associateBy { it.id }.toMutableMap()

        callTransactional {
            channelRepository.findByIdOrNull(channelId)?.also { channel ->
                channel.users.removeAll { chatUsers.remove(it.id) == null }

                chatUsers.values.onEach { chatMember ->
                    log.info("Add user ${chatMember.id} to channel $channelId")
                }.also { channel.users.addAll(it) }
            }
        }
    }

    @CacheEvict(value = [CacheConfiguration.CHANNEL_USERS_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun addUserToChannel(channelId: Long, userId: Long) {
        userService.findById(userId)?.also { user ->
            callTransactional {
                //TODO написать тест, что после закрытия транзакции данные точно фиксируются
                channelRepository.findByIdOrNull(channelId)?.users?.add(user)
            }
        }
    }

    @CacheEvict(value = [CacheConfiguration.CHANNEL_USERS_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun removeUserFromChannel(channelId: Long, userId: Long) {
        callTransactional {
            //TODO написать тест, что после закрытия транзакции данные точно фиксируются
            channelRepository.findByIdOrNull(channelId)?.users?.removeIf { it.id == userId }
        }
    }

    suspend fun bind(channelId: Long, groupId: Long) {
        callTransactional { channelRepository.save(Channel(id = channelId, groupId = groupId)) }
    }

    @CacheEvict(value = [CacheConfiguration.CHANNEL_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun unbind(channelId: Long) {
        callTransactional { channelRepository.deleteById(channelId) }
    }

    @CachePut(
        value = [CacheConfiguration.CHANNEL_BIND_INVITATIONS_CACHE],
        key = "T(java.lang.Long).toString(#channelId)"
    )
    suspend fun registerInvitation(channelId: Long, messageId: Int, key: String): Pair<Int, String> = messageId to key

    @Cacheable(
        value = [CacheConfiguration.CHANNEL_BIND_INVITATIONS_CACHE],
        key = "T(java.lang.Long).toString(#channelId)"
    )
    @CacheEvict(
        value = [CacheConfiguration.CHANNEL_BIND_INVITATIONS_CACHE],
        key = "T(java.lang.Long).toString(#channelId)"
    )
    suspend fun findInvitation(channelId: Long): Pair<Int, String>? = null
}