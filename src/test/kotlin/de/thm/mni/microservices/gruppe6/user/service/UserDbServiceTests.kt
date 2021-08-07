package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.classes.userService.GlobalRole
import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import de.thm.mni.microservices.gruppe6.lib.classes.userService.UserDTO
import de.thm.mni.microservices.gruppe6.lib.exception.ServiceException
import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.jms.core.JmsTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserDbServiceTests(
    @Mock private val userRepository: UserRepository,
    @Mock private val sender: JmsTemplate
) {

    private val passwordEncoder = BCryptPasswordEncoder()
    private val userService = UserDbService(userRepository, sender, passwordEncoder)

    private fun createUser(username: String, password: String, name: String, lastName: String, email: String, globalRole: GlobalRole): User {
        return User(UUID.randomUUID(), username, password, name, lastName, email, LocalDate.now(), LocalDateTime.now(), globalRole.name, LocalDateTime.now())
    }

    private fun createUserDTO(username: String?, password: String?, name: String?, lastName: String?, email: String?, globalRole: GlobalRole?): UserDTO {
        val userDTO = UserDTO()
        userDTO.username = username
        userDTO.password = password
        userDTO.name = name
        userDTO.lastName = lastName
        userDTO.email = email
        userDTO.dateOfBirth = LocalDate.now()
        userDTO.globalRole = globalRole
        return userDTO
    }

    @Test
    fun testShouldReturnEmptyListOfUsers() {
        given(userRepository.findAll()).willReturn(Flux.fromIterable(emptyList()))
        val users: List<User>? = userService.getAllUsers().collectList().block()

        assertThat(users).isNotNull
        assertThat(users).isEmpty()
        assertThat(users).isEqualTo(emptyList<User>())

        verify(userRepository, times(1)).findAll()
    }

    @Test
    fun testShouldReturnOneUser() {
        val user = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        given(userRepository.findAll()).willReturn(Flux.fromIterable(listOf(user)))
        val users: List<User>? = userService.getAllUsers().collectList().block()

        assertThat(users).isNotNull
        assertThat(users).hasSize(1)
        assertThat(users!![0]).isEqualTo(user)

        verify(userRepository, times(1)).findAll()
    }

    @Test
    fun testShouldReturnUsers() {
        val user1 = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val user2 = createUser("Maxine_Musterfrau", "p2", "Maxine", "Musterfrau", "MaxineMusterfrau@mail.com", GlobalRole.ADMIN)
        val user3 = createUser("Hänno", "p3", "Heinrich", "Heidenthaler", "haenni123@mail.com", GlobalRole.USER)
        val userList = listOf(user1, user2, user3)

        given(userRepository.findAll()).willReturn(Flux.fromIterable(userList))
        val users: List<User>? = userService.getAllUsers().collectList().block()

        assertThat(users).isNotNull
        assertThat(users).hasSize(3)
        users!!.forEachIndexed { index, user -> assertThat(user).isEqualTo(userList[index]) }

        verify(userRepository, times(1)).findAll()
    }

    @Test
    fun testShouldReturnUser() {
        val user = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        given(userRepository.findById(user.id!!)).willReturn(Mono.just(user))
        val userReturned: User? = userService.getUser(user.id!!).block()

        assertThat(userReturned).isNotNull
        assertThat(userReturned).isEqualTo(user)

        verify(userRepository, times(1)).findById(user.id!!)
    }

    @Test
    fun testShouldNotReturnUser() {
        val userId = UUID.randomUUID()
        given(userRepository.findById(userId)).willReturn(Mono.error(ServiceException(HttpStatus.NOT_FOUND)))

        var error: Throwable? = null
        try {
            userService.getUser(userId).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.NOT_FOUND)

        verify(userRepository, times(1)).findById(userId)
    }

    @Test
    fun testShouldCreateUser() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userToCreate = createUserDTO(
                "TestUser",
                "password",
                "Test",
                "User",
                "testuser@mail.com",
                GlobalRole.USER,
        )

        val userToBeReturned = User(userToCreate)
        userToBeReturned.id = UUID.randomUUID()

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        doReturn(true).`when`(service).validateUserDTONotNull(userToCreate)
        given(userRepository.save(any())).willReturn(Mono.just(userToBeReturned))

        val returnedUser: User? = service.createUser(requester, userToCreate).block()

        assertThat(returnedUser).isNotNull
        assertThat(returnedUser).isEqualTo(userToBeReturned)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(1)).validateUserDTONotNull(userToCreate)
        verify(userRepository, times(1)).save(any())
    }

    @Test
    fun testShouldNotCreateUser1() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.USER)
        val userToCreate = createUserDTO(
                "TestUser",
                "password",
                "Test",
                "User",
                "testuser@mail.com",
                GlobalRole.USER,
        )

        doReturn(false).`when`(service).checkGlobalHardPermission(requester)

        var error: Throwable? = null
        try {
            service.createUser(requester, userToCreate).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.FORBIDDEN)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(0)).validateUserDTONotNull(userToCreate)
        verify(userRepository, times(0)).save(any())
    }

    @Test
    fun testShouldNotCreateUser2() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userToCreate = UserDTO()

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        doReturn(false).`when`(service).validateUserDTONotNull(userToCreate)

        var error: Throwable? = null
        try {
            service.createUser(requester, userToCreate).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.BAD_REQUEST)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(1)).validateUserDTONotNull(userToCreate)
        verify(userRepository, times(0)).save(any())
    }

    @Test
    fun testShouldUpdateUser() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()
        val userToUpdate = createUserDTO(
                "TestUser",
                "password",
                "Test",
                "User",
                "testuser@mail.com",
                GlobalRole.USER,
        )

        val userToBeReturned = User(userToUpdate)
        userToBeReturned.id = userId

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        doReturn(true).`when`(service).validateUserDTONotNull(userToUpdate)
        given(userRepository.findById(userId)).willReturn(Mono.just(userToBeReturned))
        given(userRepository.save(any())).willReturn(Mono.just(userToBeReturned))

        val returnedUser: User? = service.updateUser(requester, userId, userToUpdate).block()

        assertThat(returnedUser).isNotNull
        assertThat(returnedUser).isEqualTo(userToBeReturned)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(1)).validateUserDTONotNull(userToUpdate)
        verify(userRepository, times(1)).findById(userId)
        verify(userRepository, times(1)).save(any())
    }

    @Test
    fun testShouldNotUpdateUser1() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()
        val userToUpdate = createUserDTO(
                "TestUser",
                "password",
                "Test",
                "User",
                "testuser@mail.com",
                GlobalRole.USER,
        )

        doReturn(false).`when`(service).checkGlobalHardPermission(requester)

        var error: Throwable? = null
        try {
            service.updateUser(requester, userId, userToUpdate).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.FORBIDDEN)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(0)).validateUserDTONotNull(userToUpdate)
        verify(userRepository, times(0)).findById(userId)
        verify(userRepository, times(0)).save(any())
    }

    @Test
    fun testShouldNotUpdateUser2() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()
        val userToUpdate = createUserDTO(
                "TestUser",
                "password",
                "Test",
                "User",
                "testuser@mail.com",
                GlobalRole.USER,
        )

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        doReturn(false).`when`(service).validateUserDTONotNull(userToUpdate)

        var error: Throwable? = null
        try {
            service.updateUser(requester, userId, userToUpdate).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.BAD_REQUEST)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(1)).validateUserDTONotNull(userToUpdate)
        verify(userRepository, times(0)).findById(userId)
        verify(userRepository, times(0)).save(any())
    }

    @Test
    fun testShouldNotUpdateUser3() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()
        val userToUpdate = createUserDTO(
                "TestUser",
                "password",
                "Test",
                "User",
                "testuser@mail.com",
                GlobalRole.USER,
        )

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        doReturn(true).`when`(service).validateUserDTONotNull(userToUpdate)
        given(userRepository.findById(userId)).willReturn(Mono.empty())

        var error: Throwable? = null
        try {
            service.updateUser(requester, userId, userToUpdate).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.NOT_FOUND)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(service, times(1)).validateUserDTONotNull(userToUpdate)
        verify(userRepository, times(1)).findById(userId)
        verify(userRepository, times(0)).save(any())
    }

    @Test
    fun testShouldDeleteUser() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        given(userRepository.existsById(userId)).willReturn(Mono.just(true))
        given(userRepository.deleteById(userId)).willReturn(Mono.empty())

        val deletedUserId: UUID? = service.deleteUser(requester, userId).block()

        assertThat(deletedUserId).isNotNull
        assertThat(deletedUserId).isEqualTo(userId)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(userRepository, times(1)).existsById(userId)
        verify(userRepository, times(1)).deleteById(userId)
    }

    @Test
    fun testShouldNotDeleteUser1() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()

        doReturn(false).`when`(service).checkGlobalHardPermission(requester)

        var error: Throwable? = null
        try {
            service.deleteUser(requester, userId).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.FORBIDDEN)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(userRepository, times(0)).existsById(userId)
        verify(userRepository, times(0)).deleteById(userId)
    }

    @Test
    fun testShouldNotDeleteUser2() {
        val service = spy(userService)
        val requester = createUser("Max_Mustermann", "p1", "Max", "Mustermann", "MaxMustermann@mail.com", GlobalRole.ADMIN)
        val userId = UUID.randomUUID()

        doReturn(true).`when`(service).checkGlobalHardPermission(requester)
        given(userRepository.existsById(userId)).willReturn(Mono.just(false))

        var error: Throwable? = null
        try {
            service.deleteUser(requester, userId).block()
        } catch (e: Throwable) {
            error = e
        }
        assertThat(error).isNotNull
        assertThat(error is ServiceException)
        assertThat((error as ServiceException).status == HttpStatus.NOT_FOUND)

        verify(service, times(1)).checkGlobalHardPermission(requester)
        verify(userRepository, times(1)).existsById(userId)
        verify(userRepository, times(0)).deleteById(userId)
    }

}
