package com.sthompson.dto

import com.sthompson.domain.Email
import com.sthompson.domain.Phone

data class PersonDto(
    val id: Long? = null,
    val name: String,
    val email: Email? = null,
    val phone: Phone? = null
) {
    // Empty constructor for Jackson
    constructor() : this(null, "", null, null)
}