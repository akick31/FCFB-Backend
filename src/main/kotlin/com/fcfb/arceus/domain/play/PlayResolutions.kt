package com.fcfb.arceus.domain.play

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play

class PlayResolutions {
    fun resolveRunoffTime(
        game: Game,
        clockStopped: Boolean?,
        clock: Int,
        timeoutUsed: Boolean,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensivePlaybook: OffensivePlaybook,
    ): Int {
        return if (!clockStopped!! && !timeoutUsed) {
            when {
                playCall == PlayCall.SPIKE -> 3
                playCall == PlayCall.KNEEL -> 40
                runoffType == RunoffType.HURRY -> 7
                runoffType == RunoffType.FINAL ->
                    if (clock <= 7) {
                        clock
                    } else if (clock > 30) {
                        30
                    } else {
                        clock - 1
                    }
                runoffType == RunoffType.NORMAL ->
                    when (offensivePlaybook) {
                        OffensivePlaybook.PRO -> 15
                        OffensivePlaybook.AIR_RAID -> 10
                        OffensivePlaybook.FLEXBONE -> 20
                        OffensivePlaybook.SPREAD -> 13
                        OffensivePlaybook.WEST_COAST -> 17
                    }
                runoffType == RunoffType.CHEW -> 30
                game.gameMode == GameMode.CHEW -> 30
                offensivePlaybook == OffensivePlaybook.PRO -> 15
                offensivePlaybook == OffensivePlaybook.AIR_RAID -> 10
                offensivePlaybook == OffensivePlaybook.FLEXBONE -> 20
                offensivePlaybook == OffensivePlaybook.SPREAD -> 13
                offensivePlaybook == OffensivePlaybook.WEST_COAST -> 17
                else -> 0
            }
        } else {
            when {
                playCall == PlayCall.SPIKE -> 1
                else -> 0
            }
        }
    }

    fun resolvePlaybooks(
        game: Game,
        possession: TeamSide,
    ): Pair<OffensivePlaybook, DefensivePlaybook> {
        return if (possession == TeamSide.HOME) {
            game.homeOffensivePlaybook to game.awayDefensivePlaybook
        } else {
            game.awayOffensivePlaybook to game.homeDefensivePlaybook
        }
    }

    fun resolveTimeoutAttribution(
        game: Game,
        gamePlay: Play,
        offensiveTimeoutCalled: Boolean,
    ): Triple<Boolean, Boolean, Boolean> {
        val clockStopped = game.clockStopped
        val defensiveTimeoutCalled = gamePlay.timeoutUsed
        var timeoutUsed = false
        var homeTimeoutCalled = false
        var awayTimeoutCalled = false
        if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME && game.awayTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY && game.homeTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME && game.homeTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY && game.awayTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            awayTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME) {
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY) {
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME) {
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY) {
            homeTimeoutCalled = true
        }
        return Triple(timeoutUsed, homeTimeoutCalled, awayTimeoutCalled)
    }
}
