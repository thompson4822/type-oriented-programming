package com.sthompson.service.listeners

import com.sthompson.domain.events.SystemEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.jboss.logging.Logger

/**
 * Listener for system-level events.
 * This demonstrates handling different types of system events.
 */
@ApplicationScoped
class SystemEventListener {
    private val logger = Logger.getLogger(SystemEventListener::class.java)
    
    /**
     * Handle application startup events
     */
    fun onApplicationStarted(@Observes event: SystemEvent.ApplicationStarted) {
        logger.info("Application started: version=${event.version}, environment=${event.environment}")
        
        // In a real application, you might:
        // 1. Initialize caches
        // 2. Check for system health
        // 3. Register with service discovery
    }
    
    /**
     * Handle application shutdown events
     */
    fun onApplicationStopping(@Observes event: SystemEvent.ApplicationStopping) {
        logger.info("Application stopping: reason=${event.reason}")
        
        // In a real application, you might:
        // 1. Flush caches
        // 2. Close connections
        // 3. Perform cleanup tasks
    }
    
    /**
     * Handle job completion events
     */
    fun onJobCompleted(@Observes event: SystemEvent.JobCompleted) {
        logger.info("Job completed: id=${event.jobId}, success=${event.success}, message=${event.message}")
        
        when (event) {
            is SystemEvent.JobCompleted.DataImportCompleted -> {
                logger.info("Data import job: processed=${event.recordsProcessed}, failed=${event.recordsFailed}")
                // In a real app: Update import statistics, notify admins of large failures
            }
            is SystemEvent.JobCompleted.NotificationJobCompleted -> {
                logger.info("Notification job: sent=${event.notificationsSent}, failed=${event.notificationsFailed}")
                // In a real app: Retry failed notifications, update delivery statistics
            }
        }
    }
}
