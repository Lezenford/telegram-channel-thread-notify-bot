package com.lezenford.telegram.chanelthreadbot

import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelRepository
import com.lezenford.telegram.chanelthreadbot.model.repository.HistoryRepository
import com.lezenford.telegram.chanelthreadbot.model.repository.TopicRepository
import com.lezenford.telegram.chanelthreadbot.model.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseTest {
    @SpyBean
    protected lateinit var userRepository: UserRepository

    @SpyBean
    protected lateinit var channelRepository: ChannelRepository

    @SpyBean
    protected lateinit var topicRepository: TopicRepository

    @SpyBean
    protected lateinit var historyRepository: HistoryRepository

    @AfterEach
    fun cleanDb() {
        historyRepository.deleteAll()
        topicRepository.deleteAll()
        channelRepository.deleteAll()
        userRepository.deleteAll()
    }
}