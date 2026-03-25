plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.ktor.plugin") version "2.3.6"
    application
}

group = "com.qchat.backend"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.6")
    implementation("io.ktor:ktor-server-netty:2.3.6")
    implementation("io.ktor:ktor-server-websockets:2.3.6")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
    implementation("io.ktor:ktor-server-cors:2.3.6")
    implementation("io.ktor:ktor-server-auth:2.3.6")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.6")
    implementation("io.ktor:ktor-server-call-logging:2.3.6")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")

    // Exposed ORM (PostgreSQL)
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.47.0")

    // PostgreSQL Driver
    implementation("org.postgresql:postgresql:42.7.1")

    // HikariCP for connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Redis
    implementation("redis.clients:jedis:5.1.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Logback
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Configuration
    implementation("com.typesafe:config:1.4.3")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:2.3.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
}

application {
    mainClass.set("com.qchat.server.ApplicationKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
