package de.thm.mni.microservices.gruppe6.user.controller

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import de.thm.mni.microservices.gruppe6.lib.classes.userService.UserDTO
import de.thm.mni.microservices.gruppe6.lib.exception.ServiceException
import de.thm.mni.microservices.gruppe6.user.service.UserDbService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/users")
@CrossOrigin
class UserController(@Autowired val userService: UserDbService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // toDo: remove when jwt works
    val jwtUser = User(
        UUID.fromString("a443ffd0-f7a8-44f6-8ad3-87acd1e91042"),
        "Peter_Zwegat",
        "password",
        "Peter",
        "Zwegat",
        "peter.zwegat@mni.thm.de",
        LocalDate.now(),
        LocalDateTime.now(),
        "USER",
        null
    )

    @GetMapping("")
    fun getAllUsers(): Flux<User> = userService.getAllUsers()

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody userDTO: UserDTO): Mono<User> {
        logger.debug("createUser $userDTO")
        return userService.createUser(jwtUser, userDTO)
            .onErrorResume { return@onErrorResume Mono.error(ServiceException(HttpStatus.CONFLICT, cause = it)) }
    }

    @GetMapping("{userId}")
    fun getUser(@PathVariable userId: UUID): Mono<User> {
        logger.debug("getUser $userId")
        return userService.getUser(userId).switchIfEmpty(Mono.error(ServiceException(HttpStatus.NOT_FOUND)))
    }

    @PutMapping("/{userId}")
    fun updateUser(@PathVariable userId: UUID, @RequestBody userDTO: UserDTO): Mono<User> {
        logger.debug("updateUser $userId $userDTO")
        return userService.updateUser(jwtUser, userId, userDTO)
            .onErrorResume { return@onErrorResume Mono.error(ServiceException(HttpStatus.CONFLICT, cause = it)) }
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable userId: UUID): Mono<Void> {
        logger.debug("deleteUser $userId")
        return userService.deleteUser(jwtUser, userId)
    }
}
