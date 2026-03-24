package com.qchat.server.routes

import com.auth0.jwt.interfaces.DecodedJWT
import com.qchat.server.models.*
import com.qchat.server.security.JwtService
import com.qchat.server.services.UserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("UserRoutes")
private val userService = UserService()

/**
 * User routes
 */
fun Route.userRoutes() {
    route("/users") {
        // Get current user profile
        authenticate("auth-jwt") {
            get("/me") {
                logger.info("Get current user profile request")
                try {
                    val userId = getUserIdFromToken(call)
                    if (userId == null) {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "UNAUTHORIZED",
                                    message = "Invalid or missing authentication token"
                                )
                            )
                        )
                        return@get
                    }

                    val profile = userService.getUserProfile(userId)
                    if (profile != null) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = profile
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "USER_NOT_FOUND",
                                    message = "User not found"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Get profile error", e)
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "INTERNAL_ERROR",
                                message = e.message ?: "Internal server error"
                            )
                        )
                    )
                }
            }
        }

        // Get user by ID
        get("/{userId}") {
            logger.info("Get user profile request")
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "INVALID_PARAMETER",
                                message = "User ID is required"
                            )
                        )
                    )
                    return@get
                }

                val profile = userService.getUserProfile(userId)
                if (profile != null) {
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = profile
                        )
                    )
                } else {
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "USER_NOT_FOUND",
                                message = "User not found"
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Get user error", e)
                call.respond(
                    ApiResponse(
                        success = false,
                        error = ErrorResponse(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Internal server error"
                        )
                    )
                )
            }
        }

        // Search users
        get("/search") {
            logger.info("Search users request")
            try {
                val query = call.parameters["q"] ?: ""
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20

                if (query.length < 2) {
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "INVALID_QUERY",
                                message = "Search query must be at least 2 characters"
                            )
                        )
                    )
                    return@get
                }

                val users = userService.searchUsers(query, limit)
                call.respond(
                    ApiResponse(
                        success = true,
                        data = users
                    )
                )
            } catch (e: Exception) {
                logger.error("Search users error", e)
                call.respond(
                    ApiResponse(
                        success = false,
                        error = ErrorResponse(
                            code = "INTERNAL_ERROR",
                            message = e.message ?: "Internal server error"
                        )
                    )
                )
            }
        }

        // Update current user profile
        authenticate("auth-jwt") {
            put("/me") {
                logger.info("Update profile request")
                try {
                    val userId = getUserIdFromToken(call)
                    if (userId == null) {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "UNAUTHORIZED",
                                    message = "Invalid or missing authentication token"
                                )
                            )
                        )
                        return@put
                    }

                    val request = call.receive<UpdateProfileRequest>()
                    val result = userService.updateUserProfile(userId, request)

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
                                    code = "UPDATE_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Update failed"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Update profile error", e)
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "INTERNAL_ERROR",
                                message = e.message ?: "Internal server error"
                            )
                        )
                    )
                }
            }

            // Change password
            post("/me/change-password") {
                logger.info("Change password request")
                try {
                    val userId = getUserIdFromToken(call)
                    if (userId == null) {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "UNAUTHORIZED",
                                    message = "Invalid or missing authentication token"
                                )
                            )
                        )
                        return@post
                    }

                    val request = call.receive<ChangePasswordRequest>()
                    val result = userService.changePassword(userId, request.currentPassword, request.newPassword)

                    if (result.isSuccess) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("message" to "Password changed successfully")
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "PASSWORD_CHANGE_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Password change failed"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Change password error", e)
                    call.respond(
                        ApiResponse(
                            success = false,
                            error = ErrorResponse(
                                code = "INTERNAL_ERROR",
                                message = e.message ?: "Internal server error"
                            )
                        )
                    )
                }
            }
        }
    }
}

/**
 * Extract user ID from JWT token in request
 */
private fun getUserIdFromToken(call: ApplicationCall): String? {
    val authHeader = call.request.headers["Authorization"]
    val token = authHeader?.replace("Bearer ", "")

    return token?.let {
        try {
            JwtService.decodeToken(it)?.subject
        } catch (e: Exception) {
            null
        }
    }
}
