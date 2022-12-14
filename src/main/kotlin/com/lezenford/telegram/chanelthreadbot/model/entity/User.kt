package com.lezenford.telegram.chanelthreadbot.model.entity

import java.util.TreeSet
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "telegram_user")
data class User(
    @Id
    @Column(name = "id")
    val id: Long,

    @Column(name = "username")
    var username: String,

    @Column(name = "active")
    var active: Boolean = true
) {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "channel_user",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "channel_id")]
    )
    val channels: MutableSet<Channel> = TreeSet(Comparator.comparingLong(Channel::id))

    @OneToOne
    @JoinColumn(name = "last_topic")
    var lastTopic: Topic? = null
}