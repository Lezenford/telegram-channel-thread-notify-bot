package com.lezenford.telegram.chanelthreadbot.service.db

import com.lezenford.telegram.chanelthreadbot.configuration.CacheConfiguration
import com.lezenford.telegram.chanelthreadbot.model.entity.User
import com.lezenford.telegram.chanelthreadbot.model.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.persistence.EntityManagerFactory

@Service
class UserService(
    private val cacheManager: CacheManager,
    private val userRepository: UserRepository,
    override val entityManagerFactory: EntityManagerFactory
) : TransactionService() {
    @Cacheable(
        value = [CacheConfiguration.TELEGRAM_USERS_CACHE],
        key = "T(java.lang.Long).toString(#id)",
        unless = "#result == null"
    )
    suspend fun findById(id: Long): User? {
        return call { userRepository.findByIdOrNull(id) }
    }

    @Cacheable(
        value = [CacheConfiguration.TELEGRAM_USERS_CACHE],
        key = "'$ALL_USER_CACHE_KEY'",
    )
    suspend fun findAll(): Flow<User> {
        return call { userRepository.findAll() }.asFlow()
    }

    suspend fun save(user: User): User? {
        val userCache = cacheManager.getCache(CacheConfiguration.TELEGRAM_USERS_CACHE)
        return callTransactional { userRepository.save(user) }?.also { savedUser ->
            userCache?.put(savedUser.id.toString(), savedUser)
            userCache?.evict(ALL_USER_CACHE_KEY)
        }
    }

    companion object {
        private const val ALL_USER_CACHE_KEY = "all"
    }
}