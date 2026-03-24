package com.qchat.server.routes

import com.qchat.server.models.*
import com.qchat.server.services.UserService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthRoutes")
private val userService = UserService()

/**
 * Authentication routes
 */
fun Route.authRoutes() {
    route("/auth") {
        // Register
        post("/register") {
            logger.info("Registration request received")
            try {
                val request = call.receive<RegisterRequest>()
                val result = userService.registerUser(request)

                if (result.isSuccess) {
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = result.getOrNull()
                        )
                    )
                } else {
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "REGISTRATION_FAILED",
                                message = result.exceptionOrNull()?.message ?: "Registration failed"
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Registration error", e)
                call.respond(
                    ApiResponse(
                        success = false,
                        error = ErrorResponse(
                            code = "INVALID_REQUEST",
                            message = e.message ?: "Invalid request"
                        )
                    )
                )
            }
        }

        // Login
        post("/login") {
            logger.info("Login request received")
            try {
                val request = call.receive<LoginRequest>()
                val result = userService.authenticateUser(request)

                if (result.isSuccess) {
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = result.getOrNull()
                        )
                    )
                } else {
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "AUTHENTICATION_FAILED",
                                message = result.exceptionOrNull()?.message ?: "Authentication failed"
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Login error", e)
                call.respond(
                    ApiResponse(
                        success = false,
                        error = ErrorResponse(
                            code = "INVALID_REQUEST",
                            message = e.message ?: "Invalid request"
                        )
                    )
                )
            }
        }

        // Refresh token
        post("/refresh") {
            logger.info("Token refresh request received")
            try {
                val request = call.receive<RefreshTokenRequest>()
                // TODO: Implement token refresh logic
                call.respond(
                    ApiResponse(
                        success = false,
                        error = ErrorResponse(
                            code = "NOT_IMPLEMENTED",
                            message = "Token refresh not implemented"
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Token refresh error", e)
                call.respond(
                    ApiResponse(
                        success = false,
                        error = ErrorResponse(
                            code = "INVALID_REQUEST",
                            message = e.message ?: "Invalid request"
                        )
                    )
                )
            }
        }

        // Logout
        post("/logout") {
            logger.info("Logout request received")
            // TODO: Implement logout logic
            call.respond(
                ApiResponse(
                    success = true,
                    data = mapOf("message" to "Logged out successfully")
                )
            )
        }
    }
}
