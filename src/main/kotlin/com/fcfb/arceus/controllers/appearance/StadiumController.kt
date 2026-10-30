package com.fcfb.arceus.controllers.appearance

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.dto.request.EventStadiumRequest
import com.fcfb.arceus.dto.request.GameStadiumRequest
import com.fcfb.arceus.dto.request.StadiumCreateRequest
import com.fcfb.arceus.dto.request.StadiumMergeRequest
import com.fcfb.arceus.dto.request.StadiumUpdateRequest
import com.fcfb.arceus.dto.request.TeamStadiumRequest
import com.fcfb.arceus.service.fcfb.appearance.StadiumGamesService
import com.fcfb.arceus.service.fcfb.appearance.StadiumHistoryService
import com.fcfb.arceus.service.fcfb.appearance.StadiumMergeService
import com.fcfb.arceus.service.fcfb.appearance.StadiumService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/stadium")
class StadiumController(
    private val stadiumService: StadiumService,
    private val stadiumMergeService: StadiumMergeService,
    private val stadiumHistoryService: StadiumHistoryService,
    private val stadiumGamesService: StadiumGamesService,
) {
    @Operation(summary = "List a stadium's changes, newest first, with the version before each change")
    @GetMapping("/history")
    fun history(
        @RequestParam name: String,
    ) = stadiumHistoryService.history(name)

    @Operation(summary = "Restore a stadium to how it looked before a change")
    @PutMapping("/history/revert")
    fun revert(
        @RequestParam id: Long,
    ) = stadiumHistoryService.revert(id)

    @Operation(summary = "List the games played at a stadium and each team's record there")
    @GetMapping("/games")
    fun games(
        @RequestParam name: String,
    ) = stadiumGamesService.games(name)

    @Operation(summary = "List the unfinished scheduled games at a stadium")
    @GetMapping("/upcoming")
    fun upcoming(
        @RequestParam name: String,
    ) = stadiumGamesService.upcoming(name)

    @Operation(summary = "List the countries a stadium can be in")
    @GetMapping("/countries")
    fun countries() = stadiumService.countries()

    @Operation(summary = "Merge a duplicate stadium into another, moving every team, game and event to it")
    @PutMapping("/merge")
    fun merge(
        @RequestBody request: StadiumMergeRequest,
    ) = stadiumMergeService.merge(request)

    @Operation(summary = "List stadiums, optionally including inactive ones")
    @GetMapping("/all")
    fun getAll(
        @RequestParam(defaultValue = "false") includeInactive: Boolean,
    ) = stadiumService.getAll(includeInactive)

    @Operation(summary = "Get a stadium by its id")
    @GetMapping("/by-id")
    fun getById(
        @RequestParam id: Long,
    ) = stadiumService.getById(id)

    @Operation(summary = "Get a stadium")
    @GetMapping("")
    fun get(
        @RequestParam name: String,
    ) = stadiumService.get(name)

    @Operation(summary = "Create a stadium, optionally copying another")
    @PostMapping("")
    fun create(
        @RequestBody request: StadiumCreateRequest,
    ) = stadiumService.create(request)

    @Operation(summary = "Edit a stadium's details, name or design")
    @PutMapping("")
    fun update(
        @RequestParam name: String,
        @RequestBody request: StadiumUpdateRequest,
    ) = stadiumService.update(name, request)

    @Operation(summary = "Retire or restore a stadium")
    @PutMapping("/active")
    fun setActive(
        @RequestParam name: String,
        @RequestParam active: Boolean,
    ) = stadiumService.setActive(name, active)

    @Operation(summary = "Point a team at a stadium, optionally building a new one")
    @PutMapping("/team")
    fun assignTeam(
        @RequestBody request: TeamStadiumRequest,
    ) = stadiumService.assignTeam(request)

    @Operation(summary = "Set the stadium a game is played in")
    @PutMapping("/game")
    fun assignGame(
        @RequestBody request: GameStadiumRequest,
    ) = stadiumService.assignGame(request)

    @Operation(summary = "Set the stadium for a bowl, playoff round or conference championship")
    @PutMapping("/event")
    fun assignEvent(
        @RequestBody request: EventStadiumRequest,
    ) = stadiumService.assignEvent(request)
}
