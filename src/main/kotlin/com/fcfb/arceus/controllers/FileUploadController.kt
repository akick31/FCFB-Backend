package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.FileUploadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/upload")
class FileUploadController(
    private val fileUploadService: FileUploadService,
) {
    @Operation(summary = "Upload a postseason logo image")
    @PostMapping("/postseason-logo")
    fun uploadPostseasonLogo(
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<Map<String, String>> = fileUploadService.uploadPostseasonLogo(file)
}
