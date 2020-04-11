package net.sunaba.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.SignatureVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.impl.ClaimsHolder
import java.nio.charset.Charset
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


internal class UserSessionSerializerTest {
    @Test
    fun serialize_deserialize() {
        val alg = Algorithm.HMAC256("my-special-secret")
        val serializer = UserSessionSerializer(alg, "test-issuer")
        val user = User("12345", "test@example.com")
        val token = serializer.serialize(user)
        val deserializeUser = serializer.deserialize(token)
        assertEquals(user, deserializeUser)
    }

    @Test
    fun token_expiration() {
        assertFailsWith<TokenExpiredException>("JWT有効期限切れテスト") {
            val alg = Algorithm.HMAC256("my-special-secret")
            val serializer = UserSessionSerializer(alg, "test-issuer", maxAgeInSeconds = -1)
            val user = User("12345", "test@example.com")
            val token = serializer.serialize(user)
            serializer.deserialize(token)
        }
    }

    @Test
    fun verification_exception() {
        assertFailsWith<SignatureVerificationException>("JWT改ざんテスト") {
            val alg = Algorithm.HMAC256("my-special-secret")
            val serializer = UserSessionSerializer(alg, "test-issuer")
            val user = User("12345", "test@example.com")
            val token = serializer.serialize(user)

            val parts = token.split(".")
            val payload = Base64.getDecoder().decode(parts[1]).toString(Charsets.UTF_8)
            assertTrue(payload.contains("test@example.com"))

            //
            val newToken = payload.replace("test@example.com", "foo@example.com").let {
                arrayOf(parts[0], Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)), parts[2]).joinToString(".")
            }
            val decodedJwt = JWT.decode(newToken)
            assertEquals("foo@example.com", decodedJwt.claims["email"]!!.asString())

            serializer.deserialize(newToken)
        }
    }
}