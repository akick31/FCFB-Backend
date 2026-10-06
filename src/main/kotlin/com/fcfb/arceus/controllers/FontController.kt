package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.FontUploadRequest
import com.fcfb.arceus.service.fcfb.FontService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/font")
class FontController(
    private val fontService: FontService,
) {
    @Operation(summary = "List the available display fonts")
    @GetMapping("")
    fun list() = fontService.list()

    @Operation(summary = "Upload a custom TrueType font")
    @PostMapping("")
    fun upload(
        @RequestBody request: FontUploadRequest,
    ) = fontService.upload(request)
}
