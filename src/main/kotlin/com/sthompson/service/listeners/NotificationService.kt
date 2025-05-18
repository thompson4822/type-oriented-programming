package com.sthompson.service.listeners

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.domain.events.PersonEvent
import com.sthompson.entity.Person
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.ObservesAsync
import org.jboss.logging.Logger

/**
 * Service that sends notifications in response to domain events.
 * This demonstrates a more complex event handler that performs business logic.
 */
@ApplicationScoped
class NotificationService {
    private val logger = Logger.getLogger(NotificationService::class.java)
    
    /**
     * Send a welcome email when a person is created
     */
    fun sendWelcomeEmail(@ObservesAsync event: PersonEvent.PersonCreated) {
        val email = event.email
        if (email != null) {
            logger.info("Sending welcome email to: ${email.value}")
            sendEmail(email, "Welcome!", "Welcome to our platform!")
        } else {
            logger.info("No email available for person ${event.personId}, skipping welcome email")
        }
    }
    
    /**
     * Send a notification when a person is deleted
     */
    fun sendAccountClosedNotification(@ObservesAsync event: PersonEvent.PersonDeleted) {
        logger.info("Person ${event.personId} (${event.name}) was deleted")
        // In a real app: You might look up the person's email from a cache or database
        // and send a confirmation email
    }
    
    /**
     * Send a verification confirmation
     */
    fun sendVerificationConfirmation(@ObservesAsync event: PersonEvent.ContactVerified) {
        when (event) {
            is PersonEvent.ContactVerified.EmailVerified -> {
                logger.info("Sending email verification confirmation to: ${event.email.value}")
                sendEmail(
                    event.email,
                    "Email Verified",
                    "Your email has been successfully verified!"
                )
            }
            is PersonEvent.ContactVerified.PhoneVerified -> {
                logger.info("Sending SMS verification confirmation to: ${event.phone.value}")
                sendSms(
                    event.phone,
                    "Your phone number has been successfully verified!"
                )
            }
        }
    }
    
    // Simulated email sending
    private fun sendEmail(to: Email, subject: String, body: String) {
        // In a real application, this would connect to an email service
        logger.info("EMAIL TO: ${to.value}, SUBJECT: $subject")
        logger.info("BODY: $body")
    }
    
    // Simulated SMS sending
    private fun sendSms(to: Phone, message: String) {
        // In a real application, this would connect to an SMS service
        logger.info("SMS TO: ${to.value}")
        logger.info("MESSAGE: $message")
    }
}
