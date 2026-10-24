package com.fcfb.arceus.dto.request

data class StadiumUpdateRequest(
    val name: String? = null,
    val city: String? = null,
    val state: String? = null,
    val capacity: Int? = null,
    val design: StadiumDesignRequest? = null,
)
