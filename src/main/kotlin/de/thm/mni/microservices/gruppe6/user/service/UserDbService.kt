package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.event.*
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
                sender.convertAndSend(UserDataEvent(DataEventCode.CREATED, it.id!!))
                it
            }
    }

    fun updateUser(userId: UUID, userDTO: UserDTO): Mono<User> {
        return userRepo.findById(userId)
            .map { it.applyUserDTO(userDTO) }
            .map { userRepo.save(it.first)
                it
            }
            .publishOn(Schedulers.boundedElastic()).map {
                sender.convertAndSend(UserDataEvent(DataEventCode.UPDATED, userId))
                it.second.forEach(sender::convertAndSend)
                it.first
            }
    }

    fun deleteUser(userId: UUID): Mono<Void> {
        return userRepo.deleteById(userId)
            .publishOn(Schedulers.boundedElastic()).map {
                sender.convertAndSend(UserDataEvent(DataEventCode.DELETED, userId))
                it
            }
    }

    fun User.applyUserDTO(userDTO: UserDTO): Pair<User, List<DomainEvent>> {
        val eventList = ArrayList<DomainEvent>()

        if(this.username != userDTO.username!!){
            eventList.add(DomainEventChangedString(
                DomainEventCode.USER_CHANGED_USERNAME,
                this.id!!,
                this.username,
                userDTO.username))
            this.username = userDTO.username!!
        }

        if(this.lastName != userDTO.lastName!!){
            eventList.add(DomainEventChangedString(
                DomainEventCode.USER_CHANGED_LASTNAME,
                this.id!!,
                this.lastName,
                userDTO.lastName))
            this.lastName = userDTO.lastName!!
        }

        if(this.name != userDTO.name!!){
            eventList.add(DomainEventChangedString(
                DomainEventCode.USER_CHANGED_NAME,
                this.id!!,
                this.name,
                userDTO.name))
            this.name = userDTO.name!!
        }

        if(this.email != userDTO.email!!){
            eventList.add(DomainEventChangedString(
                DomainEventCode.USER_CHANGED_EMAIL,
                this.id!!,
                this.email,
                userDTO.email))
            this.email = userDTO.email!!
        }

        if(this.dateOfBirth != userDTO.dateOfBirth!!){
            eventList.add(DomainEventChangedDate(
                DomainEventCode.USER_CHANGED_DATEOFBIRTH,
                this.id!!,
                this.dateOfBirth,
                userDTO.dateOfBirth))
            this.dateOfBirth = userDTO.dateOfBirth!!
        }

        if(this.globalRole != userDTO.globalRole!!){
            eventList.add(DomainEventChangedString(
                DomainEventCode.USER_CHANGED_GLOBALROLE,
                this.id!!,
                this.globalRole,
                userDTO.globalRole))
            this.globalRole = userDTO.globalRole!!
        }

        return Pair(this, eventList)
    }

}
