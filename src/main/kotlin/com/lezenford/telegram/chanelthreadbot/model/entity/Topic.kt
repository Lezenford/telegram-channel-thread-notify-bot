package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "topic")
data class Topic(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    val channel: Channel,

    @Column(name = "channel_thread_id")
    val channelThreadId: Int,

    @Column(name = "group_thread_id")
    val groupThreadId: Int
) {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "topic_user", joinColumns = [JoinColumn(name = "topic_id")])
    @Column(name = "user_id")
    val users: MutableSet<Long> = mutableSetOf()
}