package com.lezenford.telegram.chanelthreadbot.configuration

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class TelegramConfiguration(
    private val properties: TelegramProperties
) {
    @Bean
    fun telegramWebClient(): WebClient = WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .baseUrl("https://api.telegram.org/bot${properties.token}/")
        .build()
}