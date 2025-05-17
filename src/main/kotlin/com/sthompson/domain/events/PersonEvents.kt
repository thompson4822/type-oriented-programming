package com.sthompson.domain.events

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import com.sthompson.entity.Person

/**
 * Sealed class hierarchy for all person-related domain events.
 * This provides type safety and exhaustive handling of all possible
 * person events in the system.
 */
sealed class PersonEvent(eventType: String) : BaseDomainEvent(eventType) {
    abstract val personId: Long
    
    /**
     * Event fired when a new person is created in the system
     */
    data class PersonCreated(
        override val personId: Long,
        val name: String,
        val email: Email?,
        val phone: Phone?
    ) : PersonEvent("person.created")
    
    /**
     * Event fired when a person's details are updated
     */
    data class PersonUpdated(
        override val personId: Long,
        val previousName: String?,
        val newName: String?,
        val previousEmail: Email?,
        val newEmail: Email?,
        val previousPhone: Phone?,
        val newPhone: Phone?
    ) : PersonEvent("person.updated")
    
    /**
     * Event fired when a person is deleted from the system
     */
    data class PersonDeleted(
        override val personId: Long,
        val name: String
    ) : PersonEvent("person.deleted")
    
    /**
     * Event fired when a person's contact information is verified
     */
    sealed class ContactVerified(
        eventType: String,
        override val personId: Long
    ) : PersonEvent(eventType) {
        
        /**
         * Event fired when a person's email is verified
         */
        data class EmailVerified(
            override val personId: Long,
            val email: Email
        ) : ContactVerified("person.email.verified", personId)
        
        /**
         * Event fired when a person's phone is verified
         */
        data class PhoneVerified(
            override val personId: Long,
            val phone: Phone
        ) : ContactVerified("person.phone.verified", personId)
    }
    
    companion object {
        /**
         * Create a PersonCreated event from a Person entity
         */
        fun fromNewPerson(person: Person): PersonCreated {
            return PersonCreated(
                personId = person.id!!,
                name = person.name,
                email = person.email,
                phone = person.phone
            )
        }
    }
}
