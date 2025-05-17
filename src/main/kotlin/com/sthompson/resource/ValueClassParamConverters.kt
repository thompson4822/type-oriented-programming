package com.sthompson.resource

import com.sthompson.domain.Email
import com.sthompson.domain.Phone
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ext.ParamConverter
import jakarta.ws.rs.ext.ParamConverterProvider
import jakarta.ws.rs.ext.Provider
import java.lang.reflect.Type

@Provider
class ValueClassParamConverterProvider : ParamConverterProvider {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> getConverter(
        rawType: Class<T>,
        genericType: Type?,
        annotations: Array<out Annotation>?
    ): ParamConverter<T>? {
        return when (rawType) {
            Email::class.java -> EmailParamConverter() as ParamConverter<T>
            Phone::class.java -> PhoneParamConverter() as ParamConverter<T>
            else -> null
        }
    }

    private interface TypedParamConverter<T> : ParamConverter<T> {
        val targetType: Class<T>
            get() = throw NotImplementedError()
    }

    private class EmailParamConverter : TypedParamConverter<Email> {
        override val targetType: Class<Email> = Email::class.java
        
        override fun fromString(value: String?): Email {
            return try {
                requireNotNull(value) { "Email cannot be null" }
                Email.create(value)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid email format: ${value?.take(20)}", e)
            }
        }

        override fun toString(value: Email?): String {
            return value?.value ?: throw BadRequestException("Email cannot be null")
        }
    }

    private class PhoneParamConverter : TypedParamConverter<Phone> {
        override val targetType: Class<Phone> = Phone::class.java
        
        override fun fromString(value: String?): Phone {
            return try {
                requireNotNull(value) { "Phone cannot be null" }
                Phone.create(value)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid phone format: ${value?.take(20)}", e)
            }
        }

        override fun toString(value: Phone?): String {
            return value?.value ?: throw BadRequestException("Phone cannot be null")
        }
    }
}
