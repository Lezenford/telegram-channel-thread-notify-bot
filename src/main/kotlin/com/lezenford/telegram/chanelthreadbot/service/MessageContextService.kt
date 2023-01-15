package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.model.entity.Channel
import com.lezenford.telegram.chanelthreadbot.model.entity.Topic
import com.lezenford.telegram.chanelthreadbot.service.db.ChannelStorageService
import com.lezenford.telegram.chanelthreadbot.service.db.TopicService
import org.telegram.telegrambots.meta.api.objects.Message

abstract class MessageContextService {
    protected abstract val channelStorageService: ChannelStorageService
    protected abstract val topicService: TopicService

    protected suspend fun Message.context(): Context? {
        return channelStorageService.findByGroupId(chatId)?.let { channel ->

            val topic = messageThreadId?.let {
                topicService.findTopicByGroupThreadId(channelId = channel.id, groupThreadId = messageThreadId)
            } ?: topicService.save(
                Topic(
                    channel = channel,
                    channelThreadId = forwardFromMessageId,
                    groupThreadId = messageId
                )
            )

            topic?.let { Context(channel = channel, topic = topic) }
        }
    }

    protected data class Context(
        val channel: Channel,
        val topic: Topic
    )

    companion object {
        private val log by Logger()
    }
}
