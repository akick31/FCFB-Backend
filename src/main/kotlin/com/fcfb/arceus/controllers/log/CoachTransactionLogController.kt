package com.fcfb.arceus.controllers.log

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.model.log.CoachTransactionLog
import com.fcfb.arceus.service.log.CoachTransactionLogService
import io.swagger.v3.oas.annotations.Operation
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
    @Operation(summary = "List coach transactions")
    @GetMapping
    fun getEntireCoachTransactionLog(): List<CoachTransactionLog> = coachTransactionLogService.getEntireCoachTransactionLog()
}
