package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object LogoLoader {
    private const val OPAQUE = 16

    private val cache = ConcurrentHashMap<String, BufferedImage>()

    /** Logos arrive with different amounts of empty padding, which would otherwise make them render at different sizes. */
    private fun trim(logo: BufferedImage): BufferedImage {
        var left = logo.width
        var right = -1
        var top = logo.height
        var bottom = -1
        for (y in 0 until logo.height) {
            for (x in 0 until logo.width) {
                if (logo.getRGB(x, y) ushr 24 < OPAQUE) continue
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        if (right < left || bottom < top) return logo
        return logo.getSubimage(left, top, right - left + 1, bottom - top + 1)
    }

    fun load(url: String?): BufferedImage? {
        if (url.isNullOrBlank()) return null
        cache[url]?.let { return it }
        return try {
            ImageIO.read(URI(url).toURL())?.let { trim(it) }?.also { cache[url] = it }
        } catch (e: IOException) {
            Logger.error("Error loading logo from $url: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Logger.error("Invalid logo url $url: ${e.message}")
            null
        }
    }
}
