package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.RankingMetricComputeResult
import com.fcfb.arceus.dto.response.RankingMetricResponse
import com.fcfb.arceus.service.fcfb.RankingMetricService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<List<RankingMetricResponse>> = ResponseEntity.ok(rankingMetricService.getMetrics(season, week, metricType))

    @Operation(summary = "Get available weeks for a ranking metric")
    @GetMapping("/weeks")
    fun getWeeks(
        @RequestParam("season") season: Int,
        @RequestParam("metricType") metricType: String,
    ): ResponseEntity<List<Int>> = ResponseEntity.ok(rankingMetricService.getAvailableWeeks(season, metricType))

    @Operation(summary = "Get a team's ranking metric history for a season")
    @GetMapping("/history")
    fun getHistory(
        @RequestParam("season") season: Int,
        @RequestParam("team") team: String,
        @RequestParam("metricType") metricType: String,
    ): ResponseEntity<List<RankingMetricResponse>> = ResponseEntity.ok(rankingMetricService.getMetricHistory(season, team, metricType))

    @Operation(summary = "Compute ranking metrics for a week")
    @PostMapping("/compute")
    fun compute(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<RankingMetricComputeResult> = ResponseEntity.ok(rankingMetricService.computeMetrics(season, week))

    @Operation(summary = "Backfill ranking metrics for every completed week of a season")
    @PostMapping("/backfill")
    fun backfill(
        @RequestParam("season") season: Int,
    ): ResponseEntity<List<RankingMetricComputeResult>> = ResponseEntity.ok(rankingMetricService.backfillSeason(season))

    @Operation(summary = "Get every week with at least one completed game for a season")
    @GetMapping("/valid-weeks")
    fun getValidWeeks(
        @RequestParam("season") season: Int,
    ): ResponseEntity<List<Int>> = ResponseEntity.ok(rankingMetricService.getValidWeeks(season))
}
