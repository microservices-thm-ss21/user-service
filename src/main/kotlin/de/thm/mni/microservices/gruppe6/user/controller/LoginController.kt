package de.thm.mni.microservices.gruppe6.user.controller

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class LoginController {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/login")
    fun login(auth: Authentication): Mono<Void> {
        logger.info("${(auth.principal as User).name} logged in with password ${auth.credentials as String}!")
        return Mono.empty()
    }

}