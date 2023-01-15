@file:Suppress("UnstableApiUsage")
@file:OptIn(DelicateCoroutinesApi::class)

package com.lezenford.telegram.chanelthreadbot.telegram

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.util.concurrent.RateLimiter
import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.ApiResponse
import reactor.util.retry.Retry
import java.io.Serializable
import java.time.Duration
import kotlin.coroutines.CoroutineContext

@Service
class BotSender(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    properties: TelegramProperties
) : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        newFixedThreadPoolContext(properties.limit.threadCount, "telegramSendPool")
    private val rateLimiter = RateLimiter.create(properties.limit.requestPerSecond.toDouble())

    suspend fun <T : Serializable> sendMessage(
        message: BotApiMethod<T>,
        onErrorResponseAction: suspend (ApiResponse<Unit>) -> Unit = { }
    ): T? {
        return withContext(coroutineContext) {
            kotlin.runCatching {
                rateLimiter.acquire()
                webClient.post()
                    .uri(message.method).let {
                        when (message) {
                            is SetWebhook -> {
                                val multipartBodyBuilder = MultipartBodyBuilder().apply {
                                    message.certificate?.newMediaFile?.also { file ->
                                        part(SetWebhook.CERTIFICATE_FIELD, FileSystemResource(file))
                                    }
                                    part(SetWebhook.URL_FIELD, message.url)
                                    part(SetWebhook.SECRETTOKEN_FIELD, message.secretToken)
                                }

                                it.contentType(MediaType.MULTIPART_FORM_DATA)
                                it.bodyValue(multipartBodyBuilder.build())
                            }

                            else -> {
                                it.contentType(MediaType.APPLICATION_JSON)
                                it.bodyValue(message)
                            }
                        }
                    }
                    .exchangeToMono { it.toEntity<String>() }
                    .doOnError { log.error("Error invoke telegram api: ${it.message}") }
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                    .doOnError { log.error("Error invoke telegram api after all retry count: ${it.message}", it) }
                    .onErrorComplete()
                    .awaitSingleOrNull()?.let { response ->
                        if (response.statusCode.is2xxSuccessful) {
                            message.deserializeResponse(response.body)
                        } else {
                            response.body?.let { objectMapper.readValue<ApiResponse<Unit>>(it) }
                                ?.also {
                                    log.warn("Telegram api return error ${it.errorCode}: ${it.errorDescription}")
                                    onErrorResponseAction(it)
                                }
                            null
                        }
                    }
            }.onFailure {
                log.error("send message error", it)
            }.getOrNull()
        }
    }

    companion object {
        private val log by Logger()
    }
}
