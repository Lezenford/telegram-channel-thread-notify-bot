package com.lezenford.telegram.chanelthreadbot.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import com.lezenford.telegram.chanelthreadbot.cache.CoroutineCacheInterceptor
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.interceptor.CacheInterceptor
import org.springframework.cache.interceptor.CacheOperationSource
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    fun ticker(): Ticker = Ticker.systemTicker()

    @Bean
    @Primary
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    fun coroutineCacheInterceptor(
        cacheOperationSource: CacheOperationSource
    ): CacheInterceptor = CoroutineCacheInterceptor().apply {
        setCacheOperationSources(cacheOperationSource)
    }

    @Bean
    fun cacheManager(ticker: Ticker): CacheManager =
        SimpleCacheManager().also {
            it.setCaches(
                listOf(
                    CaffeineCache(
                        CHANNEL_USERS_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        TELEGRAM_USERS_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        CHANNEL_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        CHANNEL_GROUP_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        TOPIC_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    // CaffeineCache(
                    //     AVAILABLE_USERS_CACHE,
                    //     Caffeine.newBuilder()
                    //         .expireAfterAccess(1, TimeUnit.DAYS)
                    //         .ticker(ticker)
                    //         .build()
                    // ),
                    // CaffeineCache(
                    //     TOPIC_USERS_CACHE,
                    //     Caffeine.newBuilder()
                    //         .expireAfterAccess(1, TimeUnit.DAYS)
                    //         .ticker(ticker)
                    //         .build()
                    // ),
                    // CaffeineCache(
                    //     CHANNEL_GROUP_CACHE,
                    //     Caffeine.newBuilder()
                    //         .expireAfterAccess(1, TimeUnit.DAYS)
                    //         .ticker(ticker)
                    //         .build()
                    // ),
                    // CaffeineCache(
                    //     AVAILABLE_GROUP_CACHE,
                    //     Caffeine.newBuilder()
                    //         .expireAfterAccess(1, TimeUnit.DAYS)
                    //         .ticker(ticker)
                    //         .build()
                    // ),
                    CaffeineCache(
                        USER_HISTORY_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterAccess(1, TimeUnit.DAYS)
                            .ticker(ticker)
                            .build()
                    ),
                    CaffeineCache(
                        CHANNEL_BIND_INVITATIONS_CACHE,
                        Caffeine.newBuilder()
                            .expireAfterWrite(10, TimeUnit.SECONDS)
                            .ticker(ticker)
                            .build()
                    ),
                    // CaffeineCache(
                    //     CHANNEL_TOPIC_CACHE,
                    //     Caffeine.newBuilder()
                    //         .expireAfterAccess(1, TimeUnit.DAYS)
                    //         .ticker(ticker)
                    //         .build()
                    // ),
                )
            )
        }

    companion object {
        const val CHANNEL_CACHE = "ChannelCache"
        const val CHANNEL_USERS_CACHE = "ChannelUsersCache"
        const val CHANNEL_GROUP_CACHE = "ChannelGroupCache"
        const val CHANNEL_BIND_INVITATIONS_CACHE = "ChannelBindInvitationsCache"
        const val TOPIC_CACHE = "TopicCache"
        const val TELEGRAM_USERS_CACHE = "TelegramUsersCache"
        const val USER_HISTORY_CACHE = "UserHistoryCache"
        // const val AVAILABLE_USERS_CACHE = "AvailableUsersCache"
        // const val TOPIC_USERS_CACHE = "TopicUsersCache"
        // const val CHANNEL_GROUP_CACHE = "ChannelGroupCache"
        // const val AVAILABLE_GROUP_CACHE = "AvailableGroupCache"

        // const val CHANNEL_TOPIC_CACHE = "ChannelTopicCache"
    }
}