package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "telegram_user")
data class User(
    @Id
    @Column(name = "id")
    val id: Long,

    @Column(name = "full_name")
    var fullName: String,

    @Column(name = "username")
    var username: String? = null,

    @Column(name = "active")
    var active: Boolean = true,

    @Column(name = "replay_own_message")
    var replyOwnMessage: Boolean = true
) {
    @OneToOne
    @JoinColumn(name = "last_topic")
    var lastTopic: Topic? = null
}
