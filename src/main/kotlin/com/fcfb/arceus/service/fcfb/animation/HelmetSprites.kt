package com.fcfb.arceus.service.fcfb.animation

import java.awt.image.BufferedImage

data class HelmetSprites(
    val facingRight: BufferedImage,
    val facingLeft: BufferedImage,
) {
    fun facing(right: Boolean): BufferedImage = if (right) facingRight else facingLeft
}
