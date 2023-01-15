package com.lezenford.telegram.chanelthreadbot.service

import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import com.lezenford.telegram.chanelthreadbot.telegram.command.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand

@Service
class CommandService(
    commands: List<Command>,
    private val botSender: BotSender
) : CommandLineRunner {
    private val commands: Map<String, Command> = commands.associateBy { it.command }

    suspend fun executeCommand(update: Update) {
        val message = update.message ?: update.channelPost ?: update.editedMessage ?: update.editedChannelPost
        val command = message.text.takeWhile { it != ' ' }.drop(1).lowercase()
        commands[command]?.action(update)
    }

    override fun run(vararg args: String?) {
        CoroutineScope(Dispatchers.Default).launch {
            commands.values.filter { it.publish }.map {
                BotCommand(it.command, it.description)
            }.takeIf { it.isNotEmpty() }?.also {
                botSender.sendMessage(SetMyCommands.builder().commands(it).build())
            }
        }
    }
}
