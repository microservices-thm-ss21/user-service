package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.classes.userService.GlobalRole
import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import de.thm.mni.microservices.gruppe6.lib.classes.userService.UserDTO
import de.thm.mni.microservices.gruppe6.lib.event.*
import de.thm.mni.microservices.gruppe6.lib.exception.ServiceException
import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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

    fun createUser(requester: User, userDTO: UserDTO): Mono<User> {
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty(Mono.error(ServiceException(HttpStatus.FORBIDDEN, "You have no permissions to create a user.")))
            .map { User(userDTO) }
            .flatMap { userRepo.save(it) }
            .publishOn(Schedulers.boundedElastic()).map {
                sender.convertAndSend(
                    EventTopic.DataEvents.topic,
                    UserDataEvent(DataEventCode.CREATED, it.id!!)
                )
                it
            }
    }

    fun updateUser(requester: User, userId: UUID, userDTO: UserDTO): Mono<User> {
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty(Mono.error(ServiceException(HttpStatus.FORBIDDEN, "You have no permissions to create a user.")))
            .flatMap { userRepo.findById(userId) }
            .map { it.applyUserDTO(userDTO) }
            .flatMap {
                userRepo.save(it.first).map { user ->
                    Pair(user, it.second)
                }
            }
            .publishOn(Schedulers.boundedElastic()).map {
                sender.convertAndSend(
                    EventTopic.DataEvents.topic,
                    UserDataEvent(DataEventCode.UPDATED, userId)
                )
                it.second.forEach { (topic, event) -> sender.convertAndSend(topic, event) }
                it.first
            }
    }

    fun deleteUser(requester: User, userId: UUID): Mono<Void> {
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty(Mono.error(ServiceException(HttpStatus.FORBIDDEN, "You have no permissions to create a user.")))
            .flatMap { userRepo.deleteById(userId)}
            .publishOn(Schedulers.boundedElastic())
            .map {
                sender.convertAndSend(
                    EventTopic.DataEvents.topic,
                    UserDataEvent(DataEventCode.DELETED, userId)
                )
                it
            }
    }

    fun User.applyUserDTO(userDTO: UserDTO): Pair<User, List<Pair<String, DomainEvent>>> {
        val eventList = ArrayList<Pair<String, DomainEvent>>()

        if (this.username != userDTO.username!!) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedString(
                        DomainEventCode.USER_CHANGED_USERNAME,
                        this.id!!,
                        this.username,
                        userDTO.username
                    )
                )
            )
            this.username = userDTO.username!!
        }

        if (this.lastName != userDTO.lastName!!) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedString(
                        DomainEventCode.USER_CHANGED_LASTNAME,
                        this.id!!,
                        this.lastName,
                        userDTO.lastName
                    )
                )
            )
            this.lastName = userDTO.lastName!!
        }

        if (this.name != userDTO.name!!) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedString(
                        DomainEventCode.USER_CHANGED_NAME,
                        this.id!!,
                        this.name,
                        userDTO.name
                    )
                )
            )
            this.name = userDTO.name!!
        }

        if (this.email != userDTO.email!!) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedString(
                        DomainEventCode.USER_CHANGED_EMAIL,
                        this.id!!,
                        this.email,
                        userDTO.email
                    )
                )
            )
            this.email = userDTO.email!!
        }

        if (this.dateOfBirth != userDTO.dateOfBirth!!) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedDate(
                        DomainEventCode.USER_CHANGED_DATEOFBIRTH,
                        this.id!!,
                        this.dateOfBirth,
                        userDTO.dateOfBirth
                    )
                )
            )
            this.dateOfBirth = userDTO.dateOfBirth!!
        }

        if (this.globalRole != userDTO.globalRole!!) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedString(
                        DomainEventCode.USER_CHANGED_GLOBALROLE,
                        this.id!!,
                        this.globalRole,
                        userDTO.globalRole
                    )
                )
            )
            this.globalRole = userDTO.globalRole!!
        }

        return Pair(this, eventList)
    }

    fun checkGlobalHardPermission(user: User): Boolean {
        return user.globalRole == GlobalRole.ADMIN.name
    }

}
