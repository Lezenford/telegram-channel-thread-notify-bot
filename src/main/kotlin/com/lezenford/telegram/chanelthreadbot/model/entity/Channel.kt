package com.lezenford.telegram.chanelthreadbot.model.entity

import java.util.TreeSet
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table

@Entity
@Table(name = "channel")
data class Channel(
    @Id
    @Column(name = "id")
    val id: Long,

    @Column(name = "group_id")
    val groupId: Long
) {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "channel_user",
        joinColumns = [JoinColumn(name = "channel_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    val users: MutableSet<User> = TreeSet(Comparator.comparingLong(User::id))
}