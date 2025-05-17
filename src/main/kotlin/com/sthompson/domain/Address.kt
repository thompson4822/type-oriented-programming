package com.sthompson.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Value class representing a postal code with validation.
 */
@JvmInline
value class PostalCode private constructor(val value: String) {
    companion object {
        fun create(raw: String): PostalCode {
            // Simple validation - could be enhanced with country-specific rules
            require(raw.matches(Regex("^[A-Za-z0-9 -]{3,10}$"))) {
                "Invalid postal code format"
            }
            return PostalCode(raw)
        }
    }
}

/**
 * Value class representing a country code with validation.
 */
@JvmInline
value class CountryCode private constructor(val value: String) {
    companion object {
        fun create(raw: String): CountryCode {
            // ISO 3166-1 alpha-2 country code
            require(raw.matches(Regex("^[A-Z]{2}$"))) {
                "Invalid country code format. Must be ISO 3166-1 alpha-2 format (e.g., US, GB)"
            }
            return CountryCode(raw)
        }
    }
}

/**
 * Embeddable class representing a physical address.
 * This is a complex value object with multiple fields.
 */
data class Address(
    val street1: String,
    val street2: String? = null,
    val city: String,
    val state: String? = null,
    val postalCode: PostalCode,
    val countryCode: CountryCode
) {
    init {
        require(street1.isNotBlank()) { "Street address cannot be blank" }
        require(city.isNotBlank()) { "City cannot be blank" }
    }
    
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(street1)
        if (!street2.isNullOrBlank()) sb.append(", $street2")
        sb.append(", $city")
        if (!state.isNullOrBlank()) sb.append(", $state")
        sb.append(", ${postalCode.value}")
        sb.append(", ${countryCode.value}")
        return sb.toString()
    }
}

@Converter(autoApply = true)
class PostalCodeConverter : AttributeConverter<PostalCode, String> {
    override fun convertToDatabaseColumn(postalCode: PostalCode?) = postalCode?.value
    override fun convertToEntityAttribute(dbValue: String?) = 
        dbValue?.let { PostalCode.create(it) }
}

@Converter(autoApply = true)
class CountryCodeConverter : AttributeConverter<CountryCode, String> {
    override fun convertToDatabaseColumn(countryCode: CountryCode?) = countryCode?.value
    override fun convertToEntityAttribute(dbValue: String?) = 
        dbValue?.let { CountryCode.create(it) }
}
