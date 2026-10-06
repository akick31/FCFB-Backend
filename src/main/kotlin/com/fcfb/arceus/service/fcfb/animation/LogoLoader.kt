package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.BoundedCache
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.SafeUrl
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.imageio.ImageIO

object LogoLoader {
    private const val OPAQUE = 16
    private const val DEFAULT_IMAGES_PATH = "./images"
    private const val MAX_PIXELS = 4096L * 4096L
    private const val CONNECT_TIMEOUT_MS = 5000
    private const val READ_TIMEOUT_MS = 8000
    private const val CACHE_SIZE = 512

    private val cache = BoundedCache<String, BufferedImage>(CACHE_SIZE)

    /**
     * Uploaded logos are stored as paths relative to the images directory, which [URI.toURL] rejects as not absolute.
     * Set at startup so those resolve against the same directory the upload service writes to.
     */
    @Volatile
    var imagesPath: String = DEFAULT_IMAGES_PATH

    /** The first of these urls that loads, so a selected-but-unloadable logo (e.g. an SVG) falls back to a known-good one. */
    fun loadFirst(vararg urls: String?): BufferedImage? = urls.firstNotNullOfOrNull { load(it) }

    fun load(url: String?): BufferedImage? {
        if (url.isNullOrBlank()) return null
        cache.get(url)?.let { return it }
        return try {
            readBounded(url)?.let { trim(it) }?.also { cache.put(url, it) }
        } catch (e: IOException) {
            Logger.error("Error loading logo from $url: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Logger.error("Invalid logo url $url: ${e.message}")
            null
        }
    }

    /** Reads the image only after confirming its declared dimensions are sane, so a decompression bomb never fully decodes. */
    private fun readBounded(url: String): BufferedImage? {
        val stream = openStream(url) ?: return null
        stream.use { input ->
            ImageIO.createImageInputStream(input).use { imageStream ->
                val readers = ImageIO.getImageReaders(imageStream)
                if (!readers.hasNext()) return null
                val reader = readers.next()
                return try {
                    reader.input = imageStream
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    if (width <= 0 || height <= 0 || width.toLong() * height > MAX_PIXELS) null else reader.read(0)
                } finally {
                    reader.dispose()
                }
            }
        }
    }

    private fun openStream(url: String): InputStream? {
        val uri = URI(url)
        if (uri.isAbsolute) return openRemote(uri)
        val base = File(imagesPath).canonicalFile
        val file = File(base, url).canonicalFile
        if (file != base && !file.path.startsWith(base.path + File.separator)) {
            Logger.error("Rejected logo path outside the images directory: $url")
            return null
        }
        return if (file.isFile) file.inputStream() else null
    }

    private fun openRemote(uri: URI): InputStream? =
        try {
            SafeUrl.openStream(uri.toString(), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
        } catch (e: Exception) {
            Logger.error("Rejected or unreachable logo url $uri: ${e.message}")
            null
        }

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
}
