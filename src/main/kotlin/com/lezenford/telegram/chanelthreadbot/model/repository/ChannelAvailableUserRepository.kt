package com.lezenford.telegram.chanelthreadbot.model.repository

import com.lezenford.telegram.chanelthreadbot.model.entity.ChannelAvailableUser
import org.springframework.data.jpa.repository.JpaRepository

interface ChannelAvailableUserRepository : JpaRepository<ChannelAvailableUser, Long> {
    fun findAllByChannelId(channelId: Long): List<ChannelAvailableUser>
    fun deleteAllByChannelIdAndUserId(channelId: Long, userId: Long)
    fun findByChannelIdAndUsername(channelId: Long, username: String): ChannelAvailableUser?
}