package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.PylonStyle
import com.fcfb.arceus.enums.animation.ScoreboardShape
import com.fcfb.arceus.enums.animation.ScoreboardStyle
import java.awt.image.BufferedImage

data class JumbotronSettings(
    val style: ScoreboardStyle,
    val size: Float,
    val width: Float,
    val elevation: Float,
    val pylonMount: Boolean,
    val pylonStyle: PylonStyle,
    val pylonsOutside: Boolean,
    val shape: ScoreboardShape,
    val title: String,
    val logo: BufferedImage?,
)
