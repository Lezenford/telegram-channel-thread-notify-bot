package com.lezenford.telegram.chanelthreadbot.web

import com.lezenford.telegram.chanelthreadbot.extensions.Logger
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.nio.charset.StandardCharsets

@Component
class LoggingHttpClient : HttpClient(SslContextFactory.Client()) {
    override fun newRequest(uri: URI): Request {
        val request: Request = super.newRequest(uri)
        return enhance(request)
    }

    private fun enhance(request: Request): Request {
        val group = StringBuilder()
        request.onRequestBegin { }
        request.onRequestContent { _, content ->
            group.append(StandardCharsets.UTF_8.decode(content))
        }
        request.onRequestSuccess {
            log.info("Request: $group")
            group.clear()
        }
        request.onResponseContent { _, content ->
            group.append("${StandardCharsets.UTF_8.decode(content)}\n")
        }
        request.onResponseSuccess {
            log.info("Response: $group")
            group.clear()
        }
        return request
    }

    companion object {
        private val log by Logger()
    }
}
