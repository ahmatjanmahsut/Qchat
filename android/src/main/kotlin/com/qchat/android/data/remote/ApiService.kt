package com.qchat.android.data.remote

import com.qchat.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 消息API服务
 */
interface MessageApiService {

    /**
     * 发送消息
     */
    @POST("api/v1/messages/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<MessageResponse>

    /**
     * 获取消息
     */
    @GET("api/v1/messages/{chatId}")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("before") beforeTimestamp: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<MessagesResponse>

    /**
     * 获取单条消息
     */
    @GET("api/v1/messages/single/{messageId}")
    suspend fun getMessage(@Path("messageId") messageId: String): Response<MessageResponse>

    /**
     * 删除消息
     */
    @DELETE("api/v1/messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<Unit>

    /**
     * 标记消息已读
     */
    @PUT("api/v1/messages/{messageId}/read")
    suspend fun markAsRead(@Path("messageId") messageId: String): Response<Unit>

    /**
     * 批量标记已读
     */
    @PUT("api/v1/messages/read/batch")
    suspend fun markAsReadBatch(@Body request: BatchMessageStatusUpdate): Response<Unit>

    /**
     * 编辑消息
     */
    @PUT("api/v1/messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId: String,
        @Body request: EditMessageRequest
    ): Response<MessageResponse>

    /**
     * 获取消息状态
     */
    @GET("api/v1/messages/{messageId}/status")
    suspend fun getMessageStatus(@Path("messageId") messageId: String): Response<MessageStatusResponse>

    /**
     * 转发消息
     */
    @POST("api/v1/messages/forward")
    suspend fun forwardMessage(@Body request: ForwardMessageRequest): Response<MessagesResponse>
}

/**
 * 聊天API服务
 */
interface ChatApiService {

    /**
     * 获取聊天列表
     */
    @GET("api/v1/chats")
    suspend fun getChats(): Response<ChatsResponse>

    /**
     * 获取私聊信息
     */
    @GET("api/v1/chats/private/{userId}")
    suspend fun getPrivateChat(@Path("userId") userId: String): Response<ChatResponse>

    /**
     * 创建私聊
     */
    @POST("api/v1/chats/private")
    suspend fun createPrivateChat(@Body request: CreatePrivateChatRequest): Response<ChatResponse>

    /**
     * 获取群组信息
     */
    @GET("api/v1/chats/group/{groupId}")
    suspend fun getGroupChat(@Path("groupId") groupId: String): Response<GroupChatResponse>

    /**
     * 创建群组
     */
    @POST("api/v1/chats/group")
    suspend fun createGroupChat(@Body request: CreateGroupChatRequest): Response<ChatResponse>

    /**
     * 更新群组
     */
    @PUT("api/v1/chats/group/{groupId}")
    suspend fun updateGroupChat(
        @Path("groupId") groupId: String,
        @Body request: UpdateGroupChatRequest
    ): Response<ChatResponse>

    /**
     * 添加群组成员
     */
    @POST("api/v1/chats/group/{groupId}/members")
    suspend fun addGroupMembers(
        @Path("groupId") groupId: String,
        @Body request: AddMembersRequest
    ): Response<Unit>

    /**
     * 移除群组成员
     */
    @DELETE("api/v1/chats/group/{groupId}/members/{userId}")
    suspend fun removeGroupMember(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    /**
     * 离开群组
     */
    @POST("api/v1/chats/group/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String): Response<Unit>

    /**
     * 删除聊天
     */
    @DELETE("api/v1/chats/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: String): Response<Unit>

    /**
     * 更新聊天设置
     */
    @PUT("api/v1/chats/{chatId}/settings")
    suspend fun updateChatSettings(
        @Path("chatId") chatId: String,
        @Body request: UpdateChatSettingsRequest
    ): Response<Unit>
}

/**
 * 用户API服务
 */
interface UserApiService {

    /**
     * 获取用户信息
     */
    @GET("api/v1/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<UserResponse>

    /**
     * 更新用户信息
     */
    @PUT("api/v1/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserResponse>

    /**
     * 上传头像
     */
    @POST("api/v1/users/me/avatar")
    suspend fun uploadAvatar(@Body request: UploadAvatarRequest): Response<AvatarResponse>

    /**
     * 获取用户公钥
     */
    @GET("api/v1/users/{userId}/keys")
    suspend fun getUserPublicKeys(@Path("userId") userId: String): Response<PublicKeysResponse>

    /**
     * 获取预密钥包
     */
    @GET("api/v1/users/{userId}/prekey-bundle")
    suspend fun getPreKeyBundle(@Path("userId") userId: String): Response<PreKeyBundleResponse>

    /**
     * 上传预密钥包
     */
    @POST("api/v1/users/me/prekey-bundle")
    suspend fun uploadPreKeyBundle(@Body request: UploadPreKeyBundleRequest): Response<Unit>

    /**
     * 获取设备列表
     */
    @GET("api/v1/users/me/devices")
    suspend fun getDevices(): Response<DevicesResponse>

    /**
     * 注册设备
     */
    @POST("api/v1/users/me/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<DeviceResponse>

    /**
     * 删除设备
     */
    @DELETE("api/v1/users/me/devices/{deviceId}")
    suspend fun removeDevice(@Path("deviceId") deviceId: String): Response<Unit>
}

/**
 * 认证API服务
 */
interface AuthApiService {

    /**
     * 用户注册
     */
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * 用户登录
     */
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /**
     * 刷新令牌
     */
    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    /**
     * 退出登录
     */
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    /**
     * 验证令牌
     */
    @GET("api/v1/auth/verify")
    suspend fun verifyToken(): Response<UserResponse>
}
