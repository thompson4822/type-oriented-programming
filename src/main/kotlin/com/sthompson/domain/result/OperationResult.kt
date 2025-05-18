package com.sthompson.domain.result

import com.sthompson.domain.Email
import com.sthompson.domain.Phone

/**
 * Sealed interface representing the result of an operation.
 * This provides a type-safe way to handle success and failure cases
 * without relying on exceptions for control flow.
 */
sealed interface OperationResult<out T> {
    /**
     * Execute different actions based on whether the result is a success or failure
     */
    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (FailureReason) -> R
    ): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(reason)
    }

    /**
     * Map the success value to a new value
     */
    fun <R> map(transform: (T) -> R): OperationResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(this.reason)
    }

    /**
     * Return the success value or a default value if this is a failure
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> defaultValue
    }

    /**
     * Return the success value or null if this is a failure
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    /**
     * Return true if this is a success
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Return true if this is a failure
     */
    fun isFailure(): Boolean = this is Failure

    /**
     * Execute an action if this is a success
     */
    fun onSuccess(action: (T) -> Unit): OperationResult<T> {
        if (this is Success) action(value)
        return this
    }

    /**
     * Execute an action if this is a failure
     */
    fun onFailure(action: (FailureReason) -> Unit): OperationResult<T> {
        if (this is Failure) action(reason)
        return this
    }

    companion object {
        /**
         * Create a success result with the given value
         */
        fun <T> success(value: T): OperationResult<T> = Success(value)

        /**
         * Create a failure result with the given reason
         */
        fun <T> failure(reason: FailureReason): OperationResult<T> = Failure(reason)

        /**
         * Create a failure result with the given message
         */
        fun <T> failure(message: String, cause: Throwable? = null): OperationResult<T> =
            Failure(FailureReason.Error(message, cause))

        /**
         * Create a not found failure result
         */
        fun <T> notFound(message: String): OperationResult<T> =
            Failure(FailureReason.NotFound(message))

        /**
         * Create a validation failure result
         */
        fun <T> validationFailure(message: String): OperationResult<T> =
            Failure(FailureReason.ValidationFailed(message))
    }
}

/**
 * Represents a successful operation with a result value.
 */
data class Success<out T>(val value: T) : OperationResult<T>

/**
 * Represents a failed operation with a reason for the failure.
 */
data class Failure<out T>(val reason: FailureReason) : OperationResult<T>

/**
 * Sealed class hierarchy representing different types of failure reasons.
 * This allows for exhaustive handling of all possible failure types.
 */
sealed class FailureReason {
    /**
     * General error with a message and optional cause
     */
    data class Error(val message: String, val cause: Throwable? = null) : FailureReason()

    /**
     * Resource not found error
     */
    data class NotFound(val message: String) : FailureReason()

    /**
     * Validation failure with details about what failed validation
     */
    data class ValidationFailed(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : FailureReason()

    /**
     * Unauthorized access attempt
     */
    data class Unauthorized(val message: String = "Unauthorized access") : FailureReason()

    /**
     * Forbidden access attempt (authenticated but not authorized)
     */
    data class Forbidden(val message: String = "Access forbidden") : FailureReason()

    /**
     * Conflict with existing resource (generic)
     */
    data class Conflict(val message: String) : FailureReason()

    /**
     * System is temporarily unavailable
     */
    data class ServiceUnavailable(val message: String = "Service temporarily unavailable") : FailureReason()

    /**
     * Domain-specific failure reasons for Person entities
     */
    sealed class PersonFailure : FailureReason() {
        /**
         * Email already exists for another person
         */
        data class EmailAlreadyExists(val email: Email) : PersonFailure() {
            override fun toString() = "Person with email ${email.value} already exists"
        }

        /**
         * Phone already exists for another person
         */
        data class PhoneAlreadyExists(val phone: Phone) : PersonFailure() {
            override fun toString() = "Person with phone ${phone.value} already exists"
        }

        /**
         * Email doesn't match the person's email
         */
        data class EmailMismatch(val expected: Email?, val actual: Email) : PersonFailure() {
            override fun toString() = "Email ${actual.value} does not match person's email ${expected?.value ?: "none"}"
        }

        /**
         * Phone doesn't match the person's phone
         */
        data class PhoneMismatch(val expected: Phone?, val actual: Phone) : PersonFailure() {
            override fun toString() = "Phone ${actual.value} does not match person's phone ${expected?.value ?: "none"}"
        }
    }

    /**
     * Domain-specific failure reasons for Organization entities
     */
    sealed class OrganizationFailure : FailureReason() {
        /**
         * Organization name already exists
         */
        data class NameAlreadyExists(val name: String) : OrganizationFailure() {
            override fun toString() = "Organization with name '$name' already exists"
        }

        /**
         * Person is already a member of the organization
         */
        data class AlreadyMember(val personId: Long, val organizationId: Long) : OrganizationFailure() {
            override fun toString() = "Person is already a member of this organization"
        }
    }
}
