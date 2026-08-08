package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.UploadRankingsRequest
import com.fcfb.arceus.dto.response.RankingResponse
import com.fcfb.arceus.service.fcfb.RankingService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/ranking")
class RankingController(
    private val rankingService: RankingService,
) {
    @Operation(summary = "Get rankings by week")
    @GetMapping
    fun getRankings(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("pollType") pollType: String,
    ): ResponseEntity<List<RankingResponse>> = ResponseEntity.ok(rankingService.getRankings(season, week, pollType))

    @Operation(summary = "Get available ranked weeks")
    @GetMapping("/weeks")
    fun getWeeks(
        @RequestParam("season") season: Int,
        @RequestParam("pollType") pollType: String,
    ): ResponseEntity<List<Int>> = ResponseEntity.ok(rankingService.getAvailableWeeks(season, pollType))

    @Operation(summary = "Upload rankings")
    @PostMapping
    fun uploadRankings(
        @RequestBody request: UploadRankingsRequest,
    ): ResponseEntity<List<RankingResponse>> =
        ResponseEntity.ok(rankingService.uploadRankings(request.season, request.week, request.pollType, request.teams))
}
