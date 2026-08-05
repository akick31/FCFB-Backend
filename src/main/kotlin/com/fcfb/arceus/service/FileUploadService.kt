package com.fcfb.arceus.service

import com.fcfb.arceus.util.Logger
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
) {
    companion object {
        private const val MAX_LOGO_SIZE_BYTES = 5 * 1024 * 1024
    }

    fun uploadPostseasonLogo(file: MultipartFile): ResponseEntity<Map<String, String>> {
        validatePostseasonLogo(file)?.let { return it }

        return try {
            val logosDir = ensureDirectory(File(ensureDirectory(File(imagesPath)), "postseason-logos"))
            val relativePath = "postseason-logos/${saveFile(file, logosDir)}"
            Logger.info("Uploaded postseason logo: $relativePath")
            ResponseEntity.ok(mapOf("url" to relativePath))
        } catch (e: Exception) {
            Logger.error("Error uploading postseason logo: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }

    private fun validatePostseasonLogo(file: MultipartFile): ResponseEntity<Map<String, String>>? {
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
            Logger.info("Created directory: ${dir.absolutePath}")
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
