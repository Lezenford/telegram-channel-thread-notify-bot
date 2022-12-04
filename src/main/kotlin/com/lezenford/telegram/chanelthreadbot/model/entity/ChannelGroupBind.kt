package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "channel_group_bind")
data class ChannelGroupBind(
    @Column(name = "channel_id")
    val channelId: Long,

    @Column(name = "group_id")
    val groupId: Long
) : BaseEntity()