package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.enums.animation.PostseasonCategory
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.animation.FieldSources
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.ConferenceChampionshipField
import com.fcfb.arceus.model.appearance.GameLook
import com.fcfb.arceus.model.appearance.PlayoffField
import com.fcfb.arceus.model.appearance.PostseasonField
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.repositories.appearance.GameLookRepository
import com.fcfb.arceus.service.fcfb.animation.FieldSourceService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.snapshotJson
import org.springframework.stereotype.Service

@Service
class GameLookService(
    private val gameLookRepository: GameLookRepository,
    private val fieldSourceService: FieldSourceService,
    private val teamService: TeamService,
) {
    fun capture(game: Game) {
        val id = game.gameId ?: return
        if (gameLookRepository.existsById(id)) return
        val sources = fieldSourceService.liveSources(game, teamService.getTeamByName(game.homeTeam))
        val look =
            GameLook().apply {
                gameId = id
                stadiumCaptured = sources.stadium != null
                sources.stadium?.copyInto(stadium)
                bowlFieldJson = sources.bowlField?.let { snapshotJson.writeValueAsString(it) }
                postseasonKind = kindOf(sources.postseasonField)
                postseasonFieldJson = sources.postseasonField?.let { snapshotJson.writeValueAsString(it) }
            }
        gameLookRepository.save(look)
    }

    fun sourcesFor(game: Game): FieldSources? {
        if (game.gameStatus != GameStatus.FINAL) return null
        val look = game.gameId?.let { gameLookRepository.findById(it).orElse(null) } ?: return null
        return FieldSources(
            bowlField = look.bowlFieldJson?.let { snapshotJson.readValue(it, BowlField::class.java) },
            postseasonField = look.postseasonFieldJson?.let { postseasonFieldOf(look.postseasonKind, it) },
            stadium = look.stadium.takeIf { look.stadiumCaptured },
        )
    }

    private fun kindOf(field: PostseasonField?): PostseasonCategory? =
        when (field) {
            is PlayoffField -> PostseasonCategory.PLAYOFF
            is ConferenceChampionshipField -> PostseasonCategory.CCG
            else -> null
        }

    private fun postseasonFieldOf(
        kind: PostseasonCategory?,
        stored: String,
    ): PostseasonField? =
        when (kind) {
            PostseasonCategory.PLAYOFF -> snapshotJson.readValue(stored, PlayoffField::class.java)
            PostseasonCategory.CCG -> snapshotJson.readValue(stored, ConferenceChampionshipField::class.java)
            else -> null
        }
}
