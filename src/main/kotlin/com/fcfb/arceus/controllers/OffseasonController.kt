package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Offseason
import com.fcfb.arceus.service.fcfb.OffseasonService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/offseason")
class OffseasonController(
    private val offseasonService: OffseasonService,
) {
    @GetMapping
    fun getCurrentOffseason(): ResponseEntity<Offseason?> = ResponseEntity.ok(offseasonService.getCurrentOffseason())
}
