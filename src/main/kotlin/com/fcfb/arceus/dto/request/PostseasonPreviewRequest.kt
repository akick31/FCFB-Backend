package com.fcfb.arceus.dto.request

data class PostseasonPreviewRequest(
    val category: String,
    val key: String,
    val bowl: BowlFieldRequest? = null,
    val postseason: PostseasonFieldRequest? = null,
)
