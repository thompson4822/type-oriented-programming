# Event System Guide

This document outlines how to use and extend the event system in our application. The event system is designed to be simple, type-safe, and decoupled, allowing different parts of the application to communicate without direct dependencies.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Event Types](#event-types)
3. [Adding New Events](#adding-new-events)
4. [Publishing Events](#publishing-events)
5. [Subscribing to Events](#subscribing-to-events)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

## Architecture Overview

Our event system is built on Quarkus CDI events, providing a simple yet powerful way to implement event-driven architecture without external dependencies like Kafka. The system consists of:

- **Domain Events**: Sealed class hierarchy representing different types of events
- **Event Publisher**: Service for publishing events to subscribers
- **Event Subscribers**: Components that listen for and react to events

This approach allows for loose coupling between components, making the system more maintainable and extensible.

## Event Types

Events are organized in a sealed class hierarchy:

```
DomainEvent (base interface)
├── PersonEvent (person-related events)
│   ├── PersonCreated
│   ├── PersonUpdated
│   ├── PersonDeleted
│   └── ContactVerified
│       ├── EmailVerified
│       └── PhoneVerified
└── SystemEvent (system-level events)
    ├── ApplicationStarted
    ├── ApplicationStopping
    └── JobCompleted
        ├── DataImportCompleted
        └── NotificationJobCompleted
```

Each event carries relevant data and has a unique event type identifier.

## Adding New Events

To add a new event type to the system, follow these steps:

### 1. Identify the Event Category

Determine whether your new event belongs to an existing category (e.g., `PersonEvent`, `SystemEvent`) or requires a new category.

### 2. Define the Event Class

Add your event class to the appropriate sealed class hierarchy:

```kotlin
// For an existing category (e.g., PersonEvent)
sealed class PersonEvent(eventType: String) : BaseDomainEvent(eventType) {
    // Existing event classes...
    
    /**
     * Event fired when a person's subscription is renewed
     */
    data class SubscriptionRenewed(
        override val personId: Long,
        val subscriptionId: String,
        val expiresAt: LocalDateTime
    ) : PersonEvent("person.subscription.renewed")
}

// For a new category
sealed class OrderEvent(eventType: String) : BaseDomainEvent(eventType) {
    /**
     * Event fired when a new order is created
     */
    data class OrderCreated(
        val orderId: String,
        val personId: Long,
        val amount: BigDecimal
    ) : OrderEvent("order.created")
    
    /**
     * Event fired when an order is fulfilled
     */
    data class OrderFulfilled(
        val orderId: String,
        val fulfilledAt: Instant
    ) : OrderEvent("order.fulfilled")
}
```

### 3. Update the EventPublisher

If you've added a new event category, update the `EventPublisher` to handle it:

```kotlin
@ApplicationScoped
class EventPublisher {
    // Existing event channels...
    
    @Inject
    lateinit var orderEvents: Event<OrderEvent>
    
    fun publish(event: DomainEvent) {
        logger.debug("Publishing event: ${event.eventType} (${event.eventId})")
        
        // Fire the generic event for all subscribers
        eventBus.fire(event)
        
        // Also fire the specific event type for targeted subscribers
        when (event) {
            is PersonEvent -> personEvents.fire(event)
            is SystemEvent -> systemEvents.fire(event)
            is OrderEvent -> orderEvents.fire(event)
            else -> logger.warn("Unknown event type: ${event.javaClass.name}")
        }
    }
}
```

## Publishing Events

To publish an event from your code:

### 1. Create the Event Instance

```kotlin
val event = PersonEvent.SubscriptionRenewed(
    personId = person.id!!,
    subscriptionId = subscription.id,
    expiresAt = subscription.expiresAt
)
```

### 2. Inject and Use the EventPublisher

```kotlin
@Inject
lateinit var eventPublisher: EventPublisher

// In your method
eventPublisher.publish(event)
```

### 3. Where to Publish Events

Events should typically be published from:

- **Service Layer**: When business operations complete
- **Domain Entities**: For domain events (using domain services)
- **Application Lifecycle**: For system events

## Subscribing to Events

To subscribe to events:

### 1. Create an Event Listener Class

```kotlin
@ApplicationScoped
class SubscriptionEventListener {
    private val logger = Logger.getLogger(SubscriptionEventListener::class.java)
    
    /**
     * Handle subscription renewals (synchronous)
     */
    fun onSubscriptionRenewed(@Observes event: PersonEvent.SubscriptionRenewed) {
        logger.info("Subscription renewed: Person ID=${event.personId}, " +
                   "Subscription ID=${event.subscriptionId}, " +
                   "Expires at=${event.expiresAt}")
        
        // Handle the event...
    }
    
    /**
     * Handle subscription renewals (asynchronous)
     */
    fun sendRenewalConfirmation(@ObservesAsync event: PersonEvent.SubscriptionRenewed) {
        logger.info("Sending renewal confirmation for subscription ${event.subscriptionId}")
        
        // Send confirmation email, update analytics, etc.
    }
}
```

### 2. Handling Multiple Event Types

You can observe base event types to handle multiple events:

```kotlin
/**
 * Log all person events for auditing
 */
fun auditPersonEvents(@Observes event: PersonEvent) {
    when (event) {
        is PersonEvent.PersonCreated -> logger.info("Person created: ${event.personId}")
        is PersonEvent.PersonUpdated -> logger.info("Person updated: ${event.personId}")
        is PersonEvent.PersonDeleted -> logger.info("Person deleted: ${event.personId}")
        is PersonEvent.ContactVerified -> {
            when (event) {
                is PersonEvent.ContactVerified.EmailVerified -> 
                    logger.info("Email verified: ${event.email.value}")
                is PersonEvent.ContactVerified.PhoneVerified -> 
                    logger.info("Phone verified: ${event.phone.value}")
            }
        }
        is PersonEvent.SubscriptionRenewed -> 
            logger.info("Subscription renewed: ${event.subscriptionId}")
    }
}
```

## Best Practices

### Event Design

1. **Immutable Events**: Make events immutable data classes
2. **Meaningful Names**: Use past tense for event names (e.g., `PersonCreated`, not `CreatePerson`)
3. **Include Necessary Context**: Include all data needed by subscribers
4. **Avoid Circular Dependencies**: Don't include references to mutable objects

### Publishing

1. **Transactional Boundaries**: Publish events after the transaction is committed
2. **Idempotent Handlers**: Design event handlers to be idempotent (can be processed multiple times safely)
3. **Error Handling**: Handle exceptions in event handlers to prevent affecting the main flow

### Subscribing

1. **Single Responsibility**: Each subscriber should have a clear, focused purpose
2. **Performance Considerations**: Use `@ObservesAsync` for long-running operations
3. **Avoid Side Effects**: Subscribers should not modify the event or its data

## Examples

### Example 1: Adding a Payment Event

```kotlin
// 1. Define the event
sealed class PaymentEvent(eventType: String) : BaseDomainEvent(eventType) {
    data class PaymentReceived(
        val paymentId: String,
        val amount: BigDecimal,
        val personId: Long
    ) : PaymentEvent("payment.received")
    
    data class PaymentFailed(
        val paymentId: String,
        val personId: Long,
        val reason: String
    ) : PaymentEvent("payment.failed")
}

// 2. Update EventPublisher (if needed)
// Add a new field and update the when expression

// 3. Create a subscriber
@ApplicationScoped
class PaymentEventListener {
    @Inject
    lateinit var notificationService: NotificationService
    
    fun onPaymentReceived(@Observes event: PaymentEvent.PaymentReceived) {
        // Process successful payment
    }
    
    fun onPaymentFailed(@ObservesAsync event: PaymentEvent.PaymentFailed) {
        // Handle failed payment
        notificationService.sendPaymentFailureNotification(event.personId, event.reason)
    }
}

// 4. Publish the event
val event = PaymentEvent.PaymentReceived(
    paymentId = payment.id,
    amount = payment.amount,
    personId = payment.personId
)
eventPublisher.publish(event)
```

### Example 2: Subscribing to Multiple Event Types

```kotlin
@ApplicationScoped
class AnalyticsEventListener {
    @Inject
    lateinit var analyticsService: AnalyticsService
    
    // Track all domain events for analytics
    fun trackEvent(@ObservesAsync event: DomainEvent) {
        val eventData = when (event) {
            is PersonEvent -> mapOf(
                "category" to "person",
                "action" to event.eventType,
                "personId" to when (event) {
                    is PersonEvent.PersonCreated -> event.personId
                    is PersonEvent.PersonUpdated -> event.personId
                    is PersonEvent.PersonDeleted -> event.personId
                    is PersonEvent.ContactVerified -> event.personId
                    else -> null
                }
            )
            is PaymentEvent -> mapOf(
                "category" to "payment",
                "action" to event.eventType,
                "paymentId" to when (event) {
                    is PaymentEvent.PaymentReceived -> event.paymentId
                    is PaymentEvent.PaymentFailed -> event.paymentId
                    else -> null
                }
            )
            else -> mapOf(
                "category" to "other",
                "action" to event.eventType
            )
        }
        
        analyticsService.trackEvent(eventData)
    }
}
```

By following these guidelines, you can extend the event system to handle new types of events while maintaining a clean, decoupled architecture.
