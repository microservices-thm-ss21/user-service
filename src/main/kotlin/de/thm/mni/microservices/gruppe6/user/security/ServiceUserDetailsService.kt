package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ServiceUserDetailsService(private val userRepository: UserRepository):
    ReactiveUserDetailsService, ReactiveUserDetailsPasswordService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        return this.userRepository.findByUsername(username)
    }

    override fun updatePassword(userDetails: UserDetails, newPassword: String): Mono<UserDetails> {
        val user = userDetails as User
        user.password = newPassword
        return userRepository.save(user).cast(UserDetails::class.java)
    }
}