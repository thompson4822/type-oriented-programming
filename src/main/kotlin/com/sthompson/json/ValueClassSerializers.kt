package com.sthompson.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.sthompson.domain.*

// Phone serialization
class PhoneSerializer : StdSerializer<Phone>(Phone::class.java) {
    override fun serialize(value: Phone, gen: JsonGenerator, provider: SerializerProvider) {
        // Simply write the string value without any class information
        gen.writeString(value.value)
    }
}

class PhoneDeserializer : StdDeserializer<Phone>(Phone::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Phone {
        return try {
            // Handle both string values and objects with a "value" field
            val node = p.codec.readTree<JsonNode>(p)
            val value = when {
                node.isTextual -> node.textValue()
                node.has("value") -> node.get("value").textValue()
                else -> throw ctxt.weirdStringException(node.toString(), Phone::class.java, "Expected string or object with 'value' field")
            }
            Phone.create(value)
        } catch (e: IllegalArgumentException) {
            throw ctxt.weirdStringException(p.valueAsString, Phone::class.java, e.message)
        }
    }
}

// Email serialization
class EmailSerializer : StdSerializer<Email>(Email::class.java) {
    override fun serialize(value: Email, gen: JsonGenerator, provider: SerializerProvider) {
        // Simply write the string value without any class information
        gen.writeString(value.value)
    }
}

class EmailDeserializer : StdDeserializer<Email>(Email::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Email {
        return try {
            // Handle both string values and objects with a "value" field
            val node = p.codec.readTree<JsonNode>(p)
            val value = when {
                node.isTextual -> node.textValue()
                node.has("value") -> node.get("value").textValue()
                else -> throw ctxt.weirdStringException(node.toString(), Email::class.java, "Expected string or object with 'value' field")
            }
            Email.create(value)
        } catch (e: IllegalArgumentException) {
            throw ctxt.weirdStringException(p.valueAsString, Email::class.java, e.message)
        }
    }
}

// PostalCode serialization
class PostalCodeSerializer : StdSerializer<PostalCode>(PostalCode::class.java) {
    override fun serialize(value: PostalCode, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.value)
    }
}

class PostalCodeDeserializer : StdDeserializer<PostalCode>(PostalCode::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PostalCode {
        return try {
            val node = p.codec.readTree<JsonNode>(p)
            val value = when {
                node.isTextual -> node.textValue()
                node.has("value") -> node.get("value").textValue()
                else -> throw ctxt.weirdStringException(node.toString(), PostalCode::class.java, "Expected string or object with 'value' field")
            }
            PostalCode.create(value)
        } catch (e: IllegalArgumentException) {
            throw ctxt.weirdStringException(p.valueAsString, PostalCode::class.java, e.message)
        }
    }
}

// CountryCode serialization
class CountryCodeSerializer : StdSerializer<CountryCode>(CountryCode::class.java) {
    override fun serialize(value: CountryCode, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.value)
    }
}

class CountryCodeDeserializer : StdDeserializer<CountryCode>(CountryCode::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): CountryCode {
        return try {
            val node = p.codec.readTree<JsonNode>(p)
            val value = when {
                node.isTextual -> node.textValue()
                node.has("value") -> node.get("value").textValue()
                else -> throw ctxt.weirdStringException(node.toString(), CountryCode::class.java, "Expected string or object with 'value' field")
            }
            CountryCode.create(value)
        } catch (e: IllegalArgumentException) {
            throw ctxt.weirdStringException(p.valueAsString, CountryCode::class.java, e.message)
        }
    }
}