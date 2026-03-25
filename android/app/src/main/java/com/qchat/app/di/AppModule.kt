package com.qchat.app.di

import android.content.Context
import androidx.room.Room
import com.qchat.app.data.local.dao.ContactDao
import com.qchat.app.data.local.dao.GroupDao
import com.qchat.app.data.local.dao.GroupMemberDao
import com.qchat.app.data.local.dao.MessageDao
import com.qchat.app.data.local.dao.SessionDao
import com.qchat.app.data.local.dao.UserDao
import com.qchat.app.data.local.database.QchatDatabase
import com.qchat.app.data.local.database.migration.ALL_MIGRATIONS
import com.qchat.app.data.remote.api.ContactApi
import com.qchat.app.data.remote.api.GroupApi
import com.qchat.app.data.remote.api.MessageApi
import com.qchat.app.data.remote.api.SessionApi
import com.qchat.app.data.remote.api.UserApi
import com.qchat.app.data.remote.config.ServerConfigManager
import com.qchat.app.data.repository.ContactRepository
import com.qchat.app.data.repository.ContactRepositoryImpl
import com.qchat.app.data.repository.GroupRepository
import com.qchat.app.data.repository.GroupRepositoryImpl
import com.qchat.app.data.repository.MessageRepository
import com.qchat.app.data.repository.MessageRepositoryImpl
import com.qchat.app.data.repository.SessionRepository
import com.qchat.app.data.repository.SessionRepositoryImpl
import com.qchat.app.data.repository.UserRepository
import com.qchat.app.data.repository.UserRepositoryImpl
import com.qchat.app.data.sync.ConflictResolver
import com.qchat.app.data.sync.LastWriteWinsConflictResolver
import com.qchat.app.data.sync.MessageConflictResolver
import com.qchat.app.data.sync.MessageHistorySyncer
import com.qchat.app.data.sync.MessageHistorySyncerImpl
import com.qchat.app.data.sync.OfflineSyncQueue
import com.qchat.app.data.sync.OfflineSyncQueueImpl
import com.qchat.app.data.sync.SyncService
import com.qchat.app.data.sync.SyncServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QchatDatabase {
        return Room.databaseBuilder(
            context,
            QchatDatabase::class.java,
            QchatDatabase.DATABASE_NAME
        )
            .addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: QchatDatabase): UserDao = database.userDao()

    @Provides
    fun provideSessionDao(database: QchatDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideMessageDao(database: QchatDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideGroupDao(database: QchatDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideGroupMemberDao(database: QchatDatabase): GroupMemberDao = database.groupMemberDao()

    @Provides
    fun provideContactDao(database: QchatDatabase): ContactDao = database.contactDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideServerConfigManager(@ApplicationContext context: Context): ServerConfigManager {
        return ServerConfigManager(context)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json, serverConfigManager: ServerConfigManager): Retrofit {
        return Retrofit.Builder()
            .baseUrl(serverConfigManager.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideSessionApi(retrofit: Retrofit): SessionApi = retrofit.create(SessionApi::class.java)

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi = retrofit.create(MessageApi::class.java)

    @Provides
    @Singleton
    fun provideGroupApi(retrofit: Retrofit): GroupApi = retrofit.create(GroupApi::class.java)

    @Provides
    @Singleton
    fun provideContactApi(retrofit: Retrofit): ContactApi = retrofit.create(ContactApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        userApi: UserApi
    ): UserRepository = UserRepositoryImpl(userDao, userApi)

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        sessionApi: SessionApi
    ): SessionRepository = SessionRepositoryImpl(sessionDao, sessionApi)

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageDao: MessageDao,
        sessionDao: SessionDao,
        messageApi: MessageApi
    ): MessageRepository = MessageRepositoryImpl(messageDao, sessionDao, messageApi)

    @Provides
    @Singleton
    fun provideGroupRepository(
        groupDao: GroupDao,
        groupMemberDao: GroupMemberDao,
        groupApi: GroupApi
    ): GroupRepository = GroupRepositoryImpl(groupDao, groupMemberDao, groupApi)

    @Provides
    @Singleton
    fun provideContactRepository(
        contactDao: ContactDao,
        contactApi: ContactApi
    ): ContactRepository = ContactRepositoryImpl(contactDao, contactApi)
}

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideConflictResolver(): ConflictResolver = LastWriteWinsConflictResolver()

    @Provides
    @Singleton
    fun provideMessageConflictResolver(
        conflictResolver: ConflictResolver
    ): MessageConflictResolver = MessageConflictResolver(conflictResolver)

    @Provides
    @Singleton
    fun provideOfflineSyncQueue(): OfflineSyncQueue = OfflineSyncQueueImpl()

    @Provides
    @Singleton
    fun provideMessageHistorySyncer(
        messageApi: MessageApi,
        messageDao: MessageDao,
        sessionDao: SessionDao,
        conflictResolver: MessageConflictResolver
    ): MessageHistorySyncer = MessageHistorySyncerImpl(messageApi, messageDao, sessionDao, conflictResolver)

    @Provides
    @Singleton
    fun provideSyncService(
        messageApi: MessageApi,
        sessionApi: SessionApi,
        userApi: UserApi,
        groupApi: GroupApi,
        contactApi: ContactApi
    ): SyncService = SyncServiceImpl(messageApi, sessionApi, userApi, groupApi, contactApi)
}
