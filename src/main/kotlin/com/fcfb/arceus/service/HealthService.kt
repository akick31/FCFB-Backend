package com.fcfb.arceus.service

import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class HealthService(
    private val healthEndpoint: HealthEndpoint,
) {
    fun checkHealth(): ResponseEntity<String> =
        if (healthEndpoint.health()?.status == Status.UP) {
            ResponseEntity.ok("Application is healthy")
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Application is unhealthy")
        }
}
