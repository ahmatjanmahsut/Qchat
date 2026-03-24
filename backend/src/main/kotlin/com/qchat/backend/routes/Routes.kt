package com.qchat.backend.routes

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.auth.*
import io.ktor.server.websocket.*
import com.qchat.backend.database.dao.*
import com.qchat.backend.plugins.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class UserDto(
    val id: Long,
    val phoneNumber: String,
    val username: String? = null,
    val displayName: String,
    val profilePicture: String? = null,
    val publicKey: String,
    val lastSeen: Long? = null,
    val isOnline: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class MessageDto(
    val id: Long,
    val sessionId: Long,
    val senderId: Long,
    val content: String,
    val contentType: String,
    val status: String,
    val isOutgoing: Boolean,
    val replyToMessageId: Long? = null,
    val editedAt: Long? = null,
    val deletedAt: Long? = null,
    val serverTimestamp: Long? = null,
    val version: Int = 1
)

@Serializable
data class SessionDto(
    val id: Long,
    val type: String,
    val participantId: Long? = null,
    val groupId: Long? = null,
    val name: String,
    val avatar: String? = null,
    val lastMessageId: Long? = null,
    val lastMessageContent: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

fun Routing.userRoutes() {
    route("/api/users") {
        get("/me") {
            // 获取当前用户
        }

        get("/{userId}") {
            // 获取用户信息
        }

        get("/contacts") {
            // 获取联系人列表
        }

        get("/search") {
            // 搜索用户
        }

        put("/me") {
            // 更新用户信息
        }
    }
}

fun Routing.sessionRoutes() {
    route("/api/sessions") {
        get {
            // 获取会话列表
        }

        get("/{sessionId}") {
            // 获取会话详情
        }

        post {
            // 创建会话
        }

        put("/{sessionId}") {
            // 更新会话
        }

        delete("/{sessionId}") {
            // 删除会话
        }

        post("/{sessionId}/read") {
            // 标记会话已读
        }
    }
}

fun Routing.messageRoutes() {
    route("/api/messages") {
        get("/sessions/{sessionId}") {
            // 获取会话消息
        }

        get("/{messageId}") {
            // 获取消息详情
        }

        post {
            // 发送消息
        }

        put("/{messageId}") {
            // 更新消息
        }

        delete("/{messageId}") {
            // 删除消息
        }

        post("/{messageId}/read") {
            // 标记消息已读
        }
    }
}

fun Routing.groupRoutes() {
    route("/api/groups") {
        get {
            // 获取群组列表
        }

        get("/{groupId}") {
            // 获取群组详情
        }

        post {
            // 创建群组
        }

        put("/{groupId}") {
            // 更新群组
        }

        delete("/{groupId}") {
            // 删除群组
        }

        post("/{groupId}/leave") {
            // 离开群组
        }

        get("/{groupId}/members") {
            // 获取群组成员
        }

        post("/{groupId}/members") {
            // 添加群组成员
        }

        delete("/{groupId}/members/{userId}") {
            // 移除群组成员
        }
    }
}

fun Routing.contactRoutes() {
    route("/api/contacts") {
        get {
            // 获取联系人列表
        }

        post {
            // 添加联系人
        }

        put("/{contactId}") {
            // 更新联系人
        }

        delete("/{contactId}") {
            // 删除联系人
        }

        post("/import") {
            // 批量导入联系人
        }

        post("/{contactId}/block") {
            // 屏蔽联系人
        }
    }
}

fun Routing.authRoutes() {
    route("/api/auth") {
        post("/register") {
            // 用户注册
        }

        post("/login") {
            // 用户登录
        }

        post("/logout") {
            // 用户登出
        }

        post("/refresh") {
            // 刷新Token
        }
    }
}

fun Routing.syncRoutes() {
    route("/api/sync") {
        post("/messages") {
            // 同步消息
        }

        post("/sessions") {
            // 同步会话
        }

        post("/users") {
            // 同步用户
        }

        post("/groups") {
            // 同步群组
        }

        post("/contacts") {
            // 同步联系人
        }
    }
}

fun Routing.wsRoutes() {
    webSocket("/ws") {
        // WebSocket连接处理
        val userId = call.parameters["userId"]?.toLongOrNull() ?: return@webSocket

        webSocketManager.addSession(userId, this)

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        // 处理WebSocket消息
                    }
                    is Frame.Close -> {
                        // 处理关闭
                    }
                    else -> {}
                }
            }
        } finally {
            webSocketManager.removeSession(userId)
        }
    }
}
