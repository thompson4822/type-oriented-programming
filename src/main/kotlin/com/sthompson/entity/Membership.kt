package com.sthompson.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Represents the role a person has within an organization
 */
enum class MembershipRole {
    OWNER,
    ADMIN,
    MEMBER,
    GUEST
}

/**
 * Entity representing the relationship between a Person and an Organization
 */
@Entity
class Membership : PanacheEntity() {
    companion object : PanacheCompanion<Membership> {
        fun findByPersonAndOrganization(personId: Long, organizationId: Long): Membership? =
            find("person.id = ?1 and organization.id = ?2", personId, organizationId).firstResult()
            
        fun findByPerson(personId: Long): List<Membership> =
            list("person.id", personId)
            
        fun findByOrganization(organizationId: Long): List<Membership> =
            list("organization.id", organizationId)
            
        fun findActiveByPerson(personId: Long): List<Membership> =
            list("person.id = ?1 and active = true", personId)
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    lateinit var person: Person
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    lateinit var organization: Organization
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var role: MembershipRole
    
    @Column(nullable = false)
    var joinedAt: LocalDateTime = LocalDateTime.now()
    
    var expiresAt: LocalDateTime? = null
    
    @Column(nullable = false)
    var active: Boolean = true
    
    /**
     * Check if the membership is currently valid
     */
    fun isValid(): Boolean {
        if (!active) return false
        
        val now = LocalDateTime.now()
        return expiresAt == null || expiresAt!!.isAfter(now)
    }
}
