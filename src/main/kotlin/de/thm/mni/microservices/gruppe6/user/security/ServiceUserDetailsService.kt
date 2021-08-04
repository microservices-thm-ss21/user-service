package de.thm.mni.microservices.gruppe6.user.security

import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ServiceUserDetailsService(private val userRepository: UserRepository) :
    ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        return this.userRepository.findByUsername(username)
    }
}
