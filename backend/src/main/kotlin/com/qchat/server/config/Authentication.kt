package com.qchat.server.config

import com.qchat.server.security.JwtService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWT
import io.ktor.server.auth.jwt.jwt

/**
 * Configure JWT authentication
 */
fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JwtService.makeVerifier())
            realm = "QChat Server"
            validate { credential ->
                JwtService.validateToken(credential.payload)
            }
        }
    }
}
