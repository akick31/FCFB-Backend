package com.fcfb.arceus.model.animation

import java.awt.image.BufferedImage

data class HelmetSprites(
    val facingRight: BufferedImage,
    val facingLeft: BufferedImage,
) {
    fun facing(right: Boolean): BufferedImage = if (right) facingRight else facingLeft
}
