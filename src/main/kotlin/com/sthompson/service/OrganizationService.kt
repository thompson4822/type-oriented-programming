package com.sthompson.service

import com.sthompson.domain.Email
import com.sthompson.domain.result.Failure
import com.sthompson.domain.result.FailureReason
import com.sthompson.domain.result.OperationResult
import com.sthompson.dto.OrganizationDto
import com.sthompson.entity.Membership
import com.sthompson.entity.MembershipRole
import com.sthompson.entity.Organization
import com.sthompson.entity.Person
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.LocalDateTime

/**
 * Service for managing Organization entities with result types.
 */
@ApplicationScoped
class OrganizationService {
    @Inject
    lateinit var eventPublisher: EventPublisher

    private val logger = Logger.getLogger(OrganizationService::class.java)

    /**
     * Find an organization by ID with a type-safe result
     */
    fun findById(id: Long): OperationResult<Organization> {
        val organization = Organization.findById(id)
        return if (organization != null) {
            OperationResult.success(organization)
        } else {
            OperationResult.notFound("Organization with ID $id not found")
        }
    }

    /**
     * Find an organization by name with a type-safe result
     */
    fun findByName(name: String): OperationResult<Organization> {
        val organization = Organization.findByName(name)
        return if (organization != null) {
            OperationResult.success(organization)
        } else {
            OperationResult.notFound("Organization with name '$name' not found")
        }
    }

    /**
     * Create a new organization with the creator as owner
     */
    @Transactional
    fun createOrganization(dto: OrganizationDto, creatorId: Long): OperationResult<Organization> {
        // Check if name already exists
        if (Organization.findByName(dto.name) != null) {
            return OperationResult.failure(
                FailureReason.OrganizationFailure.NameAlreadyExists(dto.name)
            )
        }

        // Find the creator
        val creator = Person.findById(creatorId) ?: return OperationResult.failure(
            FailureReason.NotFound("Creator with ID $creatorId not found")
        )

        // Create and persist the organization
        val organization = Organization().apply {
            name = dto.name
            description = dto.description
            type = dto.type
            email = dto.email
            phone = dto.phone
            active = true
        }

        try {
            organization.persist()

            // Create membership for the creator as owner
            val membership = Membership().apply {
                person = creator
                this.organization = organization
                role = MembershipRole.OWNER
                joinedAt = LocalDateTime.now()
                active = true
            }
            membership.persist()

            return OperationResult.success(organization)
        } catch (e: Exception) {
            logger.error("Failed to create organization", e)
            return OperationResult.failure("Failed to create organization: ${e.message}", e)
        }
    }

    /**
     * Add a member to an organization
     */
    @Transactional
    fun addMember(organizationId: Long, personId: Long, role: MembershipRole): OperationResult<Membership> {
        // Find the organization
        val organizationResult = findById(organizationId)
        if (organizationResult.isFailure()) {
            // Extract the failure reason and return it
            val failureReason = (organizationResult as Failure).reason
            return OperationResult.failure(failureReason)
        }
        val organization = organizationResult.getOrNull()!!

        // Find the person
        val person = Person.findById(personId) ?: return OperationResult.failure(
            FailureReason.NotFound("Person with ID $personId not found")
        )

        // Check if membership already exists
        val existingMembership = Membership.findByPersonAndOrganization(personId, organizationId)
        if (existingMembership != null) {
            if (existingMembership.active) {
                return OperationResult.failure(
                    FailureReason.OrganizationFailure.AlreadyMember(personId, organizationId)
                )
            } else {
                // Reactivate the membership
                existingMembership.active = true
                existingMembership.role = role
                existingMembership.joinedAt = LocalDateTime.now()
                existingMembership.expiresAt = null
                existingMembership.persist()
                return OperationResult.success(existingMembership)
            }
        }

        // Create new membership
        val membership = Membership().apply {
            this.person = person
            this.organization = organization
            this.role = role
            joinedAt = LocalDateTime.now()
            active = true
        }

        try {
            membership.persist()
            return OperationResult.success(membership)
        } catch (e: Exception) {
            logger.error("Failed to add member to organization", e)
            return OperationResult.failure("Failed to add member: ${e.message}", e)
        }
    }

    /**
     * Get all members of an organization
     */
    fun getMembers(organizationId: Long): OperationResult<List<Membership>> {
        // Find the organization
        val organizationResult = findById(organizationId)
        if (organizationResult.isFailure()) {
            // Extract the failure reason and return it
            val failureReason = (organizationResult as Failure).reason
            return OperationResult.failure(failureReason)
        }

        val memberships = Membership.findByOrganization(organizationId)
        return OperationResult.success(memberships)
    }

    /**
     * Get all organizations a person is a member of
     */
    fun getPersonOrganizations(personId: Long): OperationResult<List<Membership>> {
        // Find the person
        val person = Person.findById(personId) ?: return OperationResult.failure(
            FailureReason.NotFound("Person with ID $personId not found")
        )

        val memberships = Membership.findActiveByPerson(personId)
        return OperationResult.success(memberships)
    }
}
