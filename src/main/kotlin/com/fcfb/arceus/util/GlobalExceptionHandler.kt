package com.fcfb.arceus.util

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CurrentSeasonNotFoundException::class)
    fun handleCurrentSeasonNotFound(e: Exception): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (e.message ?: HttpStatus.NOT_FOUND.reasonPhrase)))

    @ExceptionHandler(
        CurrentWeekNotFoundException::class,
        DiscordUserNotFoundException::class,
        GameNotFoundException::class,
        GameStatsNotFoundException::class,
        GameWeekJobNotFoundException::class,
        NewSignupNotFoundException::class,
        NoCoachDiscordIdsFoundException::class,
        NoCoachesFoundException::class,
        NoGameFoundException::class,
        PlayNotFoundException::class,
        ScheduleNotFoundException::class,
        TeamNotFoundException::class,
        UserNotFoundException::class,
    )
    fun handleNotFound(e: Exception): ResponseEntity<Map<String, String>> = errorResponse(HttpStatus.NOT_FOUND, e)

    @ExceptionHandler(
        InvalidCoinTossChoiceException::class,
        InvalidGameStatsRequestException::class,
        InvalidRankingsException::class,
        InvalidConferenceException::class,
        InvalidNewSignupException::class,
        MetricNotImplementedException::class,
        InvalidRankingMetricException::class,
    )
    fun handleBadRequest(e: Exception): ResponseEntity<Map<String, String>> = errorResponse(HttpStatus.BAD_REQUEST, e)

    @ExceptionHandler(UserUnauthorizedException::class)
    fun handleUnauthorized(e: Exception): ResponseEntity<Map<String, String>> = errorResponse(HttpStatus.UNAUTHORIZED, e)

    @ExceptionHandler(UserForbiddenException::class)
    fun handleForbidden(e: Exception): ResponseEntity<Map<String, String>> = errorResponse(HttpStatus.FORBIDDEN, e)

    @ExceptionHandler(
        RankingsNotUploadedException::class,
        TooManyCoachesException::class,
        DiscordAlreadyLinkedException::class,
        SeasonNotReadyException::class,
    )
    fun handleConflict(e: Exception): ResponseEntity<Map<String, String>> = errorResponse(HttpStatus.CONFLICT, e)

    @ExceptionHandler(UnableToCreateGameThreadException::class)
    fun handleDiscordUnavailable(e: Exception): ResponseEntity<Map<String, String>> {
        Logger.error("Discord rejected a game thread creation", e)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(mapOf("error" to (e.message ?: "Discord is unavailable")))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, String>> {
        Logger.error("Unhandled exception reached the controller boundary", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to (e.message ?: "An unexpected error occurred")))
    }

    private fun errorResponse(
        status: HttpStatus,
        e: Exception,
    ): ResponseEntity<Map<String, String>> {
        val message = e.message ?: status.reasonPhrase
        Logger.info("Request rejected with ${status.value()}: $message")
        return ResponseEntity.status(status).body(mapOf("error" to message))
    }
}
