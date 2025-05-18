package com.sthompson.service.listeners

import com.sthompson.domain.events.DomainEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.jboss.logging.Logger
import java.time.format.DateTimeFormatter

/**
 * Listener that logs all domain events to create an audit trail.
 * This demonstrates observing the base DomainEvent type to catch all events.
 */
@ApplicationScoped
class AuditEventListener {
    private val logger = Logger.getLogger(AuditEventListener::class.java)
    private val dateFormatter = DateTimeFormatter.ISO_INSTANT
    
    /**
     * Log all domain events to create an audit trail
     */
    fun onAnyDomainEvent(@Observes event: DomainEvent) {
        logger.info("AUDIT: [${event.eventType}] ID=${event.eventId}, Time=${dateFormatter.format(event.timestamp)}")
        
        // In a real application, you might:
        // 1. Store the event in a database
        // 2. Send it to an external audit system
        // 3. Create a structured log entry with all event details
    }
}
