package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "user_history")
data class History(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    val topic: Topic,

    @Column(name = "unread_messages_count")
    var unreadMessagesCount: Int = 0,

    @Column(name = "unread_count_message_id")
    var unreadCountMessageId: Int? = null,

    @Column(name = "topic_notification_message_id")
    var topicNotificationMessageId: Int? = null,

    @Column(name = "owner_notification_message_id")
    var ownerNotificationMessageId: Int? = null,

    @Column(name = "owner_original_message_id")
    var ownerOriginalMessageId: Int? = null,

    @Column(name = "notification_message_id")
    var notificationMessageId: Int? = null,

    @Column(name = "original_message_id")
    var originalMessageId: Int? = null,

    @Column(name = "notification_message_with_button_id")
    var notificationButtonMessageId: Int? = null
) {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    fun listExistMessages(): List<Int> = listOf(
        topicNotificationMessageId,
        unreadCountMessageId,
        ownerNotificationMessageId,
        notificationMessageId
    ).mapNotNull { it }

    fun cleanExistMessages() {
        topicNotificationMessageId = null
        unreadCountMessageId = null
        ownerNotificationMessageId = null
        notificationMessageId = null
    }
}
