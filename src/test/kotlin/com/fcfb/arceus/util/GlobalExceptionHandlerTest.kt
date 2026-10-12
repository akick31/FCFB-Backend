package com.fcfb.arceus.util

import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.exception.discord.DiscordUserNotFoundException
import com.fcfb.arceus.exception.discord.UnableToCreateGameThreadException
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.exception.game.InvalidCoinTossChoiceException
import com.fcfb.arceus.exception.game.NoGameFoundException
import com.fcfb.arceus.exception.play.PlayNotFoundException
import com.fcfb.arceus.exception.schedule.ScheduleNotFoundException
import com.fcfb.arceus.exception.season.CurrentSeasonNotFoundException
import com.fcfb.arceus.exception.season.CurrentWeekNotFoundException
import com.fcfb.arceus.exception.statistics.GameStatsNotFoundException
import com.fcfb.arceus.exception.team.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.exception.team.NoCoachesFoundException
import com.fcfb.arceus.exception.team.TeamNotFoundException
import com.fcfb.arceus.exception.team.TooManyCoachesException
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.exception.user.UserNotFoundException
import com.fcfb.arceus.exception.user.UserUnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {
    private val globalExceptionHandler = GlobalExceptionHandler()

    @Test
    fun `unexpected failures return a generic message without internal details`() {
        val failures =
            listOf(
                RuntimeException("SELECT secret FROM credentials WHERE account_id = 42"),
                IllegalStateException("Could not open /srv/private/application.properties"),
                IllegalArgumentException("Upstream request failed: https://internal.example/api?token=synthetic-token"),
                NullPointerException("Internal account object was null"),
                RuntimeException(""),
                RuntimeException(),
            )

        failures.forEach { exception ->
            val response = globalExceptionHandler.handleException(exception)

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
            assertEquals(mapOf("error" to "An unexpected error occurred"), response.body)
        }
    }

    @Test
    fun `domain lookup failures map to 404 and keep their message`() {
        val notFound =
            listOf(
                CurrentSeasonNotFoundException(),
                CurrentWeekNotFoundException(),
                DiscordUserNotFoundException(),
                GameNotFoundException("Game not found for Request Message ID: 123"),
                GameStatsNotFoundException("Could not find game stats for game 1 and team Ohio State"),
                NoCoachDiscordIdsFoundException(),
                NoCoachesFoundException(),
                NoGameFoundException(),
                PlayNotFoundException("No current play found for game 2708"),
                ScheduleNotFoundException("Opponent not found for Ohio State"),
                TeamNotFoundException("Team not found with name: colorado sate"),
                UserNotFoundException("User not found with Discord ID 123"),
            )

        notFound.forEach { exception ->
            val response = globalExceptionHandler.handleNotFound(exception)

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(exception.message, response.body?.get("error"))
        }
    }

    @Test
    fun `invalid input maps to 400`() {
        val response = globalExceptionHandler.handleBadRequest(InvalidCoinTossChoiceException("Invalid coin toss choice: foo"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid coin toss choice: foo", response.body?.get("error"))
    }

    @Test
    fun `bad credentials map to 401 and a forbidden action maps to 403`() {
        assertEquals(HttpStatus.UNAUTHORIZED, globalExceptionHandler.handleUnauthorized(UserUnauthorizedException()).statusCode)
        assertEquals(HttpStatus.FORBIDDEN, globalExceptionHandler.handleForbidden(UserForbiddenException()).statusCode)
    }

    @Test
    fun `state conflicts map to 409`() {
        val response = globalExceptionHandler.handleConflict(TooManyCoachesException())

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("Attempting to add too many coaches to a team", response.body?.get("error"))
    }

    @Test
    fun `a failed Discord thread maps to 502`() {
        val response = globalExceptionHandler.handleDiscordUnavailable(UnableToCreateGameThreadException())

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("Unable to create game thread in Discord", response.body?.get("error"))
    }
}
