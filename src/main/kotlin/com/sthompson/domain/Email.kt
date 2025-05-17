package com.sthompson.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        fun create(raw: String): Email {
            require(raw.matches(Regex("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,}$"))) {
                "Invalid email format"
            }
            return Email(raw)
        }
    }
}

@Converter(autoApply = true) // Fixed
class EmailConverter : AttributeConverter<Email, String> {
    override fun convertToDatabaseColumn(email: Email?) = email?.value
    override fun convertToEntityAttribute(dbValue: String?) = 
        dbValue?.let { Email.create(it) } // Now throws on invalid data
}
