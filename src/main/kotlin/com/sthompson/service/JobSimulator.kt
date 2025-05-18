package com.sthompson.service

import com.sthompson.domain.events.SystemEvent
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * Simulates background jobs to demonstrate system events.
 * Uses Quarkus scheduler to periodically run jobs.
 */
@ApplicationScoped
class JobSimulator {
    @Inject
    lateinit var eventPublisher: EventPublisher
    
    private val logger = Logger.getLogger(JobSimulator::class.java)
    
    /**
     * Simulate a data import job every 5 minutes
     */
    @Scheduled(every = "5m")
    fun simulateDataImport() {
        logger.info("Starting simulated data import job")
        
        // Simulate job execution
        val jobId = UUID.randomUUID().toString()
        val recordsProcessed = ThreadLocalRandom.current().nextInt(100, 1000)
        val recordsFailed = ThreadLocalRandom.current().nextInt(0, 10)
        val success = recordsFailed < 5
        
        // Publish job completion event
        val event = SystemEvent.JobCompleted.DataImportCompleted(
            jobId = jobId,
            success = success,
            message = if (success) "Import completed successfully" else "Import completed with errors",
            recordsProcessed = recordsProcessed,
            recordsFailed = recordsFailed
        )
        
        eventPublisher.publish(event)
    }
    
    /**
     * Simulate a notification job every 15 minutes
     */
    @Scheduled(every = "15m")
    fun simulateNotificationJob() {
        logger.info("Starting simulated notification job")
        
        // Simulate job execution
        val jobId = UUID.randomUUID().toString()
        val notificationsSent = ThreadLocalRandom.current().nextInt(10, 100)
        val notificationsFailed = ThreadLocalRandom.current().nextInt(0, 5)
        val success = notificationsFailed < 3
        
        // Publish job completion event
        val event = SystemEvent.JobCompleted.NotificationJobCompleted(
            jobId = jobId,
            success = success,
            message = if (success) "Notifications sent successfully" else "Some notifications failed",
            notificationsSent = notificationsSent,
            notificationsFailed = notificationsFailed
        )
        
        eventPublisher.publish(event)
    }
}
