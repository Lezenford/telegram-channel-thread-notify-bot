package com.lezenford.telegram.chanelthreadbot.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

@ConstructorBinding
@ConfigurationProperties("telegram")
data class TelegramProperties(
    val token: String,
    val type: BotType,
    val secretToken: String = UUID.randomUUID().toString(),
    val limit: Limit = Limit(),
    val userUpdate: UserUpdate = UserUpdate(),
    val longPolling: LongPolling = LongPolling(),
    val webhook: Webhook = Webhook()
) {
    val tokenHash = Base64.getEncoder().encode(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
    ).decodeToString().replace("/", "").replace("+", "").replace("=", "")

    data class Limit(
        val requestPerSecond: Int = 20,
        val threadCount: Int = 5
    )

    data class UserUpdate(
        val rate: Long = DEFAULT_RATE,
        val delay: Long = DEFAULT_DELAY
    ) {
        companion object {
            const val DEFAULT_DELAY = 5000L
            const val DEFAULT_RATE = 600_000L
        }
    }

    data class LongPolling(
        val delay: Long = DEFAULT_DELAY,
        val rate: Long = DEFAULT_RATE
    ) {

        companion object {
            const val DEFAULT_DELAY = 1000L
            const val DEFAULT_RATE = 500L
        }
    }

    data class Webhook(
        val url: String = "",
        val prefix: String = DEFAULT_PREFIX,
        val publicKey: String? = null
    ) {
        companion object {
            const val DEFAULT_PREFIX = "telegram"
        }
    }

    enum class BotType {
        WEBHOOK, LONG_POLLING
    }
}
