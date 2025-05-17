package com.sthompson.entity

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * Represents the type of organization
 */
enum class OrganizationType {
    BUSINESS,
    GOVERNMENT,
    EDUCATIONAL,
    NON_PROFIT,
    OTHER
}

/**
 * Entity representing an organization in the system
 */
@Entity
class Organization : PanacheEntity() {
    companion object : PanacheCompanion<Organization> {
        fun findByName(name: String): Organization? =
            find("name", name).firstResult()
            
        fun findByEmail(email: Email): Organization? =
            find("email", email.value).firstResult()
    }
    
    @Column(nullable = false)
    lateinit var name: String
    
    @Column(length = 500)
    var description: String? = null
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    lateinit var type: OrganizationType
    
    var email: Email? = null
    
    var phone: Phone? = null
    
    @Column(nullable = false)
    var active: Boolean = true
}
