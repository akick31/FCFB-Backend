package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.service.fcfb.ScorebugService
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort
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
    @PostMapping("/generate/all")
    fun generateAllScorebugs() = scorebugService.generateAllScorebugs()

    @GetMapping("")
    fun getScorebugByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = scorebugService.getScorebugByGameId(gameId)

    @GetMapping("/latest")
    fun getLatestScorebugByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = scorebugService.getLatestScorebugByGameId(gameId)

    @GetMapping("/conference")
    fun getScorebugsForConference(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("conference") conference: Conference,
    ) = scorebugService.getScorebugsForConference(season, week, conference)

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
        filters = filters ?: emptyList(),
        category = category,
        conference = conference,
        season = season,
        week = week,
        gameMode = gameMode,
        sort = sort,
        pageable = pageable,
    )
}
