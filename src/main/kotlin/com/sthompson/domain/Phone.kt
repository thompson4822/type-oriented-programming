package com.sthompson.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@JvmInline
value class Phone private constructor(val value: String) {
    companion object {
        fun create(raw: String): Phone {
            require(raw.matches(Regex("^\\+?[0-9]{10,15}$"))) {
                "Invalid phone format. Must be 10-15 digits with optional + prefix."
            }
            return Phone(raw)
        }
    }

    // Optional domain methods
    fun countryCode(): String = when {
        value.startsWith("+") -> value.substring(1).takeWhile { it.isDigit() }
        else -> ""
    }

    override fun toString(): String = value
}

@Converter(autoApply = true) // Fixed
class PhoneConverter : AttributeConverter<Phone, String> {
    override fun convertToDatabaseColumn(phone: Phone?) = phone?.value
    override fun convertToEntityAttribute(dbValue: String?) = 
        dbValue?.let { Phone.create(it) } // Now throws on invalid data
}
