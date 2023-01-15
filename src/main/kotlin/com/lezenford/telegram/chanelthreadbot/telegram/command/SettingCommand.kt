package com.lezenford.telegram.chanelthreadbot.telegram.command

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.telegram.BotSender
import com.lezenford.telegram.chanelthreadbot.telegram.menu.SettingsMenu
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class SettingCommand(
    private val settingsMenu: SettingsMenu,
    private val botSender: BotSender
) : Command() {
    override val command: String = "settings"
    override val description: String = "User settings"
    override val publish: Boolean = true

    override suspend fun action(update: Update) {
        if (update.message.from.id == update.message.chatId) {
            botSender.sendMessage(settingsMenu.initMessage(update.message))
        } else {
            log.warn("Command $command supports only private chat")
        }
    }

    companion object {
        private val log by Logger()
    }
}
