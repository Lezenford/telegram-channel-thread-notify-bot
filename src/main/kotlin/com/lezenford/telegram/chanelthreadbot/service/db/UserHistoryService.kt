package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.model.entity.UserHistory
import com.lezenford.telegram.chanelthreadbot.model.repository.UserHistoryRepository
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class UserHistoryService(
    override val entityManagerFactory: EntityManagerFactory,
    private val userHistoryRepository: UserHistoryRepository
) : TransactionService() {

    @Cacheable(
        value = [CacheConfiguration.USER_HISTORY_CACHE],
        key = "T(java.lang.Long).toString(#userId)"
    )
    suspend fun findUserHistory(userId: Long): UserHistory? {
        return call { userHistoryRepository.findByUserId(userId) }
    }

    // For fix cache error fot generated key
    final suspend inline fun updateUserHistory(userHistory: UserHistory): UserHistory {
        return updateUserHistory(userHistory, userHistory.userId)
    }

    @CachePut(
        value = [CacheConfiguration.USER_HISTORY_CACHE],
        key = "T(java.lang.Long).toString(#userId)"
    )
    suspend fun updateUserHistory(userHistory: UserHistory, userId: Long): UserHistory {
        callTransactional { userHistoryRepository.save(userHistory) }
        return userHistory
    }
}