package com.lezenford.telegram.chanelthreadbot.telegram

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.service.UpdateService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates
import kotlin.math.max

@Component
@ConditionalOnProperty(value = ["telegram.type"], havingValue = "LONG_POLLING")
class LongPollingBotReceiver(
    override val updateService: UpdateService,
    private val sender: BotSender
) : BotReceiver(), CommandLineRunner {

    @Volatile
    private var offset: Int = 0

    override fun run(vararg args: String?) {
        launch {
            sender.sendMessage(DeleteWebhook())
        }
    }

    @Scheduled(
        fixedRateString = "\${telegram.long-polling.rate:${TelegramProperties.LongPolling.DEFAULT_RATE}}",
        initialDelayString = "\${telegram.long-polling.delay:${TelegramProperties.LongPolling.DEFAULT_DELAY}}"
    )
    private fun receiveUpdates() {
        runBlocking(coroutineContext) {
            sender.sendMessage(GetUpdates.builder().also {
                if (offset > 0) {
                    it.offset(offset)
                }
            }.build())?.onEach {
                updateService.receiveUpdate(it)?.also { answer ->
                    sender.sendMessage(answer)
                }
                offset = max(offset, it.updateId + 1)
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}