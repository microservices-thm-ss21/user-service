package de.thm.mni.microservices.gruppe6.user.service

import de.thm.mni.microservices.gruppe6.lib.event.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class DataEventService(
    @Autowired val userDbService: UserDbService, // Included to maintain consistency
) {

    @Throws(IllegalStateException::class)
    fun processDataEvent(dataEvent: Mono<DataEvent>) {

        dataEvent.subscribe {
            when (it) {
                is UserDbService -> {/* Do nothing with own events */ }
                else -> error("Unexpected Event type: ${it?.javaClass}")
            }
        }
    }


}
