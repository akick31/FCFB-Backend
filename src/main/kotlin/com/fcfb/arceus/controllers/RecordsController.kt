package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.service.fcfb.RecordService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/records")
@CrossOrigin(origins = ["*"])
class RecordsController(
    private val recordService: RecordService,
) {
    @GetMapping
    fun getFilteredRecords(
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) recordType: RecordType?,
        @RequestParam(required = false) recordName: Stats?,
        @RequestParam(required = false) recordScope: RecordScope?,
        @RequestParam(required = false) scopeValue: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = recordService.getFilteredRecords(
        season = season,
        recordType = recordType,
        recordName = recordName,
        recordScope = recordScope,
        scopeValue = scopeValue,
        pageable = pageable,
    )

    @PostMapping("/generate/all")
    fun generateAllRecords() = recordService.generateAllRecords()

    @PostMapping("/generate/teams-and-conferences")
    fun generateTeamAndConferenceRecords() = recordService.generateTeamAndConferenceRecords()
}
