package com.qchat.server.models

import kotlinx.serialization.Serializable

/**
 * API response wrapper
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null
)

/**
 * Error response
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

/**
 * Pagination parameters
 */
@Serializable
data class PaginationParams(
    val page: Int = 1,
    val pageSize: Int = 20
) {
    val offset: Int get() = (page - 1) * pageSize
}

/**
 * Search request
 */
@Serializable
data class SearchRequest(
    val query: String,
    val type: String // "users", "conversations", "messages"
)

/**
 * Health check response
 */
@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long,
    val version: String,
    val uptime: Long
)
