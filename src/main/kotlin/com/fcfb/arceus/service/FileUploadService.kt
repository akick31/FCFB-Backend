package com.fcfb.arceus.service

import com.fcfb.arceus.enums.system.LogoKind
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.hostedImagesUrl
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class FileUploadService(
    @Value("\${images.path:./images}") private val imagesPath: String,
    @Value("\${domain.url}") private val domainUrl: String,
) {
    companion object {
        private const val MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024
    }

    fun uploadLogo(
        file: MultipartFile,
        kind: LogoKind,
    ): ResponseEntity<Map<String, String>> {
        validateLogo(file)?.let { return it }

        return try {
            val logosDir = ensureDirectory(File(ensureDirectory(File(imagesPath)), kind.folder))
            val relativePath = "${kind.folder}/${saveFile(file, logosDir)}"
            Logger().info("Uploaded ${kind.name.lowercase()} logo: $relativePath")
            ResponseEntity.ok(mapOf("url" to hostedImagesUrl(domainUrl) + relativePath))
        } catch (e: Exception) {
            Logger().error("Error uploading ${kind.name.lowercase()} logo: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }

    private fun validateLogo(file: MultipartFile): ResponseEntity<Map<String, String>>? {
        val contentType = file.contentType
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "File must be an image"))
        }
        if (file.size > MAX_LOGO_SIZE_BYTES) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "File size must be less than 5MB"))
        }
        return null
    }

    private fun ensureDirectory(dir: File): File {
        if (!dir.exists()) {
            dir.mkdirs()
            Logger().info("Created directory: ${dir.absolutePath}")
        }
        return dir
    }

    private fun saveFile(
        file: MultipartFile,
        directory: File,
    ): String {
        val extension = (file.originalFilename ?: "logo").substringAfterLast('.', "")
        val uniqueFilename = "${UUID.randomUUID()}.$extension"
        Files.write(Paths.get(directory.absolutePath, uniqueFilename), file.bytes)
        return uniqueFilename
    }
}
