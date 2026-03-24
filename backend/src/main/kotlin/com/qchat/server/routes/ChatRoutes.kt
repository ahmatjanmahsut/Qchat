package com.qchat.server.routes

import com.qchat.server.models.*
import com.qchat.server.security.JwtService
import com.qchat.server.services.ChatWebSocketService
import com.qchat.server.services.ConversationService
import com.qchat.server.services.MessageService
import com.qchat.server.services.UserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ChatRoutes")
private val conversationService = ConversationService()
private val messageService = MessageService()

/**
 * Hash user ID for logging (privacy protection)
 */
private fun hashUserId(userId: String): String = userId.hashCode().toString(16)

/**
 * Chat routes for conversations and messages
 */
fun Route.chatRoutes() {
    route("/conversations") {
        // Get user's conversations
        authenticate("auth-jwt") {
            get {
                logger.debug("Get conversations request")
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

                    val conversations = conversationService.getUserConversations(userId)
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = conversations
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Get conversations error", e)
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

        // Create new conversation
        authenticate("auth-jwt") {
            post {
                logger.info("Create conversation request")
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

                    val request = call.receive<CreateConversationRequest>()
                    val result = conversationService.createConversation(userId, request)

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
                                    code = "CREATE_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Failed to create conversation"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Create conversation error", e)
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

        // Get conversation by ID
        authenticate("auth-jwt") {
            get("/{conversationId}") {
                logger.info("Get conversation request")
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

                    val conversationId = call.parameters["conversationId"]
                    if (conversationId == null) {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "INVALID_PARAMETER",
                                    message = "Conversation ID is required"
                                )
                            )
                        )
                        return@get
                    }

                    val conversation = conversationService.getConversationById(conversationId, userId)
                    if (conversation != null) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = conversation
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "NOT_FOUND",
                                    message = "Conversation not found"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Get conversation error", e)
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

        // Add participant to group
        authenticate("auth-jwt") {
            post("/{conversationId}/participants") {
                logger.info("Add participant request")
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

                    val conversationId = call.parameters["conversationId"]
                        ?: return@post call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "INVALID_PARAMETER",
                                    message = "Conversation ID is required"
                                )
                            )
                        )

                    val request = call.receive<AddParticipantRequest>()
                    val result = conversationService.addParticipant(conversationId, userId, request.userId)

                    if (result.isSuccess) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("message" to "Participant added successfully")
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "ADD_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Failed to add participant"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Add participant error", e)
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

        // Remove participant from group
        authenticate("auth-jwt") {
            delete("/{conversationId}/participants/{userId}") {
                logger.info("Remove participant request")
                try {
                    val requesterId = getUserIdFromToken(call)
                    if (requesterId == null) {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "UNAUTHORIZED",
                                    message = "Invalid or missing authentication token"
                                )
                            )
                        )
                        return@delete
                    }

                    val conversationId = call.parameters["conversationId"]
                        ?: return@delete call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "INVALID_PARAMETER",
                                    message = "Conversation ID is required"
                                )
                            )
                        )

                    val userId = call.parameters["userId"]
                        ?: return@delete call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "INVALID_PARAMETER",
                                    message = "User ID is required"
                                )
                            )
                        )

                    val result = conversationService.removeParticipant(conversationId, requesterId, userId)

                    if (result.isSuccess) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("message" to "Participant removed successfully")
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "REMOVE_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Failed to remove participant"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Remove participant error", e)
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

    // Messages routes
    route("/messages") {
        // Send message
        authenticate("auth-jwt") {
            post {
                logger.info("Send message request")
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

                    val request = call.receive<SendMessageRequest>()
                    val result = messageService.sendMessage(userId, request)

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
                                    code = "SEND_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Failed to send message"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Send message error", e)
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

        // Get messages for conversation
        authenticate("auth-jwt") {
            get {
                logger.info("Get messages request")
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

                    val conversationId = call.parameters["conversationId"]
                        ?: return@get call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "INVALID_PARAMETER",
                                    message = "Conversation ID is required"
                                )
                            )
                        )

                    val page = call.parameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20

                    val messages = messageService.getMessages(conversationId, userId, page, pageSize)
                    call.respond(
                        ApiResponse(
                            success = true,
                            data = messages
                        )
                    )
                } catch (e: Exception) {
                    logger.error("Get messages error", e)
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

        // Mark messages as read
        authenticate("auth-jwt") {
            post("/read") {
                logger.info("Mark messages as read request")
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

                    val request = call.receive<MarkReadRequest>()
                    val result = messageService.markAsRead(request.conversationId, userId, request.messageId)

                    if (result.isSuccess) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("message" to "Messages marked as read")
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "MARK_READ_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Failed to mark messages as read"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Mark as read error", e)
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

        // Delete message
        authenticate("auth-jwt") {
            delete("/{messageId}") {
                logger.info("Delete message request")
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
                        return@delete
                    }

                    val messageId = call.parameters["messageId"]
                        ?: return@delete call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "INVALID_PARAMETER",
                                    message = "Message ID is required"
                                )
                            )
                        )

                    val result = messageService.deleteMessage(messageId, userId)

                    if (result.isSuccess) {
                        call.respond(
                            ApiResponse(
                                success = true,
                                data = mapOf("message" to "Message deleted")
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse(
                                success = false,
                                error = ErrorResponse(
                                    code = "DELETE_FAILED",
                                    message = result.exceptionOrNull()?.message ?: "Failed to delete message"
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Delete message error", e)
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
 * Extract user ID from JWT token in request with proper validation
 */
private fun getUserIdFromToken(call: ApplicationCall): String? {
    val authHeader = call.request.headers["Authorization"]
    val token = authHeader?.replace("Bearer ", "") ?: return null

    return try {
        // Use proper JWT verification instead of just decoding
        val verifier = JwtService.makeVerifier()
        val jwt = verifier.verify(token)
        
        // Check expiration
        if (jwt.expiresAt < java.util.Date()) {
            logger.warn("JWT token has expired")
            return null
        }
        
        // Validate issuer
        if (jwt.issuer != com.qchat.server.config.AppConfig.jwtIssuer) {
            logger.warn("Invalid JWT issuer: ${jwt.issuer}")
            return null
        }
        
        jwt.subject
    } catch (e: Exception) {
        logger.error("JWT validation failed: ${e.message}")
        null
    }
}
