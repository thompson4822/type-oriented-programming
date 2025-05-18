package com.sthompson.service

import com.sthompson.domain.events.DomainEvent
import com.sthompson.domain.events.PersonEvent
import com.sthompson.domain.events.SystemEvent
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
    // Generic event for all domain events
    @Inject
    lateinit var eventBus: Event<DomainEvent>

    // Specific event types for more targeted observation
    @Inject
    lateinit var personEvents: Event<PersonEvent>

    @Inject
    lateinit var systemEvents: Event<SystemEvent>

    private val logger = Logger.getLogger(EventPublisher::class.java)

    /**
     * Publish a domain event to all subscribers
     */
    fun publish(event: DomainEvent) {
        logger.debug("Publishing event: ${event.eventType} (${event.eventId})")

        // Fire the generic event for all subscribers
        eventBus.fire(event)

        // Also fire the specific event type for targeted subscribers
        when (event) {
            is PersonEvent -> personEvents.fire(event)
            is SystemEvent -> systemEvents.fire(event)
            // This branch should never be reached as all domain events should be either PersonEvent or SystemEvent
            else -> logger.warn("Unknown event type: ${event.javaClass.name}")
        }
    }

    /**
     * Publish multiple domain events in order
     */
    fun publishAll(events: Collection<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
