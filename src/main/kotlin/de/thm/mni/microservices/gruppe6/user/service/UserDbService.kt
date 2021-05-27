package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.event.DataEventCode
import de.thm.mni.microservices.gruppe6.lib.event.IssueDataEvent
import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import de.thm.mni.microservices.gruppe6.user.model.message.UserDTO
import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

@Component
class UserDbService(@Autowired val userRepo: UserRepository, @Autowired val sender: JmsTemplate) {

    fun getAllUsers(): Flux<User> = userRepo.findAll()

    fun getUser(userId: UUID): Mono<User> {
        return userRepo.findById(userId)
    }

    fun createUser(userDTO: UserDTO): Mono<User> {
        return Mono.just(userDTO).map { User(it) }.flatMap { userRepo.save(it) }
            .publishOn(Schedulers.boundedElastic()).map {
                sender.convertAndSend(IssueDataEvent(DataEventCode.CREATED, it.id!!))
                it
            }
    }

    fun updateUser(userId: UUID, userDTO: UserDTO): Mono<User> {
        val user = userRepo.findById(userId)
        return user.flatMap { userRepo.save(it.applyUserDTO(userDTO)) }
    }

    fun deleteUser(userId: UUID): Mono<Void> {
        return userRepo.deleteById(userId)
    }

    fun User.applyUserDTO(userDTO: UserDTO): User {
        this.username = userDTO.username!!
        this.lastName = userDTO.lastName!!
        this.name = userDTO.name!!
        this.dateOfBirth = userDTO.dateOfBirth!!
        this.globalRole = userDTO.globalRole!!
        return this
    }

}
