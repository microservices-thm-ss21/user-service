package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*


@Component
class ClassroomJWTService(private val jwtProperties: JWTProperties) {

    private val key: Key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    fun authorize(jwt: String): User {
        val claims: Claims =
            Jwts.parserBuilder()
                .setSigningKey(key)
                .requireSubject(jwtProperties.jwtSubject)
                .build()
                .parseClaimsJws(jwt)
                .body
        return User(claims)
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