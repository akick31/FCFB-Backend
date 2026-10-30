package com.fcfb.arceus.dto.request

import com.fcfb.arceus.enums.animation.PostseasonCategory
import com.fcfb.arceus.enums.animation.StadiumPreviewView

data class StadiumPreviewRequest(
    val venue: String? = null,
    val design: StadiumDesignRequest? = null,
    val team: String? = null,
    val opponent: String? = null,
    val category: PostseasonCategory? = null,
    val key: String? = null,
    val view: StadiumPreviewView = StadiumPreviewView.BOTH,
    val crowdFill: Double? = null,
)
