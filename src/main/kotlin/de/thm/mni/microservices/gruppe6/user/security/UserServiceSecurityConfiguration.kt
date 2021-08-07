package de.thm.mni.microservices.gruppe6.user.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter

@EnableWebFluxSecurity
@Configuration
class UserServiceSecurityConfiguration(
    private val jwtFilter: JwtFilter,
    private val basicAuthenticationFilter: AuthenticationWebFilter
) {


    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf().disable()
            .cors().disable()
            .logout().disable()
            .addFilterAt(basicAuthenticationFilter, SecurityWebFiltersOrder.HTTP_BASIC)
            .addFilterAt(jwtFilter.jwtFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange()
            .pathMatchers("/login")
            .authenticated()
            .and()
            .authorizeExchange()
            .pathMatchers("/api/*/admin/**")
            .hasAuthority("ADMIN")
            .and()
            .authorizeExchange()
            .pathMatchers("/api/**")
            .authenticated()
            .and()
            .build()
    }

    @Bean
    fun getPasswordEncoder(): PasswordEncoder {
        return DelegatingPasswordEncoder("bcrypt",
            mapOf(
                Pair(null, BCryptPasswordEncoder()),
                Pair("bcrypt", BCryptPasswordEncoder())
            )
        )
    }

}
