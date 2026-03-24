package com.qchat.backend.cache

/**
 * 缓存键定义
 */
object CacheKeys {

    // 用户相关
    fun userKey(userId: Long) = "user:$userId"
    fun userByPhoneKey(phoneNumber: String) = "user:phone:$phoneNumber"
    fun userByUsernameKey(username: String) = "user:username:$username"
    fun userOnlineKey(userId: Long) = "online:$userId"

    // 会话相关
    fun sessionKey(sessionId: Long) = "session:$sessionId"
    fun userSessionsKey(userId: Long) = "user:$userId:sessions"
    fun sessionMessagesKey(sessionId: Long) = "session:$sessionId:messages"

    // 消息相关
    fun messageKey(messageId: Long) = "message:$messageId"
    fun messageIdsKey(sessionId: Long, limit: Int, offset: Int) = "session:$sessionId:ids:$limit:$offset"

    // 群组相关
    fun groupKey(groupId: Long) = "group:$groupId"
    fun userGroupsKey(userId: Long) = "user:$userId:groups"
    fun groupMembersKey(groupId: Long) = "group:$groupId:members"

    // 联系人相关
    fun contactKey(contactId: Long) = "contact:$contactId"
    fun userContactsKey(userId: Long) = "user:$userId:contacts"

    // 设备相关
    fun deviceKey(deviceId: Long) = "device:$deviceId"
    fun userDevicesKey(userId: Long) = "user:$userId:devices"

    // WebSocket相关
    fun userWebSocketKey(userId: Long) = "ws:user:$userId"
    fun sessionTypingKey(sessionId: Long) = "typing:$sessionId"

    // 限流相关
    fun rateLimitKey(type: String, identifier: String) = "ratelimit:$type:$identifier"
}

/**
 * 缓存TTL定义（秒）
 */
object CacheTTL {
    const val USER = 3600L           // 1小时
    const val SESSION = 1800L       // 30分钟
    const val MESSAGE = 3600L       // 1小时
    const val GROUP = 1800L         // 30分钟
    const val CONTACT = 3600L      // 1小时
    const val DEVICE = 86400L       // 24小时
    const val ONLINE_STATUS = 300L  // 5分钟
    const val RATE_LIMIT = 60L      // 1分钟
    const val TYPING = 5L           // 5秒
}
