package com.fcfb.arceus.exception

import com.fcfb.arceus.exception.discord.DiscordUserNotFoundException
import com.fcfb.arceus.exception.discord.UnableToCreateGameThreadException
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.exception.game.GameWeekJobNotFoundException
import com.fcfb.arceus.exception.game.InvalidCoinTossChoiceException
import com.fcfb.arceus.exception.game.NoGameFoundException
import com.fcfb.arceus.exception.play.PlayNotFoundException
import com.fcfb.arceus.exception.ranking.InvalidRankingMetricException
import com.fcfb.arceus.exception.ranking.InvalidRankingsException
import com.fcfb.arceus.exception.ranking.MetricNotImplementedException
import com.fcfb.arceus.exception.schedule.InvalidConferenceException
import com.fcfb.arceus.exception.schedule.ScheduleNotFoundException
import com.fcfb.arceus.exception.season.CurrentSeasonNotFoundException
import com.fcfb.arceus.exception.season.CurrentWeekNotFoundException
import com.fcfb.arceus.exception.season.RankingsNotUploadedException
import com.fcfb.arceus.exception.season.SeasonNotReadyException
import com.fcfb.arceus.exception.signup.InvalidNewSignupException
import com.fcfb.arceus.exception.signup.NewSignupNotFoundException
import com.fcfb.arceus.exception.statistics.GameStatsNotFoundException
import com.fcfb.arceus.exception.statistics.InvalidGameStatsRequestException
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.exception.team.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.exception.team.NoCoachesFoundException
import com.fcfb.arceus.exception.team.TeamNotFoundException
import com.fcfb.arceus.exception.team.TooManyCoachesException
import com.fcfb.arceus.exception.user.DiscordAlreadyLinkedException
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.exception.user.UserNotFoundException
import com.fcfb.arceus.exception.user.UserUnauthorizedException
import com.fcfb.arceus.util.Logger
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
        InvalidUniformException::class,
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
            .body(mapOf("error" to "An unexpected error occurred"))
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
