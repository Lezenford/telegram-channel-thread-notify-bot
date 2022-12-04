package com.lezenford.telegram.chanelthreadbot.model.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "user_history")
data class UserHistory(
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "channel_id")
    var channelId: Long = 0,

    @Column(name = "topic_id")
    var topicId: Int = 0,

    @Column(name = "first_notification_message_id")
    var firstNotificationMessageId: Int? = null,

    @Column(name = "first_original_message_id")
    var firstOriginalMessageId: Int? = null,

    @Column(name = "second_notification_message_id")
    var secondNotificationMessageId: Int? = null,

    @Column(name = "second_original_message_id")
    var secondOriginalMessageId: Int? = null,

    @Column(name = "third_notification_message_id")
    var thirdNotificationMessageId: Int? = null,

    @Column(name = "third_original_message_id")
    var thirdOriginalMessageId: Int? = null,
) : BaseEntity() {

    fun addMessage(notificationMessageId: Int, originalMessageId: Int) {
        when {
            firstNotificationMessageId == null -> {
                firstNotificationMessageId = notificationMessageId
                firstOriginalMessageId = originalMessageId
            }

            secondNotificationMessageId == null -> {
                secondNotificationMessageId = notificationMessageId
                secondOriginalMessageId = originalMessageId
            }

            thirdNotificationMessageId == null -> {
                thirdNotificationMessageId = notificationMessageId
                thirdOriginalMessageId = originalMessageId
            }

            else -> {
                firstNotificationMessageId = secondNotificationMessageId
                secondNotificationMessageId = thirdNotificationMessageId
                thirdNotificationMessageId = notificationMessageId

                firstOriginalMessageId = secondOriginalMessageId
                secondOriginalMessageId = thirdOriginalMessageId
                thirdOriginalMessageId = originalMessageId
            }
        }
    }

    fun switchTopic(channelId: Long, topicId: Int, notificationMessageId: Int, originalMessageId: Int) {
        this.channelId = channelId
        this.topicId = topicId
        firstNotificationMessageId = notificationMessageId
        secondNotificationMessageId = null
        thirdNotificationMessageId = null
        firstOriginalMessageId = originalMessageId
        secondOriginalMessageId = null
        thirdOriginalMessageId = null
    }
}