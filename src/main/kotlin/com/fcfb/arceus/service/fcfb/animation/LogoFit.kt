package com.fcfb.arceus.service.fcfb.animation

import java.awt.Graphics2D
import java.awt.image.BufferedImage

object LogoFit {
    fun draw(
        g: Graphics2D,
        logo: BufferedImage,
        centerX: Int,
        centerY: Int,
        box: Int,
        rotationDegrees: Float = 0f,
    ) {
        val scale = box.toFloat() / maxOf(logo.width, logo.height)
        val width = (logo.width * scale).toInt()
        val height = (logo.height * scale).toInt()
        if (rotationDegrees == 0f) {
            g.drawImage(logo, centerX - width / 2, centerY - height / 2, width, height, null)
            return
        }
        val saved = g.transform
        g.rotate(Math.toRadians(rotationDegrees.toDouble()), centerX.toDouble(), centerY.toDouble())
        g.drawImage(logo, centerX - width / 2, centerY - height / 2, width, height, null)
        g.transform = saved
    }
}
