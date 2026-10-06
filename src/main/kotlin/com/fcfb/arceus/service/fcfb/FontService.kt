package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.FontUploadRequest
import com.fcfb.arceus.dto.response.FontOption
import com.fcfb.arceus.model.CustomFont
import com.fcfb.arceus.repositories.CustomFontRepository
import com.fcfb.arceus.service.fcfb.animation.FontRegistry
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Service
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.LocalDateTime
import javax.annotation.PostConstruct

@Service
class FontService(
    private val customFontRepository: CustomFontRepository,
) {
    @PostConstruct
    fun loadAll() {
        customFontRepository.findAll().forEach { font ->
            runCatching { registerUrl(font.url, font.name) }
                .onFailure { Logger.error("Could not load custom font ${font.name}: ${it.message}") }
        }
    }

    fun list(): List<FontOption> = BUILT_IN + customFontRepository.findAll().map { FontOption(it.name, it.label) }.sortedBy { it.label }

    fun upload(request: FontUploadRequest): FontOption {
        val userId = AuthContext.currentUserId() ?: throw UserForbiddenException()
        if (!request.acknowledged) throw InvalidUniformException("You must confirm you have the right to share this font")
        val label = request.label.trim().takeIf { it.isNotBlank() } ?: throw InvalidUniformException("A font name is required")
        val url = request.url.trim().takeIf { it.isNotBlank() } ?: throw InvalidUniformException("A font URL is required")
        val name = uniqueName(label)
        val family = registerUrl(url, name)
        customFontRepository.save(
            CustomFont().apply {
                this.name = name
                this.label = label
                this.family = family
                this.url = url
                this.uploadedBy = userId
                this.createdAt = LocalDateTime.now()
            },
        )
        return FontOption(name, label)
    }

    private fun registerUrl(
        url: String,
        name: String,
    ): String {
        val font =
            runCatching { Font.createFont(Font.TRUETYPE_FONT, ByteArrayInputStream(download(url))) }
                .getOrElse { throw InvalidUniformException("That link is not a valid TrueType font") }
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)
        FontRegistry.register(name, font.family)
        return font.family
    }

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.getInputStream().use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_FONT_BYTES) throw InvalidUniformException("Font files must be under 1 MB")
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
    }

    private fun uniqueName(label: String): String {
        val base = "CUSTOM_" + label.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').take(48).ifBlank { "FONT" }
        if (!customFontRepository.existsById(base)) return base
        var suffix = 2
        while (customFontRepository.existsById("${base}_$suffix")) suffix++
        return "${base}_$suffix"
    }

    companion object {
        private const val MAX_FONT_BYTES = 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 8000
        private val BUILT_IN =
            listOf(
                FontOption("CLASSIC", "Classic"),
                FontOption("BLOCK", "Block"),
                FontOption("SANS", "Sans"),
                FontOption("SERIF", "Serif"),
                FontOption("SLAB", "Slab serif"),
                FontOption("CONDENSED", "Condensed"),
                FontOption("IMPACT", "Impact"),
                FontOption("GEORGIA", "Georgia"),
                FontOption("TYPEWRITER", "Typewriter"),
                FontOption("MONOSPACE", "Monospace"),
            )
    }
}
