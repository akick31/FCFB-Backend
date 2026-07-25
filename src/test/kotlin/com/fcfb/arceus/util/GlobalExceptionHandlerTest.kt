package com.fcfb.arceus.util

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerTest {
    private lateinit var globalExceptionHandler: GlobalExceptionHandler

    @BeforeEach
    fun setup() {
        globalExceptionHandler = GlobalExceptionHandler()
    }

    @Test
    fun `test handleException with RuntimeException`() {
        val exception = RuntimeException("Test runtime exception")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Test runtime exception", response.body?.get("error"))
    }

    @Test
    fun `test handleException with IllegalArgumentException`() {
        val exception = IllegalArgumentException("Invalid argument provided")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Invalid argument provided", response.body?.get("error"))
    }

    @Test
    fun `test handleException with NullPointerException`() {
        val exception = NullPointerException("Object is null")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Object is null", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception that has null message`() {
        val exception = RuntimeException()
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An unexpected error occurred", response.body?.get("error"))
    }

    @Test
    fun `test handleException with custom exception`() {
        class CustomException(message: String) : Exception(message)

        val exception = CustomException("Custom error message")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Custom error message", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing special characters`() {
        val exception = RuntimeException("Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing unicode characters`() {
        val exception = RuntimeException("Unicode: 你好世界 🌍 🚀 🎉")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Unicode: 你好世界 🌍 🚀 🎉", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing very long message`() {
        val longMessage = "A".repeat(10000)
        val exception = RuntimeException(longMessage)
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(longMessage, response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing newlines`() {
        val exception = RuntimeException("Line 1\nLine 2\nLine 3")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Line 1\nLine 2\nLine 3", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing tabs`() {
        val exception = RuntimeException("Tab\tseparated\tvalues")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Tab\tseparated\tvalues", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing HTML-like content`() {
        val exception = RuntimeException("<html><body><h1>Error</h1><p>Something went wrong</p></body></html>")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("<html><body><h1>Error</h1><p>Something went wrong</p></body></html>", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing JSON-like content`() {
        val exception = RuntimeException("{\"error\": \"Something went wrong\", \"code\": 500}")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("{\"error\": \"Something went wrong\", \"code\": 500}", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing SQL-like content`() {
        val exception = RuntimeException("SELECT * FROM users WHERE id = 1; DROP TABLE users;")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("SELECT * FROM users WHERE id = 1; DROP TABLE users;", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing XML-like content`() {
        val exception = RuntimeException("<error><message>Something went wrong</message><code>500</code></error>")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("<error><message>Something went wrong</message><code>500</code></error>", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing URL-like content`() {
        val exception = RuntimeException("https://example.com/api/error?code=500&message=Something%20went%20wrong")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("https://example.com/api/error?code=500&message=Something%20went%20wrong", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing email-like content`() {
        val exception = RuntimeException("user@example.com")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("user@example.com", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing phone-like content`() {
        val exception = RuntimeException("+1-555-123-4567")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("+1-555-123-4567", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing date-like content`() {
        val exception = RuntimeException("2024-01-01T12:00:00Z")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("2024-01-01T12:00:00Z", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing number-like content`() {
        val exception = RuntimeException("1234567890")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("1234567890", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing decimal-like content`() {
        val exception = RuntimeException("3.14159265359")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("3.14159265359", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing boolean-like content`() {
        val exception = RuntimeException("true")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("true", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing array-like content`() {
        val exception = RuntimeException("[1, 2, 3, 4, 5]")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("[1, 2, 3, 4, 5]", response.body?.get("error"))
    }

    @Test
    fun `test handleException with exception containing object-like content`() {
        val exception = RuntimeException("{name: \"John\", age: 30, city: \"New York\"}")
        val response = globalExceptionHandler.handleException(exception)

        assertNotNull(response)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("{name: \"John\", age: 30, city: \"New York\"}", response.body?.get("error"))
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
