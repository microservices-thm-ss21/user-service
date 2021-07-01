package de.thm.mni.microservices.gruppe6.user.controller

import de.thm.mni.microservices.gruppe6.lib.exception.ServiceException
import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import de.thm.mni.microservices.gruppe6.user.model.message.UserDTO
import de.thm.mni.microservices.gruppe6.user.service.UserDbService
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

    @GetMapping("")
    fun getAllUsers(): Flux<User> = userService.getAllUsers()

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody userDTO: UserDTO): Mono<User> = userService.createUser(userDTO)
        .onErrorResume { Mono.error(ServiceException(HttpStatus.CONFLICT, cause = it)) }

    @GetMapping("{userId}")
    fun getUser(@PathVariable userId: UUID): Mono<User> =
        userService.getUser(userId).switchIfEmpty(Mono.error(ServiceException(HttpStatus.NOT_FOUND)))

    @PutMapping("/{userId}")
    fun updateUser(@PathVariable userId: UUID, @RequestBody userDTO: UserDTO): Mono<User> = userService.updateUser(userId, userDTO)
        .onErrorResume { Mono.error(ServiceException(HttpStatus.CONFLICT, cause = it)) }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable userId: UUID) = userService.deleteUser(userId)
}
