package com.sthompson.domain.events

/**
 * Sealed class hierarchy for system-level events that aren't tied to a specific
 * domain entity but represent important system occurrences.
 */
sealed class SystemEvent(eventType: String) : BaseDomainEvent(eventType) {

    /**
     * Event fired when the application starts up
     */
    data class ApplicationStarted(
        val version: String,
        val environment: String
    ) : SystemEvent("system.started")

    /**
     * Event fired when the application is shutting down
     */
    data class ApplicationStopping(
        val reason: String
    ) : SystemEvent("system.stopping")

    /**
     * Event fired when a background job completes
     */
    sealed class JobCompleted(
        eventType: String,
        open val jobId: String,
        open val success: Boolean,
        open val message: String
    ) : SystemEvent(eventType) {

        /**
         * Event fired when a data import job completes
         */
        data class DataImportCompleted(
            override val jobId: String,
            override val success: Boolean,
            override val message: String,
            val recordsProcessed: Int,
            val recordsFailed: Int
        ) : JobCompleted("system.job.import.completed", jobId, success, message)

        /**
         * Event fired when a notification job completes
         */
        data class NotificationJobCompleted(
            override val jobId: String,
            override val success: Boolean,
            override val message: String,
            val notificationsSent: Int,
            val notificationsFailed: Int
        ) : JobCompleted("system.job.notification.completed", jobId, success, message)
    }
}
