package com.lezenford.telegram.chanelthreadbot.telegram

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.service.UpdateService
import kotlinx.coroutines.launch
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.system.exitProcess

@RestController
@RequestMapping("\${telegram.webhook.prefix:${TelegramProperties.Webhook.DEFAULT_PREFIX}}")
@ConditionalOnProperty(value = ["telegram.type"], havingValue = "WEBHOOK")
class WebHookBotReceiver(
    override val updateService: UpdateService,
    private val properties: TelegramProperties,
    private val sender: BotSender
) : BotReceiver(), CommandLineRunner {

    // @ExceptionHandler(Exception::class)
    // @ResponseStatus(HttpStatus.OK)
    // suspend fun exceptionHandler(e: Exception) {
    //     log.error("Webhook error", e)
    // }

    override fun run(vararg args: String?) {
        launch {
            sender.sendMessage(
                SetWebhook.builder()
                    .secretToken(properties.secretToken)
                    .url("${properties.webhook.url}/${properties.webhook.prefix}/${properties.tokenHash}")
                    .build()
            )?.also {
                log.info("Webhook successfully setup")
            } ?: run {
                log.error("Error setup webhook")
                exitProcess(0)
            }
        }
    }

    @Volatile
    private var count = 0

    @PostMapping("/{token}")
    suspend fun webhook(
        @RequestHeader(required = true, name = "X-Telegram-Bot-Api-Secret-Token") secretHeader: String,
        @PathVariable token: String,
        @RequestBody update: Update
    ): BotApiMethod<*>? {
        // if (count > 1){
        //     exitProcess(0)
        // }
        val botApiMethod = if (validate(secretHeader, token)) {
            updateService.receiveUpdate(update)
        } else {
            null
        }
        // if (count > 1){
        //     exitProcess(0)
        // }
        // if (botApiMethod != null){
        //     count++
        //     log.info("increment")
        // }
        return botApiMethod
    }

    private fun validate(secretHeader: String, token: String): Boolean {
        return token == properties.tokenHash && secretHeader == properties.secretToken
    }

    companion object {
        private val log by Logger()
    }
}