package com.lezenford.telegram.chanelthreadbot.model.repository

import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelTopic
import org.springframework.data.jpa.repository.JpaRepository

interface ChannelTopicRepository : JpaRepository<ChannelTopic, Long> {
    fun findByChannelIdAndGroupThreadId(channelId: Long, groupThreadId: Int): ChannelTopic?
}