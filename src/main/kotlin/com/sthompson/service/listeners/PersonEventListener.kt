package com.sthompson.service.listeners

import com.sthompson.domain.events.PersonEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.ObservesAsync
import org.jboss.logging.Logger

/**
 * Listener for person-related events.
 * This demonstrates observing specific event types and using pattern matching
 * with sealed classes to handle different event subtypes.
 */
@ApplicationScoped
class PersonEventListener {
    private val logger = Logger.getLogger(PersonEventListener::class.java)
    
    /**
     * Handle person created events
     * Using @Observes makes this a synchronous observer that runs in the same transaction
     */
    fun onPersonCreated(@Observes event: PersonEvent.PersonCreated) {
        logger.info("Person created: ID=${event.personId}, Name=${event.name}")
        
        // In a real application, you might:
        // 1. Send a welcome email
        // 2. Create default settings for the new person
        // 3. Initialize related resources
    }
    
    /**
     * Handle person updated events
     * Using @ObservesAsync makes this an asynchronous observer that runs in a separate thread
     */
    fun onPersonUpdated(@ObservesAsync event: PersonEvent.PersonUpdated) {
        logger.info("Person updated: ID=${event.personId}")
        
        if (event.previousEmail != event.newEmail) {
            logger.info("Email changed from ${event.previousEmail?.value} to ${event.newEmail?.value}")
            // In a real app: Send email change notification
        }
        
        if (event.previousPhone != event.newPhone) {
            logger.info("Phone changed from ${event.previousPhone?.value} to ${event.newPhone?.value}")
            // In a real app: Send phone change notification
        }
    }
    
    /**
     * Handle contact verification events
     * This demonstrates handling a sealed class hierarchy with when expression
     */
    fun onContactVerified(@Observes event: PersonEvent.ContactVerified) {
        when (event) {
            is PersonEvent.ContactVerified.EmailVerified -> {
                logger.info("Email verified: Person ID=${event.personId}, Email=${event.email.value}")
                // In a real app: Update verification status, send confirmation
            }
            is PersonEvent.ContactVerified.PhoneVerified -> {
                logger.info("Phone verified: Person ID=${event.personId}, Phone=${event.phone.value}")
                // In a real app: Update verification status, send confirmation
            }
        }
    }
}
