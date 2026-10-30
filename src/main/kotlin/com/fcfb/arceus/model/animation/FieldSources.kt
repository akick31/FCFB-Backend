package com.fcfb.arceus.model.animation

import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.PostseasonField
import com.fcfb.arceus.model.appearance.StadiumDesign

data class FieldSources(
    val bowlField: BowlField?,
    val postseasonField: PostseasonField?,
    val stadium: StadiumDesign?,
)
