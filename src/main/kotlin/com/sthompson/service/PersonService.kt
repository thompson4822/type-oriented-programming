package com.sthompson.service

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.domain.events.PersonEvent
import com.sthompson.domain.result.FailureReason
import com.sthompson.domain.result.OperationResult
import com.sthompson.dto.PersonDto
import com.sthompson.entity.Person
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger

/**
 * Service for managing Person entities with domain events and result types.
 */
@ApplicationScoped
class PersonService {
    @Inject
    lateinit var eventPublisher: EventPublisher

    private val logger = Logger.getLogger(PersonService::class.java)

    /**
     * Find a person by ID with a type-safe result
     */
    fun findById(id: Long): OperationResult<Person> {
        val person = Person.findById(id)
        return if (person != null) {
            OperationResult.success(person)
        } else {
            OperationResult.notFound("Person with ID $id not found")
        }
    }

    /**
     * Find a person by email with a type-safe result
     */
    fun findByEmail(email: Email): OperationResult<Person> {
        val person = Person.findByEmail(email)
        return if (person != null) {
            OperationResult.success(person)
        } else {
            OperationResult.notFound("Person with email ${email.value} not found")
        }
    }

    /**
     * Find a person by phone with a type-safe result
     */
    fun findByPhone(phone: Phone): OperationResult<Person> {
        val person = Person.findByPhone(phone)
        return if (person != null) {
            OperationResult.success(person)
        } else {
            OperationResult.notFound("Person with phone ${phone.value} not found")
        }
    }

    /**
     * Create a new person with domain events
     */
    @Transactional
    fun createPerson(dto: PersonDto): OperationResult<Person> {
        // Check if email already exists
        if (dto.email != null && Person.findByEmail(dto.email) != null) {
            return OperationResult.failure(
                FailureReason.Conflict("Person with email ${dto.email.value} already exists")
            )
        }

        // Check if phone already exists
        if (dto.phone != null && Person.findByPhone(dto.phone) != null) {
            return OperationResult.failure(
                FailureReason.Conflict("Person with phone ${dto.phone.value} already exists")
            )
        }

        // Create and persist the person
        val person = Person().apply {
            name = dto.name
            email = dto.email
            phone = dto.phone
        }

        try {
            person.persist()

            // Publish domain event
            val event = PersonEvent.fromNewPerson(person)
            eventPublisher.publish(event)

            return OperationResult.success(person)
        } catch (e: Exception) {
            logger.error("Failed to create person", e)
            return OperationResult.failure("Failed to create person: ${e.message}", e)
        }
    }

    /**
     * Update a person with domain events
     */
    @Transactional
    fun updatePerson(id: Long, dto: PersonDto): OperationResult<Person> {
        val personResult = findById(id)

        return personResult.fold(
            onSuccess = { person ->
                // Store previous values for the event
                val previousName = person.name
                val previousEmail = person.email
                val previousPhone = person.phone

                // Update the person
                person.name = dto.name
                person.email = dto.email
                person.phone = dto.phone

                try {
                    person.persist()

                    // Publish domain event
                    val event = PersonEvent.PersonUpdated(
                        personId = person.id!!,
                        previousName = previousName,
                        newName = person.name,
                        previousEmail = previousEmail,
                        newEmail = person.email,
                        previousPhone = previousPhone,
                        newPhone = person.phone
                    )
                    eventPublisher.publish(event)

                    OperationResult.success(person)
                } catch (e: Exception) {
                    logger.error("Failed to update person", e)
                    OperationResult.failure("Failed to update person: ${e.message}", e)
                }
            },
            onFailure = { reason ->
                OperationResult.failure(reason)
            }
        )
    }

    /**
     * Delete a person with domain events
     */
    @Transactional
    fun deletePerson(id: Long): OperationResult<Unit> {
        val personResult = findById(id)

        return personResult.fold(
            onSuccess = { person ->
                val name = person.name

                try {
                    person.delete()

                    // Publish domain event
                    val event = PersonEvent.PersonDeleted(
                        personId = id,
                        name = name
                    )
                    eventPublisher.publish(event)

                    OperationResult.success(Unit)
                } catch (e: Exception) {
                    logger.error("Failed to delete person", e)
                    OperationResult.failure("Failed to delete person: ${e.message}", e)
                }
            },
            onFailure = { reason ->
                OperationResult.failure(reason)
            }
        )
    }

    /**
     * Verify a person's email with domain events
     */
    @Transactional
    fun verifyEmail(id: Long, email: Email): OperationResult<Person> {
        val personResult = findById(id)

        return personResult.fold(
            onSuccess = { person ->
                if (person.email != email) {
                    OperationResult.failure(
                        FailureReason.ValidationFailed("Email ${email.value} does not match person's email")
                    )
                } else {

                    // Publish domain event
                    val event = PersonEvent.ContactVerified.EmailVerified(
                        personId = id,
                        email = email
                    )
                    eventPublisher.publish(event)

                    OperationResult.success(person)
                }
            },
            onFailure = { reason ->
                OperationResult.failure(reason)
            }
        )
    }
}
