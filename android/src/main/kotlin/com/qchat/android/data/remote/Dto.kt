package com.qchat.android.data.remote

import com.qchat.android.data.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ Request Models ============

@Serializable
data class SendMessageRequest(
    @SerialName("recipient")
    val recipient: String,
    @SerialName("encrypted_content")
    val encryptedContent: String,
    @SerialName("encryption_metadata")
    val encryptionMetadata: EncryptionMetadata,
    @SerialName("type")
    val type: MessageType,
    @SerialName("reply_to")
    val replyTo: String? = null
)

@Serializable
data class EditMessageRequest(
    @SerialName("encrypted_content")
    val encryptedContent: String,
    @SerialName("encryption_metadata")
    val encryptionMetadata: EncryptionMetadata
)

@Serializable
data class ForwardMessageRequest(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("recipients")
    val recipients: List<String>
)

@Serializable
data class CreatePrivateChatRequest(
    @SerialName("user_id")
    val userId: String
)

@Serializable
data class CreateGroupChatRequest(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("member_ids")
    val memberIds: List<String>
)

@Serializable
data class UpdateGroupChatRequest(
    @SerialName("name")
    val name: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

@Serializable
data class AddMembersRequest(
    @SerialName("member_ids")
    val memberIds: List<String>
)

@Serializable
data class UpdateChatSettingsRequest(
    @SerialName("is_muted")
    val isMuted: Boolean? = null,
    @SerialName("is_pinned")
    val isPinned: Boolean? = null,
    @SerialName("is_archived")
    val isArchived: Boolean? = null
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("bio")
    val bio: String? = null
)

@Serializable
data class UploadAvatarRequest(
    @SerialName("avatar_data")
    val avatarData: String // Base64
)

@Serializable
data class UploadPreKeyBundleRequest(
    @SerialName("pre_key_bundle")
    val preKeyBundle: PreKeyBundle,
    @SerialName("signed_pre_key")
    val signedPreKey: SignedPreKey
)

@Serializable
data class RegisterDeviceRequest(
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("device_token")
    val deviceToken: String,
    @SerialName("platform")
    val platform: String
)

@Serializable
data class RegisterRequest(
    @SerialName("username")
    val username: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("password")
    val password: String
)

@Serializable
data class LoginRequest(
    @SerialName("username")
    val username: String,
    @SerialName("password")
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

// ============ Response Models ============

@Serializable
data class MessageResponse(
    @SerialName("message")
    val message: Message
)

@Serializable
data class MessagesResponse(
    @SerialName("messages")
    val messages: List<Message>,
    @SerialName("has_more")
    val hasMore: Boolean
)

@Serializable
data class MessageStatusResponse(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("status")
    val status: MessageStatus,
    @SerialName("delivered_at")
    val deliveredAt: Long? = null,
    @SerialName("read_at")
    val readAt: Long? = null
)

@Serializable
data class ChatResponse(
    @SerialName("chat")
    val chat: Chat
)

@Serializable
data class ChatsResponse(
    @SerialName("chats")
    val chats: List<Chat>
)

@Serializable
data class GroupChatResponse(
    @SerialName("group")
    val group: GroupChatInfo
)

@Serializable
data class UserResponse(
    @SerialName("user")
    val user: User
)

@Serializable
data class PublicKeysResponse(
    @SerialName("identity_key")
    val identityKey: String,
    @SerialName("signed_pre_key")
    val signedPreKey: SignedPreKey
)

@Serializable
data class PreKeyBundleResponse(
    @SerialName("pre_key_bundle")
    val preKeyBundle: PreKeyBundle
)

@Serializable
data class DevicesResponse(
    @SerialName("devices")
    val devices: List<DeviceInfo>
)

@Serializable
data class DeviceResponse(
    @SerialName("device")
    val device: DeviceInfo
)

@Serializable
data class AvatarResponse(
    @SerialName("avatar_url")
    val avatarUrl: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("user")
    val user: User,
    @SerialName("expires_in")
    val expiresIn: Long
)
