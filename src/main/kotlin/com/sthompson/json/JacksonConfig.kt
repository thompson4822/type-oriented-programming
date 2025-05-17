package com.sthompson.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.sthompson.domain.*
import io.quarkus.jackson.ObjectMapperCustomizer
import jakarta.inject.Singleton

/**
 * Customizes the Jackson ObjectMapper for proper handling of Kotlin value classes.
 * This ensures that value classes like Phone and Email are serialized correctly
 * without mangled field names.
 */
@Singleton
class ValueClassCustomizer : ObjectMapperCustomizer {
    override fun customize(objectMapper: ObjectMapper) {
        // Create a module for our value class serializers/deserializers
        val valueClassModule = SimpleModule("ValueClassModule")
            // Phone serialization
            .addSerializer(Phone::class.java, PhoneSerializer())
            .addDeserializer(Phone::class.java, PhoneDeserializer())
            // Email serialization
            .addSerializer(Email::class.java, EmailSerializer())
            .addDeserializer(Email::class.java, EmailDeserializer())
            // PostalCode serialization
            .addSerializer(PostalCode::class.java, PostalCodeSerializer())
            .addDeserializer(PostalCode::class.java, PostalCodeDeserializer())
            // CountryCode serialization
            .addSerializer(CountryCode::class.java, CountryCodeSerializer())
            .addDeserializer(CountryCode::class.java, CountryCodeDeserializer())

        // Register the module
        objectMapper.registerModule(valueClassModule)

        // Configure ObjectMapper for better Kotlin compatibility
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}