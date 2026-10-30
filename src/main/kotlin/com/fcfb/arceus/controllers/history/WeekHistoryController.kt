package com.fcfb.arceus.controllers.history

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.service.fcfb.history.WeekHistoryService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/history")
class WeekHistoryController(
    private val weekHistoryService: WeekHistoryService,
) {
    @Operation(summary = "Weeks of a season with finished games")
    @GetMapping("/weeks")
    fun getFinishedWeeks(
        @RequestParam("season") season: Int,
    ) = weekHistoryService.finishedWeeks(season)

    @Operation(summary = "A week's finished games and highlights")
    @GetMapping("/week")
    fun getWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ) = weekHistoryService.week(season, week)
}
