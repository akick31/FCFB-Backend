package com.fcfb.arceus.service

import com.fcfb.arceus.enums.system.LogoKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import java.io.File

class FileUploadServiceTest {
    @TempDir
    lateinit var imagesDir: File

    private fun service() = FileUploadService(imagesDir.path, "https://api.example.com/")

    @Test
    fun `uploaded logo is stored under its kind and returned as a hosted url`() {
        val file = MockMultipartFile("file", "crest.png", "image/png", byteArrayOf(1, 2, 3))

        val url = service().uploadLogo(file, LogoKind.CONFERENCE).body!!.getValue("url")

        assertTrue(url.startsWith("https://api.example.com/images/conference-logos/"))
        assertTrue(url.endsWith(".png"))
        assertTrue(File(imagesDir, url.removePrefix("https://api.example.com/images/")).isFile)
    }

    @Test
    fun `non image upload is rejected`() {
        val file = MockMultipartFile("file", "notes.txt", "text/plain", byteArrayOf(1))

        assertEquals(HttpStatus.BAD_REQUEST, service().uploadLogo(file, LogoKind.TEAM).statusCode)
    }
}
