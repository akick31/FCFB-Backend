package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.ConferenceRequest
import com.fcfb.arceus.service.fcfb.ConferenceService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/conference")
@CrossOrigin(origins = ["*"])
class ConferenceController(
    private val conferenceService: ConferenceService,
) {
    @GetMapping
    fun getConferences() = conferenceService.getAll()

    @PostMapping
    fun createConference(
        @RequestBody request: ConferenceRequest,
    ) = conferenceService.create(
        request.code,
        request.label,
        request.logoUrl,
        request.logoUrlDark,
        request.abbreviation,
        request.displayOrder,
    )

    @PutMapping("/{code}")
    fun updateConference(
        @PathVariable code: String,
        @RequestBody request: ConferenceRequest,
    ) = conferenceService.update(code, request.label, request.logoUrl, request.logoUrlDark, request.abbreviation, request.displayOrder)

    @PutMapping("/{code}/active")
    fun setConferenceActive(
        @PathVariable code: String,
        @RequestParam active: Boolean,
    ) = conferenceService.setActive(code, active)
}
