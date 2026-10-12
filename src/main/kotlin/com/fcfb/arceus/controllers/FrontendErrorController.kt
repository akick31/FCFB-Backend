package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.FrontendErrorRequest
import com.fcfb.arceus.service.FrontendErrorService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/internal/frontend-errors")
class FrontendErrorController(
    private val frontendErrorService: FrontendErrorService,
) {
    @Operation(summary = "Report frontend error")
    @PostMapping("")
    fun reportError(
        @RequestBody request: FrontendErrorRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<Unit> = frontendErrorService.reportError(request, servletRequest)
}
