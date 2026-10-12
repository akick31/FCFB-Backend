package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.RankingMetricComputeResult
import com.fcfb.arceus.dto.response.RankingMetricResponse
import com.fcfb.arceus.dto.response.TeamRankingMetricResponse
import com.fcfb.arceus.dto.response.TeamRankingMetricTrendResponse
import com.fcfb.arceus.service.fcfb.ranking.RankingMetricService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/ranking-metric")
class RankingMetricController(
    private val rankingMetricService: RankingMetricService,
) {
    @Operation(summary = "Get ranking metrics by week")
    @GetMapping
    fun getMetrics(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("metricType") metricType: String,
    ): List<RankingMetricResponse> = rankingMetricService.getMetrics(season, week, metricType)

    @Operation(summary = "Get available weeks for a ranking metric")
    @GetMapping("/weeks")
    fun getWeeks(
        @RequestParam("season") season: Int,
        @RequestParam("metricType") metricType: String,
    ): List<Int> = rankingMetricService.getAvailableWeeks(season, metricType)

    @Operation(summary = "Get a team's ranking metric history for a season")
    @GetMapping("/history")
    fun getHistory(
        @RequestParam("season") season: Int,
        @RequestParam("team") team: String,
        @RequestParam("metricType") metricType: String,
    ): List<RankingMetricResponse> = rankingMetricService.getMetricHistory(season, team, metricType)

    @Operation(summary = "Get one team's value/rank/total across every ranking metric for a week")
    @GetMapping("/team")
    fun getTeamMetrics(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("teamId") teamId: Int,
    ): List<TeamRankingMetricResponse> = rankingMetricService.getTeamMetrics(season, week, teamId)

    @Operation(summary = "Get one team's rank/total across every ranking metric for every week of a season")
    @GetMapping("/team/trends")
    fun getTeamMetricTrends(
        @RequestParam("season") season: Int,
        @RequestParam("teamId") teamId: Int,
    ): List<TeamRankingMetricTrendResponse> = rankingMetricService.getTeamMetricTrends(season, teamId)

    @Operation(summary = "Compute ranking metrics for a week")
    @PostMapping("/compute")
    fun compute(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): RankingMetricComputeResult = rankingMetricService.computeMetrics(season, week)

    @Operation(summary = "Backfill ranking metrics for every completed week of a season")
    @PostMapping("/backfill")
    fun backfill(
        @RequestParam("season") season: Int,
    ): List<RankingMetricComputeResult> = rankingMetricService.backfillSeason(season)

    @Operation(summary = "Get every week with at least one completed game for a season")
    @GetMapping("/valid-weeks")
    fun getValidWeeks(
        @RequestParam("season") season: Int,
    ): List<Int> = rankingMetricService.getValidWeeks(season)
}
