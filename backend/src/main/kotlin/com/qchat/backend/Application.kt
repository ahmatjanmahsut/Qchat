package com.qchat.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.qchat.backend.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSecurity()
        configureWebSockets()
        configureSerialization()
        configureMonitoring()
    }.start(wait = true)
}

fun Application.module() {
    configureRouting()
    configureSecurity()
    configureWebSockets()
    configureSerialization()
    configureMonitoring()
}
