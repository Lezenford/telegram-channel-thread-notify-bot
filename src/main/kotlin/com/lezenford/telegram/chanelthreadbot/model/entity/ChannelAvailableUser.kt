package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "channel_available_users")
data class ChannelAvailableUser(
    @Column(name = "channel_id")
    val channelId: Long,

    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "username")
    var username: String? = null
) : BaseEntity()