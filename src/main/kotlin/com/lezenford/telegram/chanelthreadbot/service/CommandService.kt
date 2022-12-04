package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.telegram.command.Command
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class CommandService(commands: List<Command>) {
    private val commands: Map<String, Command> = commands.associateBy { it.command }

    suspend fun executeCommand(update: Update) {
        val message = update.message ?: update.channelPost ?: update.editedMessage ?: update.editedChannelPost
        val command = message.text.takeWhile { it != ' ' }.drop(1).lowercase()
        commands[command]?.action(update)
    }
}