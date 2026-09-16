package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object LogoLoader {
    private val cache = ConcurrentHashMap<String, BufferedImage>()

    fun load(url: String?): BufferedImage? {
        if (url.isNullOrBlank()) return null
        cache[url]?.let { return it }
        return try {
            ImageIO.read(URI(url).toURL())?.also { cache[url] = it }
        } catch (e: IOException) {
            Logger.error("Error loading logo from $url: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Logger.error("Invalid logo url $url: ${e.message}")
            null
        }
    }
}
