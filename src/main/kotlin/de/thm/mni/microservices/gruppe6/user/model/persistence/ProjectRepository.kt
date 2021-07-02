package de.thm.mni.microservices.gruppe6.user.model.persistence

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.*

interface ProjectRepository: ReactiveCrudRepository<Project, UUID> {
    @Query("INSERT INTO projects VALUES (:projectId)")
    fun saveProject(projectId: UUID) : Mono<Void>
}
