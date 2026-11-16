package com.fcfb.arceus.dto.request

data class ContactRequest(
    val name: String,
    val email: String,
    val subject: String,
    val message: String,
    val company: String? = null,
)
