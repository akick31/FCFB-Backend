package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.exception.signup.InvalidNewSignupException
import com.fcfb.arceus.model.account.NewSignup
import com.fcfb.arceus.repositories.account.NewSignupRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class NewSignupServiceTest {
    private val repository = mockk<NewSignupRepository>()
    private val service = NewSignupService(mockk(), mockk(relaxed = true), mockk(), repository)

    init {
        every { repository.save(any()) } answers { firstArg() }
    }

    private fun signup(username: String) =
        NewSignup(
            username, "Coach", "coach.tag", "123", "coach@example.com", "", "password1",
            CoachPosition.HEAD_COACH, "", "Alpha", "Beta", "Gamma",
            OffensivePlaybook.AIR_RAID, DefensivePlaybook.FOUR_THREE, false, "", LocalDateTime.now(),
        )

    @ParameterizedTest
    @ValueSource(strings = ["<@789932609806401546>", "user:<@1156052929858502678>", "<#123>", "   "])
    fun `discord mention markup and blank usernames are rejected`(username: String) {
        assertThrows<InvalidNewSignupException> { service.createNewSignup(signup(username)) }
    }

    @Test
    fun `plain usernames keep spaces and symbols but lose surrounding whitespace`() {
        assertEquals("TheHuskerFan#3201", service.createNewSignup(signup("  TheHuskerFan#3201 ")).username)
        assertEquals("Coach Tortilla", service.createNewSignup(signup("Coach Tortilla")).username)
    }
}
