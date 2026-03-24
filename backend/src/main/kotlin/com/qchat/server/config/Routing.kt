package com.qchat.server.config

import com.qchat.server.routes.authRoutes
import com.qchat.server.routes.chatRoutes
import com.qchat.server.routes.healthCheck
import com.qchat.server.routes.userRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Configure API routes
 */
fun Application.configureRouting() {
    routing {
        // Health check endpoint
        route("/api/v1") {
            route("/health") {
                healthCheck()
            }
            authRoutes()
            userRoutes()
            chatRoutes()
        }
    }
}
