package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.HealthService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/health")
class HealthController(
    private val healthService: HealthService,
) {
    @Operation(summary = "Check service health")
    @GetMapping("")
    fun healthCheck(): ResponseEntity<String> = healthService.checkHealth()
}
