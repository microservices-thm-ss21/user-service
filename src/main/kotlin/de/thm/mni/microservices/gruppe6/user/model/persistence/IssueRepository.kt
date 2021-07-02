package de.thm.mni.microservices.gruppe6.user.model.persistence

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.util.*

interface IssueRepository: ReactiveCrudRepository<Issue, UUID> {

    @Query("INSERT INTO issues VALUES (:issueId)")
    fun saveIssue(issueId: UUID) : Mono<Void>
}
