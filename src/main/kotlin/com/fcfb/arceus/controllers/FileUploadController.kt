package com.fcfb.arceus.controllers

import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/upload")
class FileUploadController(
    @Value("\${images.path:./images}") private val imagesPath: String,
) {
    /**
     * Upload a postseason game logo
     * @param file The image file to upload
     * @return The URL/path to the uploaded file
     */
    @PostMapping("/postseason-logo")
    fun uploadPostseasonLogo(
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<Map<String, String>> {
        return try {
            // Validate file type
            val contentType = file.contentType
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "File must be an image"))
            }

            // Validate file size (max 5MB)
            val maxSize = 5 * 1024 * 1024 // 5MB
            if (file.size > maxSize) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "File size must be less than 5MB"))
            }

            // Create images directory if it doesn't exist
            val imagesDir = File(imagesPath)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
                Logger.info("Created images directory: ${imagesDir.absolutePath}")
            }

            // Create postseason-logos subdirectory
            val logosDir = File(imagesDir, "postseason-logos")
            if (!logosDir.exists()) {
                logosDir.mkdirs()
                Logger.info("Created postseason-logos directory: ${logosDir.absolutePath}")
            }

            // Generate unique filename
            val originalFilename = file.originalFilename ?: "logo"
            val extension = originalFilename.substringAfterLast('.', "")
            val uniqueFilename = "${UUID.randomUUID()}.$extension"
            val filePath = Paths.get(logosDir.absolutePath, uniqueFilename)

            // Save file
            Files.write(filePath, file.bytes)

            // Return the URL path (relative to images directory)
            val relativePath = "postseason-logos/$uniqueFilename"
            Logger.info("Uploaded postseason logo: $relativePath")

            ResponseEntity.ok(mapOf("url" to relativePath))
        } catch (e: Exception) {
            Logger.error("Error uploading postseason logo: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to upload file: ${e.message}"))
        }
    }
}
