package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.model.entity.Topic
import com.lezenford.telegram.chanelthreadbot.model.repository.TopicRepository
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class TopicService(
    private val cacheManager: CacheManager,
    private val topicRepository: TopicRepository,
    private val channelService: ChannelService,
    override val entityManagerFactory: EntityManagerFactory
) : TransactionService() {

    @Cacheable(
        value = [CacheConfiguration.TOPIC_CACHE],
        key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Integer).toString(#groupThreadId)",
        unless = "#result == null"
    )
    suspend fun findTopicByGroupThreadId(channelId: Long, groupThreadId: Int): Topic? {
        //TODO проверить unless
        return call {
            topicRepository.findByChannelIdAndGroupThreadId(channelId, groupThreadId)
        }
    }

    suspend fun save(topic: Topic): Topic? {
        val cache = cacheManager.getCache(CacheConfiguration.TOPIC_CACHE)
        val key = "${topic.channel.id} ${topic.groupThreadId}"
        return callTransactional { topicRepository.save(topic) }?.also {
            cache?.put(key, it)
        } ?: run { cache?.evict(key); null }
    }

    // @Cacheable(
    //     value = [CacheConfiguration.TOPIC_CACHE],
    //     key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Integer).toString(#groupThreadId)",
    //     unless = "#result == null"
    // )
    // suspend fun addTopicToChannel(channelId: Long, channelThreadId: Int, groupThreadId: Int): Topic? {
    //     //TODO проверить unless
    //     return channelService.findById(channelId)?.let { channel ->
    //         Topic(
    //             channel = channel,
    //             channelThreadId = channelThreadId,
    //             groupThreadId = groupThreadId
    //         ).let {
    //             callTransactional { topicRepository.save(it) }
    //         }
    //     }
    // }

    @CacheEvict(
        value = [CacheConfiguration.TOPIC_CACHE],
        key = "T(java.lang.Long).toString(#channelId) + ' ' + T(java.lang.Long).toString(#groupThreadId)"
    )
    suspend fun addUserToTopic(channelId: Long, groupThreadId: Int, userId: Long) {
        callTransactional {
            topicRepository.findByChannelIdAndGroupThreadId(
                channelId,
                groupThreadId
            )?.users?.add(userId)
        }
    }
}