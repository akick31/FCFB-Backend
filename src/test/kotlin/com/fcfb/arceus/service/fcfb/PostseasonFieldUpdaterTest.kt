package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.enums.game.PlayoffRound
import com.fcfb.arceus.model.PlayoffField
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.UserForbiddenException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class PostseasonFieldUpdaterTest {
    private val updater = PostseasonFieldUpdater()

    @AfterEach
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `an admin update keeps unsent required settings and clears unsent optional ones`() {
        signIn("ROLE_ADMIN")
        val field = PlayoffField().apply { goalPostColor = "#111111" }
        field.centerLogoUrl = "old.png"

        updater.applyTo(field, PostseasonFieldRequest(turfColor = "#00AA00", wallText = "CFP"))

        assertEquals("#00AA00", field.turfColor)
        assertEquals("#111111", field.goalPostColor)
        assertEquals("CFP", field.wallText)
        assertNull(field.centerLogoUrl)
    }

    @Test
    fun `a white wall is rejected`() {
        signIn("ROLE_ADMIN")

        assertThrows<InvalidUniformException> { updater.applyTo(PlayoffField(), PostseasonFieldRequest(wallColor = "#FFFFFF")) }
    }

    @Test
    fun `anyone below commissioner is forbidden`() {
        signIn("ROLE_USER")

        assertThrows<UserForbiddenException> { updater.applyTo(PlayoffField(), PostseasonFieldRequest(turfColor = "#00AA00")) }
    }

    @Test
    fun `playoff rounds match the labels the schedule writes`() {
        assertEquals(PlayoffRound.QUARTERFINAL, PlayoffRound.fromLabel("Quarterfinal"))
        assertEquals(PlayoffRound.NATIONAL_CHAMPIONSHIP, PlayoffRound.fromLabel(" national championship "))
        assertNull(PlayoffRound.fromLabel("Bowl"))
    }

    private fun signIn(role: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority(role)))
    }
}
