package com.qchat.server.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ApplicationConfigValue
import io.ktor.server.config.HoconConfig

/**
 * Application configuration loaded from application.conf
 */
object AppConfig {
    private val config = ConfigFactory.load()

    // Server configuration
    val serverHost: String = config.getString("app.host")
    val serverPort: Int = config.getInt("app.port")

    // Database configuration
    val databaseHost: String = config.getString("database.host")
    val databasePort: Int = config.getInt("database.port")
    val databaseName: String = config.getString("database.name")
    val databaseUsername: String = config.getString("database.username")
    val databasePassword: String = config.getString("database.password")

    val databaseUrl: String = "jdbc:postgresql://$databaseHost:$databasePort/$databaseName"

    // Redis configuration
    val redisHost: String = config.getString("redis.host")
    val redisPort: Int = config.getInt("redis.port")

    // JWT configuration
    val jwtSecret: String = config.getString("jwt.secret")
    val jwtExpiration: Long = config.getLong("jwt.expiration")
    val jwtIssuer: String = config.getString("jwt.issuer")
}
