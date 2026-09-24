package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.BowlFieldRequest
import com.fcfb.arceus.service.fcfb.BowlFieldService
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
@RequestMapping("${ApiConstants.FULL_PATH}/bowl-field")
class BowlFieldController(
    private val bowlFieldService: BowlFieldService,
) {
    @Operation(summary = "List every bowl field")
    @GetMapping("/all")
    fun getAllBowlFields() = bowlFieldService.getAll()

    @Operation(summary = "Get a bowl field")
    @GetMapping("")
    fun getBowlField(
        @RequestParam bowl: String,
    ) = bowlFieldService.getField(bowl)

    @Operation(summary = "Update a bowl field")
    @PutMapping("")
    fun updateBowlField(
        @RequestParam bowl: String,
        @RequestBody request: BowlFieldRequest,
    ) = bowlFieldService.updateField(bowl, request)
}
