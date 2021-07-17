package de.thm.mni.microservices.gruppe6.user.model.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
interface UserRepository: ReactiveCrudRepository<User, UUID> {

    fun findByUsername(username: String): Mono<UserDetails>

}
