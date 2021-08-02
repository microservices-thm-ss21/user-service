package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.classes.userService.GlobalRole
import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import de.thm.mni.microservices.gruppe6.lib.classes.userService.UserDTO
import de.thm.mni.microservices.gruppe6.lib.event.*
import de.thm.mni.microservices.gruppe6.lib.exception.ServiceException
import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getAllUsers(): Flux<User> {
        logger.debug("getAllUsers ")
        return userRepo.findAll()
    }

    fun getUser(userId: UUID): Mono<User> {
        logger.debug("getUser $userId ")
        return userRepo.findById(userId)
    }

    fun createUser(requester: User, userDTO: UserDTO): Mono<User> {
        logger.debug("createUser $requester $userDTO")
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty(
                Mono.error(ServiceException(HttpStatus.FORBIDDEN, "You have no permissions to create a user."))
            )
            .filter { validateUserDTONotNull(userDTO) }
            .switchIfEmpty(
                Mono.error(ServiceException(HttpStatus.BAD_REQUEST, "Request Body was not complete"))
            )
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
        logger.debug("updateUser $requester $userId $userDTO ")
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty(
                Mono.error(
                    ServiceException(
                        HttpStatus.FORBIDDEN,
                        "You have no permissions to create a user."
                    )
                )
            )
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
        logger.debug("deleteUser $requester $userId ")
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty(
                Mono.error(
                    ServiceException(
                        HttpStatus.FORBIDDEN,
                        "You have no permissions to create a user."
                    )
                )
            )
            .flatMap { userRepo.deleteById(userId) }
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

        if (userDTO.username != null && this.username != userDTO.username!!) {
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

        if (userDTO.lastName != null && this.lastName != userDTO.lastName!!) {
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

        if (userDTO.name != null && this.name != userDTO.name!!) {
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

        if (userDTO.email != null && this.email != userDTO.email!!) {
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

        if (userDTO.dateOfBirth != null && this.dateOfBirth != userDTO.dateOfBirth!!) {
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

        if (userDTO.globalRole != null && this.globalRole != userDTO.globalRole!!.name) {
            eventList.add(
                Pair(
                    EventTopic.DomainEvents_UserService.topic,
                    DomainEventChangedString(
                        DomainEventCode.USER_CHANGED_GLOBALROLE,
                        this.id!!,
                        this.globalRole,
                        userDTO.globalRole!!.name
                    )
                )
            )
            this.globalRole = userDTO.globalRole!!.name
        }

        return Pair(this, eventList)
    }

    fun checkGlobalHardPermission(user: User): Boolean {
        logger.debug("checkGlobalHardPermission $user ")
        return user.globalRole == GlobalRole.ADMIN.name
    }

    fun validateUserDTONotNull(userDTO: UserDTO): Boolean {
        logger.debug("validateUserDTONotNull $userDTO ")
        return userDTO.dateOfBirth != null && userDTO.email != null && userDTO.globalRole != null
                && userDTO.lastName != null && userDTO.name != null && userDTO.password != null && userDTO.username != null
    }
}
