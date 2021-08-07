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
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.*

/**
 * Implements the functionality used to process users
 */
@Component
class UserDbService(@Autowired val userRepo: UserRepository,
                    @Autowired val sender: JmsTemplate,
                    @Autowired val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Returns all users
     * @return flux of users
     */
    fun getAllUsers(): Flux<User> {
        logger.debug("getAllUsers ")
        return userRepo.findAll()
    }

    /**
     * Returns a user by userId
     * @param userId
     * @return mono of a user
     */
    fun getUser(userId: UUID): Mono<User> {
        logger.debug("getUser $userId ")
        return userRepo.findById(userId).switchIfEmpty(Mono.error(ServiceException(HttpStatus.NOT_FOUND)))
    }

    /**
     * Creates a new user. Checks first if requester has the right permissions to do so.
     * Sends all necessary events.
     * @param requester
     * @param userDTO holding all necessary user info
     * @throws ServiceException when requester does not have right permissions or the dto is not complete.
     * @return new user
     */
    fun createUser(requester: User, userDTO: UserDTO): Mono<User> {
        logger.debug("createUser $requester $userDTO")
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty { Mono.error(ServiceException(HttpStatus.FORBIDDEN, "You have no permissions to create a user.")) }
            .filter { validateUserDTONotNull(userDTO) }
            .switchIfEmpty { Mono.error(ServiceException(HttpStatus.BAD_REQUEST, "Request Body was not complete")) }
            .map {

                User(userDTO) }
            .map { it.apply { password = passwordEncoder.encode(password) }            }
            .flatMap { userRepo.save(it) }
            .publishOn(Schedulers.boundedElastic()).map {
                sender.convertAndSend(
                    EventTopic.DataEvents.topic,
                    UserDataEvent(DataEventCode.CREATED, it.id!!)
                )
                it
            }
    }

    /**
     * Updates a user. Checks first if requester has the right permissions to do so.
     * Sends all necessary events.
     * @param requester
     * @param userId of the user that should be updates
     * @param userDTO holding all necessary user info
     * @throws ServiceException when requester does not have right permissions or the dto is not complete
     * or user does not exist.
     * @return updated user
     */
    fun updateUser(requester: User, userId: UUID, userDTO: UserDTO): Mono<User> {
        logger.debug("updateUser $requester $userId $userDTO ")
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty {
                Mono.error(
                        ServiceException(
                                HttpStatus.FORBIDDEN,
                                "You have no permissions to update a user."
                        )
                )
            }
            .filter { validateUserDTONotNull(userDTO) }
            .switchIfEmpty { Mono.error(ServiceException(HttpStatus.BAD_REQUEST, "Request Body was not complete")) }
            .flatMap { userRepo.findById(userId) }
            .switchIfEmpty { Mono.error(ServiceException(HttpStatus.NOT_FOUND)) }
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

    /**
     * Deletes a user. Checks first if requester has the right permissions to do so.
     * Sends all necessary events.
     * @param requester
     * @param userId of the user that should be deleted
     * @throws ServiceException when requester does not have right permissions or the dto is not complete
     * or user does not exist.
     * @return id of deleted user
     */
    fun deleteUser(requester: User, userId: UUID): Mono<UUID> {
        logger.debug("deleteUser $requester $userId ")
        return Mono.just(checkGlobalHardPermission(requester))
            .filter { it }
            .switchIfEmpty {
                Mono.error(
                        ServiceException(
                                HttpStatus.FORBIDDEN,
                                "You have no permissions to delete a user."
                        )
                )
            }
            .flatMap {
                userRepo.existsById(userId)
                    .filter { it }
                    .flatMap { userRepo.deleteById(userId).thenReturn(userId) }
                    .switchIfEmpty{ Mono.error(ServiceException(HttpStatus.NOT_FOUND, "User does not exist")) }

            }
            .publishOn(Schedulers.boundedElastic())
            .map {
                sender.convertAndSend(
                    EventTopic.DataEvents.topic,
                    UserDataEvent(DataEventCode.DELETED, userId)
                )
                it
            }
    }

    /**
     * Applies the userDTO on the user. Updates the values of the user.
     * Collects a list of events that need be be send after updating.
     * @return the updated user and all the events that need to be send.
     */
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

    /**
     * Checks if user fulfills hard permissions.
     * Hard permissions are fulfilled if user is a global admin
     * @param user
     * @return boolean
     */
    fun checkGlobalHardPermission(user: User): Boolean {
        logger.debug("checkGlobalHardPermission $user ")
        return user.globalRole == GlobalRole.ADMIN.name
    }

    /**
     * Validate that no value inside the userDTO is null
     * @param userDTO
     * @return boolean
     */
    fun validateUserDTONotNull(userDTO: UserDTO): Boolean {
        logger.debug("validateUserDTONotNull $userDTO ")
        return userDTO.dateOfBirth != null && userDTO.email != null && userDTO.globalRole != null
                && userDTO.lastName != null && userDTO.name != null && userDTO.password != null && userDTO.username != null
    }
}
