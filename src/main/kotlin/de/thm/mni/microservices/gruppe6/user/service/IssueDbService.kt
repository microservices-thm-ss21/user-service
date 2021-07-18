package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.classes.userService.User
import de.thm.mni.microservices.gruppe6.lib.event.DataEventCode.*
import de.thm.mni.microservices.gruppe6.lib.event.IssueDataEvent
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
            CREATED -> issueRepo.saveIssue(issueDataEvent.id).subscribe()
            DELETED -> issueRepo.deleteById(issueDataEvent.id).subscribe()
            UPDATED -> {}
            else -> throw IllegalArgumentException("Unexpected code for issueDataEvent: ${issueDataEvent.code}")
        }
    }
}
