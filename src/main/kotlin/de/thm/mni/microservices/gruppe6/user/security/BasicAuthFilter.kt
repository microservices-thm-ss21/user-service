package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import java.util.*

@Configuration
class BasicAuthFilter(
    private val userDetailsService: ReactiveUserDetailsService,
    private val jwtService: JwtService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun basicAuthenticationFilter(): AuthenticationWebFilter {
        val authManager = UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
        val basicAuthenticationFilter = AuthenticationWebFilter(authManager)
        basicAuthenticationFilter.setAuthenticationFailureHandler { webFilterExchange, _ ->
            val exchange = webFilterExchange.exchange
            logger.debug(
                Base64.getDecoder().decode(exchange.request.headers[HttpHeaders.AUTHORIZATION]!!.first())
                    .contentToString()
            )
            webFilterExchange.chain.filter(exchange)
        }
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

        return ServerAuthenticationSuccessHandler { webFilterExchange, authentication ->
            val exchange = webFilterExchange.exchange
            exchange.response
                .headers
                .add(HttpHeaders.AUTHORIZATION, getHttpAuthHeaderValue(authentication))
            logger.debug(Base64.getDecoder().decode(exchange.request.headers[HttpHeaders.AUTHORIZATION]!!.first()).contentToString())
            webFilterExchange.chain.filter(exchange)
        }
    }
}
