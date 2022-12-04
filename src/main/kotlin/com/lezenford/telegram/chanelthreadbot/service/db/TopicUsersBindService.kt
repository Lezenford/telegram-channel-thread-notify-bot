package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelTopicUserBind
import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelTopicUserBindRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class TopicUsersBindService(
    override val entityManagerFactory: EntityManagerFactory,
    private val channelTopicUserBindRepository: ChannelTopicUserBindRepository,
    private val channelAvailableUsersService: ChannelAvailableUsersService
) : TransactionService() {

    @Cacheable(
        value = [CacheConfiguration.TOPIC_USERS_CACHE],
        key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Long).toString(#topicId)"
    )
    suspend fun findUsersByChannelAndTopic(channelId: Long, topicId: Int): Flow<Long> {
        val availableChannelUsers = channelAvailableUsersService.listChannelAvailableUsers(channelId)
            .map { it.userId }.toSet()
        return call { channelTopicUserBindRepository.findAllByChannelIdAndTopicId(channelId, topicId) }.asFlow()
            .map { it.userId }.filter { availableChannelUsers.contains(it) }
    }

    @CacheEvict(
        value = [CacheConfiguration.TOPIC_USERS_CACHE],
        key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Long).toString(#topicId)"
    )
    suspend fun addUserToTopic(channelId: Long, topicId: Int, userId: Long) {
        if (channelAvailableUsersService.listChannelAvailableUsers(channelId).count { it.userId == userId } > 0) {
            kotlin.runCatching {
                callTransactional {
                    channelTopicUserBindRepository.save(
                        ChannelTopicUserBind(
                            userId = userId,
                            topicId = topicId,
                            channelId = channelId
                        )
                    )
                    log.info("User $userId successfully bind to topic $topicId in channel $channelId")
                }
            }.onFailure {
                log.error("Error add user $userId with topic $topicId in group $channelId", it)
            }
        } else {
            log.error("Can't bind user $userId with topic $topicId in group $channelId: Permission denied")
        }
    }

    companion object {
        private val log by Logger()
    }
}