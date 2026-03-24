package com.qchat.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header("X-Application", "Qchat Backend")
    }

    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(
                text = """{"success": false, "error": "Not Found"}""",
                contentType = ContentType.Application.Json,
                status = status
            )
        }
        status(HttpStatusCode.InternalServerError) { call, status ->
            call.respondText(
                text = """{"success": false, "error": "Internal Server Error"}""",
                contentType = ContentType.Application.Json,
                status = status
            )
        }
        exception<Exception> { call, cause ->
            call.respondText(
                text = """{"success": false, "error": "${cause.message}"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
