package com.sthompson.dto

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.entity.Organization
import com.sthompson.entity.OrganizationType

/**
 * Data Transfer Object for Organization entities
 */
data class OrganizationDto(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val type: OrganizationType,
    val email: Email? = null,
    val phone: Phone? = null,
    val active: Boolean = true
) {
    // Empty constructor for Jackson
    constructor() : this(null, "", null, OrganizationType.OTHER, null, null)
    
    companion object {
        /**
         * Create a DTO from an Organization entity
         */
        fun fromEntity(organization: Organization): OrganizationDto {
            return OrganizationDto(
                id = organization.id,
                name = organization.name,
                description = organization.description,
                type = organization.type,
                email = organization.email,
                phone = organization.phone,
                active = organization.active
            )
        }
    }
}
