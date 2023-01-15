package com.lezenford.telegram.chanelthreadbot.configuration

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import org.eclipse.jetty.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.JettyClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class TelegramConfiguration(
    private val properties: TelegramProperties
) {

    @Bean
    fun telegramWebClient(httpClient: HttpClient): WebClient = WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .baseUrl("https://api.telegram.org/bot${properties.token}/")
        .clientConnector(JettyClientHttpConnector(httpClient))
        .build()
}
