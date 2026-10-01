package com.fcfb.arceus.dto.request

/** A draft postseason field to render using the last-played matchup. [category] is BOWL, PLAYOFF, or CCG. */
data class PostseasonPreviewRequest(
    val category: String,
    val key: String,
    val bowl: BowlFieldRequest? = null,
    val postseason: PostseasonFieldRequest? = null,
)
