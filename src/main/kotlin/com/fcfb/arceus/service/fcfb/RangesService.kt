package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Ranges
import com.fcfb.arceus.repositories.RangesRepository
import com.fcfb.arceus.util.ResultNotFoundException
import org.springframework.stereotype.Service

@Service
class RangesService(
    private val rangesRepository: RangesRepository,
) {
    fun getNormalResult(
        playCall: PlayCall,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        difference: Int,
    ): Ranges {
        return when (playCall) {
            PlayCall.SPIKE -> {
                Ranges(
                    PlayType.NORMAL.description,
                    offensivePlaybook.description,
                    defensivePlaybook.description,
                    0,
                    0,
                    0,
                    Scenario.SPIKE,
                    0,
                    0,
                    0,
                )
            }
            PlayCall.KNEEL -> {
                Ranges(
                    PlayType.NORMAL.description,
                    offensivePlaybook.description,
                    defensivePlaybook.description,
                    0,
                    0,
                    0,
                    Scenario.KNEEL,
                    1,
                    0,
                    0,
                )
            }
            else -> {
                rangesRepository.getNormalResult(
                    playCall.description,
                    offensivePlaybook.description,
                    defensivePlaybook.description,
                    difference.toString(),
                ) ?: throw ResultNotFoundException()
            }
        }
    }

    fun getNonNormalResult(
        playCall: PlayCall,
        difference: Int,
    ) = rangesRepository.getNonNormalResult(
        playCall.description,
        difference.toString(),
    ) ?: throw ResultNotFoundException()

    fun getFieldGoalResult(
        playCall: PlayCall,
        distance: Int,
        difference: Int,
    ) = rangesRepository.getFieldGoalResult(
        playCall.description,
        distance.toString(),
        difference.toString(),
    ) ?: throw ResultNotFoundException()

    fun getPuntResult(
        playCall: PlayCall,
        ballLocation: Int,
        difference: Int,
    ) = rangesRepository.getPuntResult(
        playCall.description,
        ballLocation.toString(),
        difference.toString(),
    ) ?: throw ResultNotFoundException()

    fun getPlayTime(
        playCall: PlayCall,
        yards: Int,
    ) = rangesRepository.getPlayTime(playCall.description, yards) ?: throw ResultNotFoundException()
}
