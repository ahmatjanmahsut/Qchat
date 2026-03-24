package com.qchat.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import java.util.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWTVerifier.Builder("your-secret-key")
                .withIssuer("qchat")
                .build())
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
