package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.service.fcfb.GameFieldService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game-field")
class GameFieldController(
    private val gameFieldService: GameFieldService,
) {
    @Operation(summary = "Get a game's field appearance")
    @GetMapping("")
    fun getGameField(
        @RequestParam gameId: Int,
    ) = gameFieldService.getField(gameId)

    @Operation(summary = "Update a game's field appearance")
    @PutMapping("")
    fun updateGameField(
        @RequestParam gameId: Int,
        @RequestBody request: TeamFieldRequest,
    ) = gameFieldService.updateField(gameId, request)
}
