package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import org.springframework.security.core.Authentication

class ServiceAuthentication(
    val user: User?,
    private val jwt: String): Authentication {

    private var valid: Boolean = true

    override fun getName() = user?.name
    override fun getAuthorities() = user?.authorities
    override fun getCredentials() = jwt
    override fun getDetails() = jwt
    override fun getPrincipal() = user
    override fun isAuthenticated() = user != null && valid
    override fun setAuthenticated(isAuthenticated: Boolean) { valid = isAuthenticated }

    constructor(jwt: String): this(null, jwt)


}