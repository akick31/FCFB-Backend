package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.service.log.CoachTransactionLogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/coach_transaction_log")
class CoachTransactionLogController(
    private val coachTransactionLogService: CoachTransactionLogService,
) {
    @GetMapping
    fun getEntireCoachTransactionLog(): ResponseEntity<List<CoachTransactionLog>> =
        ResponseEntity.ok(
            coachTransactionLogService.getEntireCoachTransactionLog(),
        )
    g
}
