package com.sthompson.domain.result

/**
 * Represents a successful operation with a result value.
 */
data class Success<out T>(val value: T) : OperationResult<T>
