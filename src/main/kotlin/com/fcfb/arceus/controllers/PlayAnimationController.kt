package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.PlayAnimationService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/play-animation")
class PlayAnimationController(
    private val playAnimationService: PlayAnimationService,
) {
    @Operation(summary = "Get play animation")
    @GetMapping("")
    fun getPlayAnimationByPlayId(
        @RequestParam("playId") playId: Int,
    ) = playAnimationService.getPlayAnimationByPlayId(playId)
}
