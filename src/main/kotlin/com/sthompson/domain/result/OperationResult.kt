package com.sthompson.domain.result

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
