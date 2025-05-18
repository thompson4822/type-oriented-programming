package com.sthompson.service

import com.sthompson.domain.events.SystemEvent
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

/**
 * Handles application lifecycle events and publishes corresponding domain events.
 */
@ApplicationScoped
class ApplicationLifecycle {
    @Inject
    lateinit var eventPublisher: EventPublisher
    
    @ConfigProperty(name = "quarkus.application.version", defaultValue = "unknown")
    lateinit var appVersion: String
    
    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    lateinit var environment: String
    
    private val logger = Logger.getLogger(ApplicationLifecycle::class.java)
    
    /**
     * Handle Quarkus startup event and publish our domain event
     */
    fun onStart(@Observes event: StartupEvent) {
        logger.info("Application starting...")
        
        // Publish our domain event
        val startedEvent = SystemEvent.ApplicationStarted(
            version = appVersion,
            environment = environment
        )
        
        eventPublisher.publish(startedEvent)
    }
    
    /**
     * Handle Quarkus shutdown event and publish our domain event
     */
    fun onStop(@Observes event: ShutdownEvent) {
        logger.info("Application stopping...")
        
        // Publish our domain event
        val stoppingEvent = SystemEvent.ApplicationStopping(
            reason = "Normal shutdown"
        )
        
        eventPublisher.publish(stoppingEvent)
    }
}
