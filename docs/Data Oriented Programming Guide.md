# Data-Oriented Programming Guide

This document provides guidance on applying data-oriented programming principles in our application. Data-oriented programming focuses on modeling domain concepts as rich, type-safe data structures that encapsulate behavior and validation.

## Table of Contents

1. [Core Principles](#core-principles)
2. [Type-Oriented Approach](#type-oriented-approach)
3. [Abstract Data Types with Sealed Classes](#abstract-data-types-with-sealed-classes)
4. [Result Types and Error Handling](#result-types-and-error-handling)
5. [Event-Driven Architecture](#event-driven-architecture)
6. [Practical Guidelines](#practical-guidelines)
7. [Examples](#examples)
8. [Transitioning to Data-Oriented Thinking](#transitioning-to-data-oriented-thinking)

## Core Principles

Data-oriented programming is built on several key principles:

1. **Rich Domain Types**: Model domain concepts as dedicated types rather than primitives
2. **Type Safety**: Use the type system to prevent invalid states and operations
3. **Immutability**: Prefer immutable data structures to reduce side effects
4. **Validation at Construction**: Ensure data is valid at creation time
5. **Explicit Error Handling**: Use result types instead of exceptions for expected error cases
6. **Composition**: Build complex behaviors by composing simpler ones

These principles lead to code that is more expressive, safer, and easier to reason about.

## Type-Oriented Approach

The type-oriented approach involves creating dedicated types for domain concepts, even when they could be represented by primitive types.

### Value Classes

Use value classes to represent domain primitives:

```kotlin
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
```

Key characteristics:

1. **Private Constructor**: Forces use of factory methods with validation
2. **Validation at Creation**: Ensures all instances are valid
3. **Type Safety**: Prevents mixing different concepts that share the same underlying type
4. **Domain Methods**: Can include domain-specific behavior

### Implementation in Persistence Layer

To use value classes with JPA/Hibernate:

1. Create `AttributeConverter` implementations:
   ```kotlin
   @Converter(autoApply = true)
   class EmailConverter : AttributeConverter<Email, String> {
       override fun convertToDatabaseColumn(email: Email?) = email?.value
       override fun convertToEntityAttribute(dbValue: String?) = 
           dbValue?.let { Email.create(it) }
   }
   ```

2. Use in entity classes:
   ```kotlin
   @Entity
   class Person : PanacheEntity() {
       lateinit var name: String
       var email: Email? = null // Converter auto-applied
       var phone: Phone? = null // Converter auto-applied
   }
   ```

### Implementation in API Layer

To use value classes in REST endpoints:

1. Create serializers/deserializers:
   ```kotlin
   class EmailSerializer : StdSerializer<Email>(Email::class.java) {
       override fun serialize(value: Email, gen: JsonGenerator, provider: SerializerProvider) {
           gen.writeString(value.value)
       }
   }
   
   class EmailDeserializer : StdDeserializer<Email>(Email::class.java) {
       override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Email {
           return try {
               Email.create(p.valueAsString)
           } catch (e: IllegalArgumentException) {
               throw ctxt.weirdStringException(p.valueAsString, Email::class.java, e.message)
           }
       }
   }
   ```

2. Create parameter converters for path/query parameters:
   ```kotlin
   @Provider
   class ValueClassParamConverterProvider : ParamConverterProvider {
       @Suppress("UNCHECKED_CAST")
       override fun <T> getConverter(rawType: Class<T>, genericType: Type?, annotations: Array<out Annotation>?): ParamConverter<T>? {
           return when (rawType) {
               Email::class.java -> EmailParamConverter() as ParamConverter<T>
               else -> null
           }
       }
   }
   ```

## Abstract Data Types with Sealed Classes

Sealed classes and interfaces provide a powerful way to model domain concepts with a fixed set of variants.

### Modeling Domain Concepts

Use sealed classes to represent concepts with a finite set of possibilities:

```kotlin
sealed class PaymentMethod {
    data class CreditCard(
        val cardNumber: String,
        val expiryMonth: Int,
        val expiryYear: Int,
        val cvv: String
    ) : PaymentMethod()
    
    data class BankTransfer(
        val accountNumber: String,
        val routingNumber: String
    ) : PaymentMethod()
    
    data object Cash : PaymentMethod()
}
```

### Exhaustive Handling

Sealed classes enable exhaustive handling with `when` expressions:

```kotlin
fun processPayment(amount: Money, method: PaymentMethod): PaymentResult {
    return when (method) {
        is PaymentMethod.CreditCard -> processCreditCardPayment(amount, method)
        is PaymentMethod.BankTransfer -> processBankTransferPayment(amount, method)
        PaymentMethod.Cash -> processCashPayment(amount)
    }
}
```

### Nested Sealed Hierarchies

Create rich domain models with nested sealed hierarchies:

```kotlin
sealed class OrderStatus {
    data object Pending : OrderStatus()
    data object Processing : OrderStatus()
    sealed class Shipped : OrderStatus() {
        data class InTransit(val trackingNumber: String) : Shipped()
        data class Delivered(val deliveryDate: LocalDate) : Shipped()
    }
    sealed class Cancelled : OrderStatus() {
        data class ByCustomer(val reason: String?) : Cancelled()
        data class ByMerchant(val reason: String) : Cancelled()
        data object OutOfStock : Cancelled()
    }
}
```

## Result Types and Error Handling

Use result types to handle operations that can fail without using exceptions.

### OperationResult Type

```kotlin
sealed interface OperationResult<out T> {
    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (FailureReason) -> R
    ): R
    
    // Other utility methods...
}

data class Success<out T>(val value: T) : OperationResult<T>
data class Failure<out T>(val reason: FailureReason) : OperationResult<T>
```

### Domain-Specific Failure Reasons

Create domain-specific failure types:

```kotlin
sealed class FailureReason {
    // General failure types...
    
    sealed class PersonFailure : FailureReason() {
        data class EmailAlreadyExists(val email: Email) : PersonFailure() {
            override fun toString() = "Person with email ${email.value} already exists"
        }
        
        data class PhoneAlreadyExists(val phone: Phone) : PersonFailure() {
            override fun toString() = "Person with phone ${phone.value} already exists"
        }
    }
}
```

### Using Result Types

In service methods:

```kotlin
fun createPerson(dto: PersonDto): OperationResult<Person> {
    if (dto.email != null && Person.findByEmail(dto.email) != null) {
        return OperationResult.failure(
            FailureReason.PersonFailure.EmailAlreadyExists(dto.email)
        )
    }
    
    // Create and persist the person...
    return OperationResult.success(person)
}
```

In controllers:

```kotlin
fun create(dto: PersonDto): Response {
    val result = personService.createPerson(dto)
    
    return result.fold(
        onSuccess = { person ->
            Response.created(UriBuilder.fromResource(PersonResource::class.java)
                .path(person.id.toString())
                .build())
                .entity(PersonDto(person.id, person.name, person.email, person.phone))
                .build()
        },
        onFailure = { reason ->
            when (reason) {
                is FailureReason.PersonFailure.EmailAlreadyExists ->
                    Response.status(Response.Status.CONFLICT)
                        .entity(mapOf("message" to reason.toString()))
                        .build()
                // Handle other failure types...
                else -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(mapOf("message" to "An error occurred"))
                    .build()
            }
        }
    )
}
```

## Event-Driven Architecture

Combine data-oriented programming with event-driven architecture for a decoupled system.

### Domain Events as Data

Model events as immutable data classes:

```kotlin
sealed class PersonEvent(eventType: String) : BaseDomainEvent(eventType) {
    abstract val personId: Long
    
    data class PersonCreated(
        override val personId: Long,
        val name: String,
        val email: Email?,
        val phone: Phone?
    ) : PersonEvent("person.created")
    
    // Other event types...
}
```

### Publishing Events

Publish events after state changes:

```kotlin
@Transactional
fun createPerson(dto: PersonDto): OperationResult<Person> {
    // Validation and creation...
    
    try {
        person.persist()
        
        // Publish domain event
        val event = PersonEvent.fromNewPerson(person)
        eventPublisher.publish(event)
        
        return OperationResult.success(person)
    } catch (e: Exception) {
        logger.error("Failed to create person", e)
        return OperationResult.failure("Failed to create person: ${e.message}", e)
    }
}
```

## Practical Guidelines

### When to Create a New Type

Create a new type when:

1. **Domain Significance**: The concept has meaning in your domain
2. **Validation Rules**: The value must satisfy specific rules
3. **Behavior**: The concept has associated behavior
4. **Type Safety**: You want to prevent mixing with other similar types

### Designing Value Classes

1. **Make Constructors Private**: Force use of factory methods
2. **Validate at Creation**: Ensure all instances are valid
3. **Keep Immutable**: Avoid mutable state
4. **Add Domain Methods**: Include relevant behavior

### Designing Sealed Class Hierarchies

1. **Identify Variants**: Determine the complete set of possibilities
2. **Model Data for Each Variant**: Include relevant data for each case
3. **Consider Nesting**: Use nested hierarchies for complex domains
4. **Provide Factory Methods**: Make creation convenient and safe

### Error Handling Strategy

1. **Use Result Types for Expected Failures**: Business logic errors, validation failures
2. **Use Exceptions for Unexpected Failures**: Programming errors, infrastructure issues
3. **Make Failure Types Domain-Specific**: Create failure types that match your domain
4. **Include Relevant Context**: Ensure failures contain enough information to handle them

## Examples

### Example 1: Money Type

```kotlin
@JvmInline
value class Money private constructor(val cents: Long) {
    companion object {
        fun fromCents(cents: Long): Money {
            require(cents >= 0) { "Amount cannot be negative" }
            return Money(cents)
        }
        
        fun fromDollars(dollars: Double): Money {
            require(dollars >= 0) { "Amount cannot be negative" }
            return Money((dollars * 100).roundToLong())
        }
    }
    
    fun toDollars(): Double = cents / 100.0
    
    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money {
        require(cents >= other.cents) { "Result would be negative" }
        return Money(cents - other.cents)
    }
    
    override fun toString(): String = "$${toDollars()}"
}
```

### Example 2: Order Processing with Result Types

```kotlin
sealed class OrderProcessingError {
    data class ProductOutOfStock(val productId: String) : OrderProcessingError()
    data class PaymentFailed(val reason: String) : OrderProcessingError()
    data class ShippingUnavailable(val address: Address) : OrderProcessingError()
}

fun processOrder(order: Order): OperationResult<OrderConfirmation, OrderProcessingError> {
    // Check inventory
    for (item in order.items) {
        val available = inventoryService.checkAvailability(item.productId, item.quantity)
        if (!available) {
            return OperationResult.failure(OrderProcessingError.ProductOutOfStock(item.productId))
        }
    }
    
    // Process payment
    val paymentResult = paymentService.processPayment(order.payment, order.total)
    if (paymentResult.isFailure()) {
        return OperationResult.failure(OrderProcessingError.PaymentFailed(paymentResult.errorMessage))
    }
    
    // Arrange shipping
    val shippingResult = shippingService.arrangeShipping(order.shippingAddress, order.items)
    if (shippingResult.isFailure()) {
        return OperationResult.failure(OrderProcessingError.ShippingUnavailable(order.shippingAddress))
    }
    
    // Create confirmation
    val confirmation = OrderConfirmation(
        orderId = order.id,
        paymentId = paymentResult.paymentId,
        trackingNumber = shippingResult.trackingNumber,
        estimatedDelivery = shippingResult.estimatedDelivery
    )
    
    return OperationResult.success(confirmation)
}
```

## Transitioning to Data-Oriented Thinking

Shifting to data-oriented programming requires a change in mindset:

1. **Think in Types, Not Classes**: Focus on modeling data and its constraints
2. **Embrace Immutability**: Design for transformation, not mutation
3. **Make Invalid States Unrepresentable**: Use the type system to prevent errors
4. **Separate Data from Behavior**: Keep data structures simple and focused
5. **Design for Composition**: Build complex systems from simple parts
6. **Focus on Transformations**: Think of your program as a series of data transformations
7. **Explicit is Better than Implicit**: Make behavior and constraints explicit

### Practical Steps

1. **Start with Domain Primitives**: Replace primitive types with value classes
2. **Introduce Result Types**: Replace exceptions with explicit result types
3. **Model with Sealed Classes**: Identify concepts with finite variants
4. **Add Domain Events**: Decouple components with events
5. **Refactor Incrementally**: Apply these patterns gradually

By following these principles and guidelines, you can create a codebase that is more expressive, safer, and easier to maintain.
