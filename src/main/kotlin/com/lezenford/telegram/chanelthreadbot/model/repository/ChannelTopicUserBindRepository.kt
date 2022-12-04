package com.lezenford.telegram.chanelthreadbot.model.repository

import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelTopicUserBind
import org.springframework.data.jpa.repository.JpaRepository

interface ChannelTopicUserBindRepository : JpaRepository<ChannelTopicUserBind, Long> {
    fun findAllByChannelIdAndTopicId(channelId: Long, topicId: Int): List<ChannelTopicUserBind>
}