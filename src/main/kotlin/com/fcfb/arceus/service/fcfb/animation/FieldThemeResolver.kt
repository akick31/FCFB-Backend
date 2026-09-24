package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamField
import com.fcfb.arceus.model.TeamUniformHistory
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.TeamFieldRepository
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class FieldThemeResolver(
    private val conferenceRepository: ConferenceRepository,
    private val gameRepository: GameRepository,
    private val teamFieldRepository: TeamFieldRepository,
) {
    fun resolve(
        play: Play,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        homeUniform: TeamUniformHistory? = null,
        awayUniform: TeamUniformHistory? = null,
    ): FieldTheme {
        val style =
            when (game.gameType) {
                GameType.PLAYOFFS -> FieldStyle.PLAYOFF
                GameType.NATIONAL_CHAMPIONSHIP -> FieldStyle.NATIONAL_CHAMPIONSHIP
                GameType.CONFERENCE_CHAMPIONSHIP -> FieldStyle.CONFERENCE_CHAMPIONSHIP
                GameType.BOWL -> FieldStyle.BOWL
                else -> FieldStyle.HOME_FIELD
            }
        val homeField = fieldFor(homeTeam)
        val centerLogo =
            when (style) {
                FieldStyle.HOME_FIELD -> homeField?.midfieldLogoUrl ?: homeTeam.scorebugLogo
                FieldStyle.CONFERENCE_CHAMPIONSHIP -> conferenceLogo(homeTeam) ?: game.postseasonGameLogo
                FieldStyle.PLAYOFF, FieldStyle.NATIONAL_CHAMPIONSHIP -> playoffLogo(game)
                FieldStyle.BOWL -> game.postseasonGameLogo
            }
        return FieldTheme(
            style = style,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            centerLogoUrl = centerLogo,
            flipped = drivesRightToLeft(play),
            turf = homeFieldTurf(style, homeField),
            homeConferenceLogoUrl = conferenceLogoFor(style, homeTeam, homeTeam),
            awayConferenceLogoUrl = conferenceLogoFor(style, awayTeam, homeTeam),
            homeUniform = homeUniform,
            awayUniform = awayUniform,
            midfieldCaption = midfieldCaption(style, game),
            midfieldLocation = if (style == FieldStyle.NATIONAL_CHAMPIONSHIP) CHAMPIONSHIP_LOCATION else null,
            wallCaption = wallCaption(style, game, homeTeam, homeField),
            wallLogoUrl = null,
            homeField = homeField,
        )
    }

    /** A home field carries the host's conference mark on both sides; a bowl is neutral, so each side shows its own team's. */
    private fun conferenceLogoFor(
        style: FieldStyle,
        sideTeam: Team,
        homeTeam: Team,
    ): String? =
        when (style) {
            FieldStyle.BOWL -> conferenceLogo(sideTeam)
            FieldStyle.HOME_FIELD -> conferenceLogo(homeTeam)
            else -> null
        }

    private fun fieldFor(team: Team): TeamField? = team.name?.let { teamFieldRepository.findById(it).orElse(null) }

    private fun wallCaption(
        style: FieldStyle,
        game: Game,
        homeTeam: Team,
        homeField: TeamField?,
    ): String? {
        val named = game.postseasonGameName?.trim()?.takeIf { it.isNotBlank() }
        return when (style) {
            FieldStyle.NATIONAL_CHAMPIONSHIP -> NATIONAL_CHAMPIONSHIP_CAPTION.joinToString(" ")
            FieldStyle.PLAYOFF -> named?.let { "$CFP_PREFIX $it" }
            FieldStyle.BOWL -> named
            FieldStyle.CONFERENCE_CHAMPIONSHIP -> named ?: conferenceName(homeTeam)?.let { "$it $CONFERENCE_TITLE_SUFFIX" }
            FieldStyle.HOME_FIELD -> homeField?.wallText?.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun midfieldCaption(
        style: FieldStyle,
        game: Game,
    ): List<String> {
        val named = game.postseasonGameName?.trim()?.takeIf { it.isNotBlank() }
        return when (style) {
            FieldStyle.NATIONAL_CHAMPIONSHIP -> NATIONAL_CHAMPIONSHIP_CAPTION
            FieldStyle.PLAYOFF -> listOfNotNull(named)
            else -> emptyList()
        }
    }

    private fun homeFieldTurf(
        style: FieldStyle,
        homeField: TeamField?,
    ): Color {
        if (style != FieldStyle.HOME_FIELD) return FieldBackgroundPainter.TURF_COLOR
        return homeField?.turfColor?.let { FieldBackgroundPainter.parseColor(it) } ?: FieldBackgroundPainter.TURF_COLOR
    }

    private fun drivesRightToLeft(play: Play): Boolean = play.quarter > FIRST_HALF_QUARTERS

    private fun playoffLogo(game: Game): String? =
        gameRepository.getLatestDarkPlayoffLogo() ?: game.postseasonGameLogo ?: gameRepository.getLatestPlayoffLogo()

    /** Independents have no conference identity to put on a field, and FBS_INDEPENDENT does carry a logo that would otherwise draw. */
    private fun conferenceLogo(team: Team): String? {
        val code = team.conference ?: return null
        if (code in INDEPENDENT_CONFERENCES) return null
        return conferenceRepository.findById(code).orElse(null)?.logoUrl
    }

    private fun conferenceName(team: Team): String? = team.conference?.let { conferenceRepository.findById(it).orElse(null)?.label }

    companion object {
        private const val FIRST_HALF_QUARTERS = 2
        private const val CHAMPIONSHIP_LOCATION = "Seattle"
        private const val CONFERENCE_TITLE_SUFFIX = "Championship"
        private const val CFP_PREFIX = "CFP"
        private val NATIONAL_CHAMPIONSHIP_CAPTION = listOf("National", "Championship")
        private val INDEPENDENT_CONFERENCES = setOf("FBS_INDEPENDENT", "FCS_INDEPENDENT", "INDEPENDENT")
    }
}
