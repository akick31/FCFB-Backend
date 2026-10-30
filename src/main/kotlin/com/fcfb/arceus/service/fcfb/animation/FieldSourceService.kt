package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.game.PlayoffRound
import com.fcfb.arceus.model.animation.FieldSources
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.PlayoffField
import com.fcfb.arceus.model.appearance.PostseasonField
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.appearance.BowlFieldRepository
import com.fcfb.arceus.repositories.appearance.ConferenceChampionshipFieldRepository
import com.fcfb.arceus.repositories.appearance.PlayoffFieldRepository
import com.fcfb.arceus.repositories.appearance.VenueRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import org.springframework.stereotype.Service

@Service
class FieldSourceService(
    private val bowlFieldRepository: BowlFieldRepository,
    private val conferenceChampionshipFieldRepository: ConferenceChampionshipFieldRepository,
    private val conferenceRepository: ConferenceRepository,
    private val playoffFieldRepository: PlayoffFieldRepository,
    private val venueRepository: VenueRepository,
) {
    fun liveSources(
        game: Game,
        homeTeam: Team,
    ): FieldSources {
        val style = FieldStyle.of(game.gameType)
        val bowlField = if (style == FieldStyle.BOWL) bowlFieldFor(game) else null
        val postseasonField = postseasonFieldFor(style, game, homeTeam)
        return FieldSources(bowlField, postseasonField, stadiumFor(game, homeTeam, bowlField, postseasonField))
    }

    fun stadiumFor(
        game: Game,
        homeTeam: Team,
        bowlField: BowlField?,
        postseasonField: PostseasonField?,
    ): StadiumDesign? {
        val eventVenue =
            when (FieldStyle.of(game.gameType)) {
                FieldStyle.BOWL -> bowlField?.venue
                FieldStyle.CONFERENCE_CHAMPIONSHIP ->
                    homeTeam.conference?.let { conferenceRepository.findById(it).orElse(null)?.championshipVenue }
                FieldStyle.PLAYOFF, FieldStyle.NATIONAL_CHAMPIONSHIP -> (postseasonField as? PlayoffField)?.venue
                FieldStyle.HOME_FIELD -> homeTeam.stadium
            }
        return designOf(game.venue) ?: designOf(eventVenue)
    }

    private fun postseasonFieldFor(
        style: FieldStyle,
        game: Game,
        homeTeam: Team,
    ): PostseasonField? =
        when (style) {
            FieldStyle.NATIONAL_CHAMPIONSHIP -> playoffFieldRepository.findById(PlayoffRound.NATIONAL_CHAMPIONSHIP.label).orElse(null)
            FieldStyle.PLAYOFF -> game.postseasonGameName?.let { playoffFieldRepository.findById(it).orElse(null) }
            FieldStyle.CONFERENCE_CHAMPIONSHIP ->
                homeTeam.conference?.let { conferenceChampionshipFieldRepository.findById(it).orElse(null) }
            else -> null
        }

    private fun designOf(name: String?): StadiumDesign? =
        name?.trim()?.takeIf { it.isNotBlank() }?.let { venueRepository.findById(it).orElse(null)?.design }

    private fun bowlFieldFor(game: Game): BowlField? = game.postseasonGameName?.let { bowlFieldRepository.findById(it).orElse(null) }
}
