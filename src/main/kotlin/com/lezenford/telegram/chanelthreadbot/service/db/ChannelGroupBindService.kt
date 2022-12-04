package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelGroupBind
import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelGroupBindRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class ChannelGroupBindService(
    override val entityManagerFactory: EntityManagerFactory,
    private val channelGroupBindRepository: ChannelGroupBindRepository
) : TransactionService() {

    suspend fun findAllChannels(): Flow<Long> {
        return call { channelGroupBindRepository.findAll() }.asFlow().map { it.id }
    }

    @Cacheable(value = [CacheConfiguration.CHANNEL_GROUP_CACHE], key = "T(java.lang.Long).toString(#channelId)")
    suspend fun findGroupIdByChannelId(channelId: Long): Long? {
        return call { channelGroupBindRepository.findByChannelId(channelId)?.groupId }
    }

    @Cacheable(value = [CacheConfiguration.AVAILABLE_GROUP_CACHE], key = "T(java.lang.Long).toString(#groupId)")
    suspend fun findChannelIdByGroupId(groupId: Long): Long? {
        return call { channelGroupBindRepository.findByGroupId(groupId) }?.channelId
    }

    @CacheEvict(
        value = [CacheConfiguration.CHANNEL_GROUP_CACHE, CacheConfiguration.AVAILABLE_GROUP_CACHE],
        allEntries = true
    )
    suspend fun bindChannelAndGroup(channelId: Long, groupId: Long) {
        callTransactional { channelGroupBindRepository.save(ChannelGroupBind(channelId, groupId)) }
    }

    @CacheEvict(
        value = [CacheConfiguration.CHANNEL_GROUP_CACHE, CacheConfiguration.AVAILABLE_GROUP_CACHE],
        allEntries = true
    )
    suspend fun deleteBind(channelId: Long) {
        callTransactional { channelGroupBindRepository.deleteByChannelId(channelId) }
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