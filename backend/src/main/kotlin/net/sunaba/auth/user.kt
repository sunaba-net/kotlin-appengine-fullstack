package net.sunaba.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.sessions.SessionSerializer
import java.util.*

data class User(val id: String, val email: String, var admin: Boolean = false)

class UserSessionSerializer(private val algorithm: Algorithm, private val issuer: String = "App Issuer", private val maxAgeInSeconds: Long = 60 * 60) : SessionSerializer<User> {
    override fun deserialize(text: String): User {
        val jwt = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(text)

        fun str(key: String) = jwt.claims[key]!!.asString()
        fun bool(key: String) = jwt.claims[key]!!.asBoolean()
        return User(jwt.subject, str("email"), bool("admin"))
    }

    override fun serialize(user: User): String {
        val now = Date()
        return JWT.create().withIssuer(issuer)
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(Date(now.time + maxAgeInSeconds * 1000))
                .withSubject(user.id)
                .withClaim("email", user.email)
                .withClaim("admin", user.admin)
                .sign(algorithm)
    }
}

