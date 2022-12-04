package com.lezenford.telegram.chanelthreadbot.model.repository

import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelGroupBind
import org.springframework.data.jpa.repository.JpaRepository

interface ChannelGroupBindRepository : JpaRepository<ChannelGroupBind, Long> {
    fun findByChannelId(channelId: Long): ChannelGroupBind?
    fun findByGroupId(groupId: Long): ChannelGroupBind?
    fun deleteByChannelId(channelId: Long)
}