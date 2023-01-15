package com.lezenford.telegram.chanelthreadbot.web

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class LoggingResponseDecorator(delegate: ServerHttpResponse) : ServerHttpResponseDecorator(delegate) {

    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        return super.writeWith(
            Flux.from(body)
                .doOnNext { buffer: DataBuffer ->
                    val bodyStream = ByteArrayOutputStream().also {
                        Channels.newChannel(it).write(buffer.asByteBuffer().asReadOnlyBuffer())
                    }
                    log.info("Response: ${String(bodyStream.toByteArray())}")
                }
        )
    }

    companion object {
        private val log by Logger()
    }
}
