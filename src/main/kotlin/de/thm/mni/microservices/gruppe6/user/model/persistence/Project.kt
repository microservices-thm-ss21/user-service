package de.thm.mni.microservices.gruppe6.user.model.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("projects")
data class Project(
    @Id var projectId: UUID,
)


