package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object LogoLoader {
    private const val OPAQUE = 16
    private const val DEFAULT_IMAGES_PATH = "./images"

    private val cache = ConcurrentHashMap<String, BufferedImage>()

    /**
     * Uploaded logos are stored as paths relative to the images directory, which [URI.toURL] rejects as not absolute.
     * Set at startup so those resolve against the same directory the upload service writes to.
     */
    @Volatile
    var imagesPath: String = DEFAULT_IMAGES_PATH

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
            ImageIO.read(locate(url))?.let { trim(it) }?.also { cache[url] = it }
        } catch (e: IOException) {
            Logger.error("Error loading logo from $url: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Logger.error("Invalid logo url $url: ${e.message}")
            null
        }
    }

    private fun locate(url: String): URL {
        val uri = URI(url)
        return if (uri.isAbsolute) uri.toURL() else File(imagesPath, url).toURI().toURL()
    }
}
