package com.lezenford.telegram.chanelthreadbot.web

import com.lezenford.telegram.chanelthreadbot.configuration.properties.TelegramProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class LoggingWebFilter(
    @Value("\${telegram.webhook.prefix:${TelegramProperties.Webhook.DEFAULT_PREFIX}}")
    private val telegramControllerPath: String
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return if (exchange.request.path.value().startsWith("/${telegramControllerPath}/")) {
            chain.filter(LoggingWebExchange(exchange))
        } else {
            chain.filter(exchange)
        }
    }
}
