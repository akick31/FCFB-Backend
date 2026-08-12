package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.VenueService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/venue")
class VenueController(
    private val venueService: VenueService,
) {
    @Operation(summary = "List all venues")
    @GetMapping("/all")
    fun getAllVenues() = venueService.getAllVenues()

    @Operation(summary = "Delete a venue")
    @DeleteMapping("/{name}")
    fun deleteVenue(
        @PathVariable name: String,
    ) = venueService.deleteByName(name)
}
