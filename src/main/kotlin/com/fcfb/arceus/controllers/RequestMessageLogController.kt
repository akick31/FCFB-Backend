package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.RequestMessageLog
import com.fcfb.arceus.service.log.RequestMessageLogService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/request_message_log")
class RequestMessageLogController(
    private val requestMessageLogService: RequestMessageLogService,
) {
    @Operation(summary = "Log a request message")
    @PostMapping("")
    fun logRequestMessage(
        @RequestBody requestMessageLog: RequestMessageLog,
    ) = requestMessageLogService.logRequestMessage(requestMessageLog)
}
