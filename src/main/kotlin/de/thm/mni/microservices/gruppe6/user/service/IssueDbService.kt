package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.user.model.persistence.User
import de.thm.mni.microservices.gruppe6.user.model.persistence.UserRepository
import de.thm.mni.microservices.gruppe6.lib.event.DataEventCode.*
import de.thm.mni.microservices.gruppe6.lib.event.IssueDataEvent
import de.thm.mni.microservices.gruppe6.lib.event.UserDataEvent
import de.thm.mni.microservices.gruppe6.user.model.persistence.Issue
import de.thm.mni.microservices.gruppe6.user.model.persistence.IssueRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Component
class IssueDbService(@Autowired val issueRepo: IssueRepository) {

    fun getAllUsers(): Flux<Issue> = issueRepo.findAll()

    fun hasUser(userId: UUID): Mono<Boolean> = issueRepo.existsById(userId)

    fun receiveUpdate(issueDataEvent: IssueDataEvent) {
        when (issueDataEvent.code){
            CREATED -> issueRepo.save(Issue(issueDataEvent.id))
            DELETED -> issueRepo.deleteById(issueDataEvent.id)
            UPDATED -> {}
            else -> throw IllegalArgumentException("Unexpected code for issueDataEvent: ${issueDataEvent.code}")
        }
    }
}
