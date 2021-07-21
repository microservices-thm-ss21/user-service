package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

@Configuration
class BasicAuthFilter(private val userDetailsService: ReactiveUserDetailsService,
                      private val jwtService: JwtService) {

    @Bean
    fun basicAuthenticationFilter(): AuthenticationWebFilter {
        val authManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
        val basicAuthenticationFilter = AuthenticationWebFilter(authManager)
        basicAuthenticationFilter.setAuthenticationSuccessHandler(basicAuthSuccessHandler())
        basicAuthenticationFilter.setRequiresAuthenticationMatcher(
            ServerWebExchangeMatchers.pathMatchers("/login")
        )
        return basicAuthenticationFilter
    }

    private fun basicAuthSuccessHandler(): ServerAuthenticationSuccessHandler {
        fun getHttpAuthHeaderValue(authentication: Authentication): String {
            val jwt = jwtService.createToken(authentication.principal as User)
            return "Bearer $jwt"
        }

        return ServerAuthenticationSuccessHandler {
                webFilterExchange, authentication ->
            val exchange = webFilterExchange.exchange
            exchange.response
                .headers
                .add(HttpHeaders.AUTHORIZATION, getHttpAuthHeaderValue(authentication))
            webFilterExchange.chain.filter(exchange)
        }
    }
}
