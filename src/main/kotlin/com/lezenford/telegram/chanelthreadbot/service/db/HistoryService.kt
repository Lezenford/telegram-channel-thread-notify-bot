package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.model.entity.History
import com.lezenford.telegram.chanelthreadbot.model.repository.HistoryRepository
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class HistoryService(
    private val cacheManager: CacheManager,
    private val historyRepository: HistoryRepository,
    override val entityManagerFactory: EntityManagerFactory
) : TransactionService() {
    @Cacheable(
        value = [CacheConfiguration.USER_HISTORY_CACHE],
        key = "T(java.lang.Long).toString(#userId) + ' ' + T(java.lang.Long).toString(#topicId)"
    )
    suspend fun findByUserIdAndTopicId(userId: Long, topicId: Long): History? {
        return call { historyRepository.findAllByUserIdAndTopicId(userId, topicId) }
    }

    suspend fun save(history: History): History? {
        val cache = cacheManager.getCache(CacheConfiguration.USER_HISTORY_CACHE)
        val key = "${history.user.id} ${history.topic.id}"
        cache?.also {
            it.put(key, history)
        }
        return callTransactional { historyRepository.save(history) }
            ?: kotlin.run { cache?.evict(key); null }
    }
}