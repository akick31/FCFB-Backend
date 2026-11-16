package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.ContactRequest
import com.fcfb.arceus.service.email.ContactService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/contact")
class ContactController(
    private val contactService: ContactService,
) {
    @Operation(summary = "Send a contact message to the league")
    @PostMapping("")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendContactMessage(
        @RequestBody request: ContactRequest,
        servletRequest: HttpServletRequest,
    ) = contactService.sendContactMessage(request, servletRequest)
}
