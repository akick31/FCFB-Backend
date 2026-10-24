package com.fcfb.arceus.dto.request

data class StadiumCreateRequest(
    val name: String,
    val city: String? = null,
    val state: String? = null,
    val capacity: Int? = null,
    val copyFrom: String? = null,
)
