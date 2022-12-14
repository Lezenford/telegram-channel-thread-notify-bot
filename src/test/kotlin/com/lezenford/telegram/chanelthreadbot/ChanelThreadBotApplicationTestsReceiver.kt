package com.lezenford.telegram.chanelthreadbot

import com.lezenford.telegram.chanelthreadbot.model.repository.ChannelRepository
import com.lezenford.telegram.chanelthreadbot.model.repository.TopicRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ChanelThreadBotApplicationTestsReceiver {

    @Autowired
    private lateinit var topicRepository: TopicRepository

    @Autowired
    private lateinit var channelRepository: ChannelRepository

    @Test
    fun contextLoads() {
    }
}
