package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.AppearancePreviewRequest
import com.fcfb.arceus.service.fcfb.AppearancePreviewService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/appearance-preview")
class AppearancePreviewController(
    private val appearancePreviewService: AppearancePreviewService,
) {
    @Operation(summary = "Render a draft uniform or field without saving it")
    @PostMapping("")
    fun preview(
        @RequestBody request: AppearancePreviewRequest,
    ) = appearancePreviewService.preview(request)
}
