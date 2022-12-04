package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "channel_topic_users")
data class ChannelTopicUserBind(
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "topic_id")
    val topicId: Int,

    @Column(name = "channel_id")
    val channelId: Long
) : BaseEntity()