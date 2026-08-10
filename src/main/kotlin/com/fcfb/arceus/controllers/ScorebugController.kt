package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.service.fcfb.ScorebugService
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort
import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/scorebug")
class ScorebugController(
    private var scorebugService: ScorebugService,
) {
    @Operation(summary = "Generate all scorebugs")
    @PostMapping("/generate/all")
    fun generateAllScorebugs() = scorebugService.generateAllScorebugs()

    @Operation(summary = "Get game scorebug")
    @GetMapping("")
    fun getScorebugByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = scorebugService.getScorebugByGameId(gameId)

    @Operation(summary = "Get latest game scorebug")
    @GetMapping("/latest")
    fun getLatestScorebugByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = scorebugService.getLatestScorebugByGameId(gameId)

    @Operation(summary = "Conference scorebugs by week")
    @GetMapping("/conference")
    fun getScorebugsForConference(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("conference") conference: String,
    ) = scorebugService.getScorebugsForConference(season, week, conference)

    @Operation(summary = "Filter scorebugs by game")
    @GetMapping("/filtered")
    fun getFilteredScorebugs(
        @RequestParam(required = false) filters: List<GameFilter>?,
        @RequestParam(required = false) category: GameCategory?,
        @RequestParam(defaultValue = "CLOSEST_TO_END") sort: GameSort,
        @RequestParam(required = false) conference: String?,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) week: Int?,
        @RequestParam(required = false) gameMode: GameMode?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = scorebugService.getFilteredScorebugs(
        filters = filters,
        category = category,
        conference = conference,
        season = season,
        week = week,
        gameMode = gameMode,
        sort = sort,
        pageable = pageable,
    )
}
