package de.thm.mni.microservices.gruppe6.user.model.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface UserRepository: ReactiveCrudRepository<User, Long>
