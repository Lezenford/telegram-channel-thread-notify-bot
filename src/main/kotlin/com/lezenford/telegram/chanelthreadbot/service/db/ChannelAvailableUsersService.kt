package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelAvailableUser
import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelAvailableUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class ChannelAvailableUsersService(
    override val entityManagerFactory: EntityManagerFactory,
    private val channelAvailableUserRepository: ChannelAvailableUserRepository,
    private val cacheManager: CacheManager
) : TransactionService() {
    @Cacheable(value = [CacheConfiguration.AVAILABLE_USERS_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun listChannelAvailableUsers(channelId: Long): Flow<ChannelAvailableUser> {

        return call { channelAvailableUserRepository.findAllByChannelId(channelId) }.asFlow()
    }

    suspend fun findUserByUsername(channelId: Long, username: String): Long? {
        return call {
            channelAvailableUserRepository.findByChannelIdAndUsername(
                channelId,
                username
            )?.userId
        }
    }

    @CacheEvict(value = [CacheConfiguration.AVAILABLE_USERS_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun addUserToChannel(channelId: Long, userId: Long, username: String) {
        callTransactional {
            kotlin.runCatching {
                channelAvailableUserRepository.save(ChannelAvailableUser(channelId, userId, username))
            }.onFailure {
                log.error("Error add userId: $userId to group $channelId", it)
            }
        }
    }

    @CacheEvict(value = [CacheConfiguration.AVAILABLE_USERS_CACHE], allEntries = true)
    suspend fun removeUserFromChannel(channelId: Long, userId: Long) {
        callTransactional {
            channelAvailableUserRepository.deleteAllByChannelIdAndUserId(channelId, userId)
        }
    }

    suspend fun update(channelAvailableUser: ChannelAvailableUser) {
        if (channelAvailableUser.id != 0L) {
            callTransactional { channelAvailableUserRepository.save(channelAvailableUser) }
        }
    }

    companion object {
        private val log by Logger()
    }
}