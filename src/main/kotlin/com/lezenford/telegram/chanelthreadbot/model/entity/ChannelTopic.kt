package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "channel_topics")
data class ChannelTopic(
    @Column(name = "channel_id")
    val channelId: Long,

    @Column(name = "group_thread_id")
    val groupThreadId: Int,

    @Column(name = "channel_tread_id")
    val channelThreadId: Int
) : BaseEntity()