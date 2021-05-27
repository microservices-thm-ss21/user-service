package de.thm.mni.microservices.gruppe6.user.controller

import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import de.thm.mni.microservices.gruppe6.user.model.message.UserDTO
import de.thm.mni.microservices.gruppe6.user.service.UserDbService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/users")
class UserController(@Autowired val userService: UserDbService) {

    @GetMapping("")
    fun getAllUsers(): Flux<User> = userService.getAllUsers()

    @PostMapping("")
    fun putUser(@RequestBody userDTO: UserDTO): Mono<User> = userService.createUser(userDTO)

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: UUID, @RequestBody userDTO: UserDTO) = userService.updateUser(id, userDTO)

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: UUID) = userService.deleteUser(id)
}
