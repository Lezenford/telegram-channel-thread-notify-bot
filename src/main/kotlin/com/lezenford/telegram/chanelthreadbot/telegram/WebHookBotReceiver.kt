package com.lezenford.telegram.chanelthreadbot.telegram

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import com.lezenford.telegram.chanelthreadbot.service.UpdateService
import kotlinx.coroutines.launch
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import java.nio.file.Files
import kotlin.system.exitProcess

@RestController
@RequestMapping("\${telegram.webhook.prefix:${TelegramProperties.Webhook.DEFAULT_PREFIX}}")
@ConditionalOnProperty(value = ["telegram.type"], havingValue = "WEBHOOK")
class WebHookBotReceiver(
    override val updateService: UpdateService,
    private val properties: TelegramProperties,
    private val sender: BotSender
) : BotReceiver(), CommandLineRunner {

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.OK)
    suspend fun exceptionHandler(e: Exception) {
        log.error("Webhook error", e)
    }

    override fun run(vararg args: String?) {
        launch {
            sender.sendMessage(
                SetWebhook.builder()
                    .apply {
                        properties.webhook.publicKey?.also {
                            val certFile = Files.createTempFile("", "").toFile()
                            certFile.writeText(
                                "-----BEGIN CERTIFICATE-----".plus(
                                    properties.webhook.publicKey.removeSurrounding(
                                        "-----BEGIN CERTIFICATE-----",
                                        "-----END CERTIFICATE-----"
                                    ).replace(" ", "\n")
                                ).plus("-----END CERTIFICATE-----\n")
                            )
                            certFile.deleteOnExit()

                            certificate(InputFile(certFile))
                        }
                    }
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

    @PostMapping("/{token}")
    suspend fun webhook(
        @RequestHeader(required = true, name = TELEGRAM_BOT_API_SECRET_TOKEN_HEADER) secretHeader: String,
        @PathVariable token: String,
        @RequestBody update: Update
    ): BotApiMethod<*>? {
        if (validate(secretHeader, token)) {
            return updateService.receiveUpdate(update)
        }
        return null
    }

    private fun validate(secretHeader: String, token: String): Boolean {
        return token == properties.tokenHash && secretHeader == properties.secretToken
    }

    companion object {
        private val log by Logger()
        private const val TELEGRAM_BOT_API_SECRET_TOKEN_HEADER = "X-Telegram-Bot-Api-Secret-Token"
    }
}
