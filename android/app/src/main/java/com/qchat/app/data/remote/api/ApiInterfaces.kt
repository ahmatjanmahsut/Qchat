package com.qchat.app.data.remote.api

import com.qchat.app.data.remote.dto.ContactDto
import com.qchat.app.data.remote.dto.GroupDto
import com.qchat.app.data.remote.dto.GroupMemberDto
import com.qchat.app.data.remote.dto.MessageDto
import com.qchat.app.data.remote.dto.SessionDto
import com.qchat.app.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 用户API接口
 */
interface UserApi {
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserDto>

    @GET("users/contacts")
    suspend fun getContacts(): Response<List<UserDto>>

    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<UserDto>>

    @PUT("users/me")
    suspend fun updateProfile(@Body user: UserDto): Response<UserDto>
}

/**
 * 会话API接口
 */
interface SessionApi {
    @GET("sessions")
    suspend fun getSessions(): Response<List<SessionDto>>

    @GET("sessions/{sessionId}")
    suspend fun getSessionById(@Path("sessionId") sessionId: String): Response<SessionDto>

    @POST("sessions")
    suspend fun createSession(@Body session: SessionDto): Response<SessionDto>

    @PUT("sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: String,
        @Body session: SessionDto
    ): Response<SessionDto>

    @DELETE("sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<Unit>

    @POST("sessions/{sessionId}/read")
    suspend fun markAsRead(@Path("sessionId") sessionId: String): Response<Unit>
}

/**
 * 消息API接口
 */
interface MessageApi {
    @GET("sessions/{sessionId}/messages")
    suspend fun getMessages(
        @Path("sessionId") sessionId: String,
        @Query("since") sinceTimestamp: Long? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MessageDto>>

    @GET("messages/{messageId}")
    suspend fun getMessageById(@Path("messageId") messageId: String): Response<MessageDto>

    @POST("messages")
    suspend fun sendMessage(@Body message: MessageDto): Response<MessageDto>

    @PUT("messages/{messageId}")
    suspend fun updateMessage(
        @Path("messageId") messageId: String,
        @Body message: MessageDto
    ): Response<MessageDto>

    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<Unit>

    @POST("messages/{messageId}/read")
    suspend fun markAsRead(@Path("messageId") messageId: String): Response<Unit>
}

/**
 * 群组API接口
 */
interface GroupApi {
    @GET("groups")
    suspend fun getGroups(): Response<List<GroupDto>>

    @GET("groups/{groupId}")
    suspend fun getGroupById(@Path("groupId") groupId: String): Response<GroupDto>

    @POST("groups")
    suspend fun createGroup(@Body group: GroupDto): Response<GroupDto>

    @PUT("groups/{groupId}")
    suspend fun updateGroup(
        @Path("groupId") groupId: String,
        @Body group: GroupDto
    ): Response<GroupDto>

    @DELETE("groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: String): Response<Unit>

    @POST("groups/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String): Response<Unit>

    @GET("groups/{groupId}/members")
    suspend fun getMembers(@Path("groupId") groupId: String): Response<List<GroupMemberDto>>

    @POST("groups/{groupId}/members")
    suspend fun addMember(
        @Path("groupId") groupId: String,
        @Body member: GroupMemberDto
    ): Response<GroupMemberDto>

    @DELETE("groups/{groupId}/members/{userId}")
    suspend fun removeMember(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    @PUT("groups/{groupId}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Response<Unit>
}

/**
 * 联系人API接口
 */
interface ContactApi {
    @GET("contacts")
    suspend fun getContacts(): Response<List<ContactDto>>

    @POST("contacts")
    suspend fun addContact(@Body contact: ContactDto): Response<ContactDto>

    @PUT("contacts/{contactId}")
    suspend fun updateContact(
        @Path("contactId") contactId: String,
        @Body contact: ContactDto
    ): Response<ContactDto>

    @DELETE("contacts/{contactId}")
    suspend fun deleteContact(@Path("contactId") contactId: String): Response<Unit>

    @POST("contacts/import")
    suspend fun importContacts(@Body contacts: List<ContactDto>): Response<List<ContactDto>>

    @POST("contacts/{contactId}/block")
    suspend fun blockContact(
        @Path("contactId") contactId: String,
        @Body body: Map<String, Boolean>
    ): Response<Unit>
}
