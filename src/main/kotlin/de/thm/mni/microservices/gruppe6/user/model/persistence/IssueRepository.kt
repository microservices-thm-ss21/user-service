package de.thm.mni.microservices.gruppe6.user.model.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import java.util.*

interface IssueRepository: ReactiveCrudRepository<Issue, UUID>
