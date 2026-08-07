package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.GenerationService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/generate")
@CrossOrigin(origins = ["*"])
class GenerationController(
    private val generationService: GenerationService,
) {
    @Operation(summary = "Generate all data")
    @PostMapping("/all")
    fun generateAll() = generationService.generateAll()
}
