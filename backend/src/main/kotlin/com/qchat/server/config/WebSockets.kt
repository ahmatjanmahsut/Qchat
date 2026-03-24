package com.qchat.server.config

import com.qchat.server.services.ChatWebSocketService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.websocket.webSocket
import io.ktor.server.routing.routing
import java.time.Duration

/**
 * Configure WebSockets for real-time chat
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(60)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/chat") {
            ChatWebSocketService.handleWebSocket(this)
        }
    }
}
