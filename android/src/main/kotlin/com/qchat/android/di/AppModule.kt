package com.qchat.android.di

import android.content.Context
import androidx.room.Room
import com.qchat.android.crypto.*
import com.qchat.android.data.local.QchatDatabase
import com.qchat.android.data.local.dao.*
import com.qchat.android.data.remote.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "https://api.qchat.com/"

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
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatApiService(retrofit: Retrofit): ChatApiService {
        return retrofit.create(ChatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageApiService(retrofit: Retrofit): MessageApiService {
        return retrofit.create(MessageApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QchatDatabase {
        return Room.databaseBuilder(
            context,
            QchatDatabase::class.java,
            QchatDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: QchatDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: QchatDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideChatParticipantDao(database: QchatDatabase): ChatParticipantDao {
        return database.chatParticipantDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: QchatDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun providePreKeyDao(database: QchatDatabase): PreKeyDao {
        return database.preKeyDao()
    }

    @Provides
    @Singleton
    fun provideSignedPreKeyDao(database: QchatDatabase): SignedPreKeyDao {
        return database.signedPreKeyDao()
    }

    @Provides
    @Singleton
    fun provideKeyStoreService(): KeyStoreService {
        return KeyStoreService()
    }

    @Provides
    @Singleton
    fun provideX3DHKeyAgreement(): X3DHKeyAgreement {
        return X3DHKeyAgreement()
    }

    @Provides
    @Singleton
    fun provideDoubleRatchet(): DoubleRatchet {
        return DoubleRatchet()
    }

    @Provides
    @Singleton
    fun provideAESEncryptionService(): AESEncryptionService {
        return AESEncryptionService()
    }

    @Provides
    @Singleton
    fun provideEd25519SignatureService(): Ed25519SignatureService {
        return Ed25519SignatureService()
    }

    @Provides
    @Singleton
    fun provideEncryptionServiceManager(
        keyStoreService: KeyStoreService,
        x3dh: X3DHKeyAgreement,
        doubleRatchet: DoubleRatchet,
        aesEncryption: AESEncryptionService,
        signatureService: Ed25519SignatureService
    ): EncryptionServiceManager {
        return EncryptionServiceManager(
            keyStoreService,
            x3dh,
            doubleRatchet,
            aesEncryption,
            signatureService
        )
    }

    @Provides
    @Singleton
    fun provideWebSocketService(okHttpClient: OkHttpClient): WebSocketService {
        return WebSocketService(okHttpClient)
    }
}
