package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*


@Component
class JwtService(private val jwtProperties: JwtProperties) {

    private val key: Key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun authorize(jwt: String): User? {
        return try {
            val claims: Claims =
                Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireSubject(jwtProperties.jwtSubject)
                    .build()
                    .parseClaimsJws(jwt)
                    .body
            User(claims)
        } catch (e: Exception) {
            logger.error("Jwt handling exception!", e)
            null
        }
    }

    fun createToken(user: User): String {
        return Jwts.builder()
            .setSubject(jwtProperties.jwtSubject)
            .addClaims(user.getJwtClaims())
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + (1000 * jwtProperties.expiration)))
            .signWith(key)
            .compact()
    }

}
