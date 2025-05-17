package com.sthompson.domain.result

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
     * Conflict with existing resource
     */
    data class Conflict(val message: String) : FailureReason()
    
    /**
     * System is temporarily unavailable
     */
    data class ServiceUnavailable(val message: String = "Service temporarily unavailable") : FailureReason()
}
