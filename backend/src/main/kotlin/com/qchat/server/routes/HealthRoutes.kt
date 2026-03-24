package com.qchat.server.routes

import com.qchat.server.models.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val startTime = System.currentTimeMillis()

/**
 * Health check routes
 */
fun Route.healthCheck() {
    get {
        val uptime = System.currentTimeMillis() - startTime
        val response = HealthResponse(
            status = "healthy",
            timestamp = System.currentTimeMillis(),
            version = "1.0.0",
            uptime = uptime
        )
        call.respond(response)
    }

    get("/ready") {
        call.respond(
            mapOf(
                "status" to "ready",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    get("/live") {
        call.respond(
            mapOf(
                "status" to "alive",
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
