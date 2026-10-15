package com.fcfb.arceus.controllers.season

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.model.season.Offseason
import com.fcfb.arceus.service.fcfb.season.OffseasonService
import io.swagger.v3.oas.annotations.Operation
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
    fun getCurrentOffseason(): Offseason? = offseasonService.getCurrentOffseason()

    @Operation(summary = "Start offseason")
    @PostMapping("/start")
    fun startOffseason(): Offseason = offseasonService.startOffseasonNow()

    @Operation(summary = "End offseason")
    @PostMapping("/end")
    fun endOffseason(): Offseason? = offseasonService.endOffseasonNow()
}
