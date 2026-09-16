package com.fcfb.arceus.service.fcfb.animation

import java.awt.Graphics2D
import java.awt.image.BufferedImage

/** Bowl and playoff logos are rarely square, so logos are scaled to fit a square box instead of being stretched to fill it. */
object LogoFit {
    fun draw(
        g: Graphics2D,
        logo: BufferedImage,
        centerX: Int,
        centerY: Int,
        box: Int,
    ) {
        val scale = box.toFloat() / maxOf(logo.width, logo.height)
        val width = (logo.width * scale).toInt()
        val height = (logo.height * scale).toInt()
        g.drawImage(logo, centerX - width / 2, centerY - height / 2, width, height, null)
    }
}
