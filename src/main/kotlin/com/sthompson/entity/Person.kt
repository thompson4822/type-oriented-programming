package com.sthompson.entity

import com.sthompson.domain.Email
import com.sthompson.domain.EmailConverter
import com.sthompson.domain.Phone
import com.sthompson.domain.PhoneConverter
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import jakarta.persistence.Entity
import jakarta.persistence.Convert

@Entity
class Person : PanacheEntity() {
    companion object : PanacheCompanion<Person> {
        fun findByEmail(email: Email): Person? = 
            find("email", email.value).firstResult() // Fixed query
        
        fun findByPhone(phone: Phone): Person? = 
            find("phone", phone.value).firstResult() // Fixed query
    }

    lateinit var name: String
    var email: Email? = null // Converter auto-applied
    var phone: Phone? = null // Converter auto-applied
}
