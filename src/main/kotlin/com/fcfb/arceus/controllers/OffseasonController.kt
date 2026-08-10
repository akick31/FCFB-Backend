package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Offseason
import com.fcfb.arceus.service.fcfb.OffseasonService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/offseason")
class OffseasonController(
    private val offseasonService: OffseasonService,
) {
    @Operation(summary = "Get current offseason")
    @GetMapping
    fun getCurrentOffseason(): ResponseEntity<Offseason?> = ResponseEntity.ok(offseasonService.getCurrentOffseason())

    @Operation(summary = "Start offseason")
    @PostMapping("/start")
    fun startOffseason(): ResponseEntity<Offseason> = ResponseEntity.ok(offseasonService.startOffseasonNow())

    @Operation(summary = "End offseason")
    @PostMapping("/end")
    fun endOffseason(): ResponseEntity<Offseason?> = ResponseEntity.ok(offseasonService.endOffseasonNow())
}
