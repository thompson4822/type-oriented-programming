package com.sthompson.service

import com.sthompson.domain.events.DomainEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Service responsible for publishing domain events to interested subscribers.
 * Uses CDI events for internal event distribution.
 */
@ApplicationScoped
class EventPublisher {
    @Inject
    lateinit var eventBus: Event<DomainEvent>
    
    private val logger = Logger.getLogger(EventPublisher::class.java)
    
    /**
     * Publish a domain event to all subscribers
     */
    fun publish(event: DomainEvent) {
        logger.debug("Publishing event: ${event.eventType} (${event.eventId})")
        eventBus.fire(event)
    }
    
    /**
     * Publish multiple domain events in order
     */
    fun publishAll(events: Collection<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
