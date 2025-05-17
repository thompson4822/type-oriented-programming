package com.sthompson.domain.events

import java.time.Instant
import java.util.UUID

/**
 * Base sealed interface for all domain events in the system.
 * 
 * Domain events represent significant occurrences within the domain
 * that other parts of the system might be interested in.
 */
sealed interface DomainEvent {
    /**
     * Unique identifier for the event
     */
    val eventId: UUID
    
    /**
     * When the event occurred
     */
    val timestamp: Instant
    
    /**
     * The type of the event, used for routing and processing
     */
    val eventType: String
}

/**
 * Base class for domain events with common implementation details
 */
abstract class BaseDomainEvent(
    override val eventType: String
) : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val timestamp: Instant = Instant.now()
}
