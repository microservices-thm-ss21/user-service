package de.thm.mni.microservices.gruppe6.user.controller

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import de.thm.mni.microservices.gruppe6.lib.classes.userService.UserDTO
import de.thm.mni.microservices.gruppe6.lib.exception.coverUnexpectedException
import de.thm.mni.microservices.gruppe6.user.security.ServiceAuthentication
import de.thm.mni.microservices.gruppe6.user.service.UserDbService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/users")
@CrossOrigin
class UserController(@Autowired val userService: UserDbService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("")
    fun getAllUsers(): Flux<User> = userService.getAllUsers().onErrorResume { Mono.error(coverUnexpectedException(it)) }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody userDTO: UserDTO, auth: ServiceAuthentication): Mono<User> {
        logger.debug("createUser $userDTO")
        return userService.createUser(auth.user!!, userDTO)
            .onErrorResume { Mono.error(coverUnexpectedException(it)) }
    }

    @GetMapping("{userId}")
    fun getUser(@PathVariable userId: UUID): Mono<User> {
        logger.debug("getUser $userId")
        return userService.getUser(userId)
    }

    @PutMapping("/{userId}")
    fun updateUser(@PathVariable userId: UUID, @RequestBody userDTO: UserDTO, auth: ServiceAuthentication): Mono<User> {
        logger.debug("updateUser $userId $userDTO")
        return userService.updateUser(auth.user!!, userId, userDTO)
            .onErrorResume { Mono.error(coverUnexpectedException(it)) }
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable userId: UUID, auth: ServiceAuthentication): Mono<Void> {
        logger.debug("deleteUser $userId")
        return userService.deleteUser(auth.user!!, userId)
            .onErrorResume { Mono.error(coverUnexpectedException(it)) }
            .flatMap { Mono.empty() }
    }
}
