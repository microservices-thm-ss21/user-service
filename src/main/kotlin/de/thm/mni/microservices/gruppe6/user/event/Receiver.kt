package de.thm.mni.microservices.gruppe6.user.event

import de.thm.mni.microservices.gruppe6.lib.event.DataEvent
import de.thm.mni.microservices.gruppe6.lib.event.DomainEvent
import de.thm.mni.microservices.gruppe6.user.service.DataEventService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import javax.jms.Message
import javax.jms.ObjectMessage

@Component
class Receiver(private val dataEventService: DataEventService) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @JmsListener(destination = "microservices.events", containerFactory = "jmsListenerContainerFactory")
    fun receive(message: Message) {
        try {
            if (message !is ObjectMessage) {
                logger.error("Received unknown message type {} with id {}", message.jmsType, message.jmsMessageID)
                return
            }
            when (val payload = message.`object`) {
                is DataEvent -> {
                    logger.debug("Received DataEvent ObjectMessage with code {} and id {}", payload.code, payload.id)
                    dataEventService.processDataEvent(Mono.just(payload))
                }
                is DomainEvent -> {
                    logger.debug("Received DomainEvent Object Message with code {}", payload.code)
                    /** Do nothing for now / forever with domain events
                     * No use within issue service */
                }
                else -> {
                    logger.error(
                        "Received unknown ObjectMessage with payload type {} with id {}",
                        payload.javaClass,
                        message.jmsMessageID
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Receiver-Error", e)
        }
    }
}
