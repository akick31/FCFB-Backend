package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.service.fcfb.ConferenceChampionshipFieldService
import com.fcfb.arceus.service.fcfb.PlayoffFieldService
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
@RequestMapping("${ApiConstants.FULL_PATH}/postseason-field")
class PostseasonFieldController(
    private val playoffFieldService: PlayoffFieldService,
    private val conferenceChampionshipFieldService: ConferenceChampionshipFieldService,
) {
    @Operation(summary = "List every playoff round field")
    @GetMapping("/playoff/all")
    fun getAllPlayoffFields() = playoffFieldService.getAll()

    @Operation(summary = "Get a playoff round field")
    @GetMapping("/playoff")
    fun getPlayoffField(
        @RequestParam round: String,
    ) = playoffFieldService.getField(round)

    @Operation(summary = "Update a playoff round field")
    @PutMapping("/playoff")
    fun updatePlayoffField(
        @RequestParam round: String,
        @RequestBody request: PostseasonFieldRequest,
    ) = playoffFieldService.updateField(round, request)

    @Operation(summary = "List every conference championship field")
    @GetMapping("/conference-championship/all")
    fun getAllConferenceChampionshipFields() = conferenceChampionshipFieldService.getAll()

    @Operation(summary = "Get a conference championship field")
    @GetMapping("/conference-championship")
    fun getConferenceChampionshipField(
        @RequestParam conference: String,
    ) = conferenceChampionshipFieldService.getField(conference)

    @Operation(summary = "Update a conference championship field")
    @PutMapping("/conference-championship")
    fun updateConferenceChampionshipField(
        @RequestParam conference: String,
        @RequestBody request: PostseasonFieldRequest,
    ) = conferenceChampionshipFieldService.updateField(conference, request)
}
