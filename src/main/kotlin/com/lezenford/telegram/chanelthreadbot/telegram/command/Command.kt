package com.lezenford.telegram.chanelthreadbot.telegram.command

import org.telegram.telegrambots.meta.api.objects.Update

abstract class Command {
    abstract val command: String
    abstract val description: String
    open val publish: Boolean = false
    abstract suspend fun action(update: Update)

    companion object {
        const val COMMAND_INIT_CHARACTER = "/"
    }
}