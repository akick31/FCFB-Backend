package com.fcfb.arceus.model.scorebug

import java.awt.Font
import java.awt.image.BufferedImage

data class PostseasonScorebugHeader(
    val logo: BufferedImage?,
    val logoWidth: Int,
    val logoReadSucceeded: Boolean,
    val titleFont: Font,
    val titleOffset: Int?,
)
