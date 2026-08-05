package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.HealthService
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
    @GetMapping("")
    fun healthCheck(): ResponseEntity<String> = healthService.checkHealth()
}
