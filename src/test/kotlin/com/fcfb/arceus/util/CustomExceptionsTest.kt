package com.fcfb.arceus.util

import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.exception.team.TeamNotFoundException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CustomExceptionsTest {
    @Test
    fun `test exceptions with special characters in message`() {
        val specialMessage = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"

        val teamException = TeamNotFoundException(specialMessage)
        assertEquals(specialMessage, teamException.message)
        assertEquals("TeamNotFoundException: $specialMessage", teamException.toString())

        val gameException = GameNotFoundException(specialMessage)
        assertEquals(specialMessage, gameException.message)
        assertEquals("GameNotFoundException: $specialMessage", gameException.toString())
    }

    @Test
    fun `test exceptions with unicode characters in message`() {
        val unicodeMessage = "Unicode: 你好世界 🌍 🚀 🎉"

        val teamException = TeamNotFoundException(unicodeMessage)
        assertEquals(unicodeMessage, teamException.message)
        assertEquals("TeamNotFoundException: $unicodeMessage", teamException.toString())

        val gameException = GameNotFoundException(unicodeMessage)
        assertEquals(unicodeMessage, gameException.message)
        assertEquals("GameNotFoundException: $unicodeMessage", gameException.toString())
    }

    @Test
    fun `test exceptions with very long message`() {
        val longMessage = "A".repeat(10000)

        val teamException = TeamNotFoundException(longMessage)
        assertEquals(longMessage, teamException.message)
        assertEquals("TeamNotFoundException: $longMessage", teamException.toString())

        val gameException = GameNotFoundException(longMessage)
        assertEquals(longMessage, gameException.message)
        assertEquals("GameNotFoundException: $longMessage", gameException.toString())
    }

    @Test
    fun `test exceptions with newlines in message`() {
        val newlineMessage = "Line 1\nLine 2\nLine 3"

        val teamException = TeamNotFoundException(newlineMessage)
        assertEquals(newlineMessage, teamException.message)
        assertEquals("TeamNotFoundException: $newlineMessage", teamException.toString())

        val gameException = GameNotFoundException(newlineMessage)
        assertEquals(newlineMessage, gameException.message)
        assertEquals("GameNotFoundException: $newlineMessage", gameException.toString())
    }

    @Test
    fun `test exceptions with tabs in message`() {
        val tabMessage = "Tab\tseparated\tvalues"

        val teamException = TeamNotFoundException(tabMessage)
        assertEquals(tabMessage, teamException.message)
        assertEquals("TeamNotFoundException: $tabMessage", teamException.toString())

        val gameException = GameNotFoundException(tabMessage)
        assertEquals(tabMessage, gameException.message)
        assertEquals("GameNotFoundException: $tabMessage", gameException.toString())
    }
}
