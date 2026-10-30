package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.enums.animation.PostseasonCategory
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.PostseasonField
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.repositories.game.GameRepository
import org.springframework.stereotype.Service

@Service
class PostseasonSampleGameService(
    private val gameRepository: GameRepository,
    private val bowlFieldService: BowlFieldService,
    private val playoffFieldService: PlayoffFieldService,
    private val conferenceChampionshipFieldService: ConferenceChampionshipFieldService,
) {
    fun lastGame(
        category: PostseasonCategory,
        key: String,
    ): Game? =
        when (category) {
            PostseasonCategory.BOWL -> gameRepository.getLastBowlGame(key)
            PostseasonCategory.PLAYOFF ->
                if (isNationalChampionship(key)) {
                    gameRepository.getLastNationalChampionshipGame()
                } else {
                    gameRepository.getLastPlayoffGame(key)
                }
            PostseasonCategory.CCG -> gameRepository.getLastConferenceChampionshipGame(key)
        }

    fun stagedGame(
        category: PostseasonCategory,
        key: String,
        home: String,
        away: String,
    ): Game =
        Game().apply {
            homeTeam = home
            awayTeam = away
            postseasonGameName = key
            gameType =
                when (category) {
                    PostseasonCategory.BOWL -> GameType.BOWL
                    PostseasonCategory.CCG -> GameType.CONFERENCE_CHAMPIONSHIP
                    PostseasonCategory.PLAYOFF ->
                        if (isNationalChampionship(key)) GameType.NATIONAL_CHAMPIONSHIP else GameType.PLAYOFFS
                }
        }

    fun bowlField(
        category: PostseasonCategory,
        key: String,
    ): BowlField? = if (category == PostseasonCategory.BOWL) bowlFieldService.getField(key) else null

    fun postseasonField(
        category: PostseasonCategory,
        key: String,
    ): PostseasonField? =
        when (category) {
            PostseasonCategory.BOWL -> null
            PostseasonCategory.PLAYOFF -> playoffFieldService.getField(key)
            PostseasonCategory.CCG -> conferenceChampionshipFieldService.getField(key)
        }

    private fun isNationalChampionship(key: String) = key.equals(NATIONAL_CHAMPIONSHIP, ignoreCase = true)

    private companion object {
        const val NATIONAL_CHAMPIONSHIP = "National Championship"
    }
}
