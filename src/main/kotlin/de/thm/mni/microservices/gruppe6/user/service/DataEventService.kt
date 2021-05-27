package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.event.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class DataEventService(
    @Autowired val issueDbService: IssueDbService,
    @Autowired val projectDbService: ProjectDbService,
    @Autowired val userDbService: UserDbService, // Included to maintain consistency
) {

    @Throws(IllegalStateException::class)
    fun processDataEvent(dataEvent: Mono<DataEvent>) {

        dataEvent.subscribe {
            when (it) {
                is IssueDataEvent -> issueDbService.receiveUpdate(it)
                is ProjectDataEvent -> projectDbService.receiveUpdate(it)
                is UserDataEvent -> {/* Do nothing with own events */ }
                else -> error("Unexpected Event type: ${it?.javaClass}")
            }
        }
    }


}
