package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.NewSignupService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/new_signups")
class NewSignupController(
    private var newSignupService: NewSignupService,
) {
    @GetMapping("")
    fun getNewSignups() = newSignupService.getNewSignups()

    @DeleteMapping("/{id}")
    fun deleteNewSignup(
        @PathVariable id: Long,
    ) = newSignupService.deleteNewSignupById(id)
}
