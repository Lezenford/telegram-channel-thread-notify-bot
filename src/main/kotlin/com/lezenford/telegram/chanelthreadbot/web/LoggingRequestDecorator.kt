package com.lezenford.telegram.chanelthreadbot.web

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class LoggingRequestDecorator(delegate: ServerHttpRequest) : ServerHttpRequestDecorator(delegate) {

    private val body: Flux<DataBuffer> = super.getBody().doOnNext { buffer: DataBuffer ->
        val bodyStream = ByteArrayOutputStream().also {
            Channels.newChannel(it).write(buffer.asByteBuffer().asReadOnlyBuffer())
        }
        log.info("Request: ${String(bodyStream.toByteArray())}")
    }

    override fun getBody(): Flux<DataBuffer> {
        return body
    }

    companion object {
        private val log by Logger()
    }
}
