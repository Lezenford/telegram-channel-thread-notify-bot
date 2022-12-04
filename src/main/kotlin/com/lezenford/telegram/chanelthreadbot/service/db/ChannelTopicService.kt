package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelTopic
import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelTopicRepository
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class ChannelTopicService(
    override val entityManagerFactory: EntityManagerFactory,
    private val channelTopicRepository: ChannelTopicRepository
) : TransactionService() {

    @Cacheable(
        value = [CacheConfiguration.CHANNEL_TOPIC_CACHE],
        key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Integer).toString(#groupThreadId)"
    )
    suspend fun findChanelThreadId(channelId: Long, groupThreadId: Int): Int? {
        return call {
            channelTopicRepository.findByChannelIdAndGroupThreadId(channelId, groupThreadId)
        }?.channelThreadId
    }

    @CachePut(
        value = [CacheConfiguration.CHANNEL_TOPIC_CACHE],
        key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Integer).toString(#groupThreadId)"
    )
    suspend fun addTopicToChannel(channelId: Long, channelThreadId: Int, groupThreadId: Int): Int {
        val channelTopic = ChannelTopic(
            channelId = channelId,
            groupThreadId = groupThreadId,
            channelThreadId = channelThreadId
        )
        callTransactional { channelTopicRepository.save(channelTopic) }
        return channelThreadId
    }
}