package com.qchat.server

import com.qchat.server.config.AppConfig
import com.qchat.server.config.configureAuthentication
import com.qchat.server.config.configureContentNegotiation
import com.qchat.server.config.configureCors
import com.qchat.server.config.configureRouting
import com.qchat.server.config.configureWebSockets
import com.qchat.server.database.DatabaseFactory
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    logger.info("Starting QChat Backend Server...")

    // Initialize database
    DatabaseFactory.init()

    // Start embedded server
    embeddedServer(Netty, port = AppConfig.serverPort, host = AppConfig.serverHost) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    logger.info("Configuring Ktor application...")

    // Configure content negotiation (JSON serialization)
    configureContentNegotiation()

    // Configure CORS
    configureCors()

    // Configure authentication (JWT)
    configureAuthentication()

    // Configure WebSockets
    configureWebSockets()

    // Configure routing
    configureRouting()

    logger.info("Ktor application configured successfully")
}
