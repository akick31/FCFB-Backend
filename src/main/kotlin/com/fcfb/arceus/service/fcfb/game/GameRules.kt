package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.model.Play
import kotlin.math.abs

object GameRules {
    fun getDifference(
        offensiveNumber: Int,
        defesiveNumber: Int,
    ): Int {
        var difference = abs(defesiveNumber - offensiveNumber)
        if (difference > 750) {
            difference = 1500 - difference
        }
        return difference
    }

    fun convertClockToSeconds(clock: String): Int {
        val clockArray = clock.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val minutes = clockArray[0].toInt()
        val seconds = clockArray[1].toInt()
        return minutes * 60 + seconds
    }

    fun convertClockToString(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    fun isTouchdownPlay(actualResult: ActualResult?): Boolean {
        return actualResult == ActualResult.TOUCHDOWN ||
            actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN ||
            actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            actualResult == ActualResult.RETURN_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            actualResult == ActualResult.KICK_SIX
    }

    fun isOffensiveTouchdownPlay(actualResult: ActualResult?): Boolean {
        return actualResult == ActualResult.TOUCHDOWN ||
            actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN
    }

    fun isDefensiveTouchdownPlay(actualResult: ActualResult?): Boolean {
        return actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            actualResult == ActualResult.RETURN_TOUCHDOWN ||
            actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            actualResult == ActualResult.KICK_SIX
    }

    fun isEndOfOvertimeHalf(play: Play) =
        play.actualResult == ActualResult.GOOD ||
            play.actualResult == ActualResult.NO_GOOD ||
            play.actualResult == ActualResult.BLOCKED ||
            play.actualResult == ActualResult.SUCCESS ||
            play.actualResult == ActualResult.FAILED ||
            play.actualResult == ActualResult.DEFENSE_TWO_POINT ||
            play.actualResult == ActualResult.TURNOVER_ON_DOWNS ||
            play.actualResult == ActualResult.TURNOVER ||
            play.actualResult == ActualResult.TOUCHDOWN ||
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            play.actualResult == ActualResult.KICK_SIX ||
            play.actualResult == ActualResult.PUNT ||
            play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
            play.actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN ||
            play.actualResult == ActualResult.MUFFED_PUNT

    fun isGameMathmaticallyOver(
        play: Play,
        homeScore: Int,
        awayScore: Int,
    ) = (
        play.actualResult == ActualResult.TOUCHDOWN ||
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
            play.actualResult == ActualResult.KICK_SIX
    ) && (abs(homeScore - awayScore) <= 2 || abs(awayScore - homeScore) <= 2)
}
