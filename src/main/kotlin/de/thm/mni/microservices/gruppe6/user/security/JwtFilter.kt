package de.thm.mni.microservices.gruppe6.user.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.function.Predicate

@Component
class JwtFilter(private val jwtService: JwtService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun jwtFilter(): AuthenticationWebFilter {
        val authManager = jwtAuthenticationManager()
        val jwtFilter = AuthenticationWebFilter(authManager)
        jwtFilter.setRequiresAuthenticationMatcher(
            ServerWebExchangeMatchers.matchers(ServerWebExchangeMatcher {
                logger.info(it.request.uri.path)
                if (it.request.uri.path == "/login") {
                    ServerWebExchangeMatcher.MatchResult.notMatch()
                } else {
                    ServerWebExchangeMatcher.MatchResult.match()
                }
            })
        )

        jwtFilter.setServerAuthenticationConverter(JWTAuthenticationConverter())
        return jwtFilter
    }

    fun jwtAuthenticationManager() = ReactiveAuthenticationManager { auth ->
        Mono.create {
            val jwt = auth.credentials as String
            val user = jwtService.authorize(jwt)
            it.success(ServiceAuthentication(user, jwt))
        }
    }

    class JWTAuthenticationConverter: ServerAuthenticationConverter {
        private val bearer = "Bearer "
        private val matchBearerLength = Predicate { authValue: String -> authValue.length > bearer.length }
        private fun isolateBearerValue(authValue: String) = Mono.just(
            authValue.substring(bearer.length)
        )

        private fun extract(serverWebExchange: ServerWebExchange): Mono<String> {
            return Mono.justOrEmpty(
                serverWebExchange.request
                    .headers
                    .getFirst(HttpHeaders.AUTHORIZATION)
            )
        }

        private fun createAuthenticationObject(jwt: String): Mono<ServiceAuthentication> {
            return Mono.create {
                it.success(ServiceAuthentication(jwt))
            }
        }

        override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
            return exchange.toMono()
                .flatMap(this::extract)
                .filter(matchBearerLength)
                .flatMap(this::isolateBearerValue)
                .flatMap(this::createAuthenticationObject)
        }
    }
}

