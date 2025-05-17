package com.sthompson.dto

import com.sthompson.entity.Membership
import com.sthompson.entity.MembershipRole
import java.time.LocalDateTime

/**
 * Data Transfer Object for Membership entities
 */
data class MembershipDto(
    val id: Long? = null,
    val personId: Long,
    val personName: String,
    val organizationId: Long,
    val organizationName: String,
    val role: MembershipRole,
    val joinedAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null,
    val active: Boolean
) {
    // Empty constructor for Jackson
    constructor() : this(
        null, 0, "", 0, "", 
        MembershipRole.MEMBER, LocalDateTime.now(), null, true
    )
    
    companion object {
        /**
         * Create a DTO from a Membership entity
         */
        fun fromEntity(membership: Membership): MembershipDto {
            return MembershipDto(
                id = membership.id,
                personId = membership.person.id!!,
                personName = membership.person.name,
                organizationId = membership.organization.id!!,
                organizationName = membership.organization.name,
                role = membership.role,
                joinedAt = membership.joinedAt,
                expiresAt = membership.expiresAt,
                active = membership.active
            )
        }
    }
}
