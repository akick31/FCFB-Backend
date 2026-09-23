package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniform
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.repositories.GameRepository
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class FieldThemeResolver(
    private val conferenceRepository: ConferenceRepository,
    private val gameRepository: GameRepository,
) {
    fun resolve(
        play: Play,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        homeUniform: TeamUniform? = null,
        awayUniform: TeamUniform? = null,
    ): FieldTheme {
        val style =
            when (game.gameType) {
                GameType.PLAYOFFS -> FieldStyle.PLAYOFF
                GameType.NATIONAL_CHAMPIONSHIP -> FieldStyle.NATIONAL_CHAMPIONSHIP
                GameType.CONFERENCE_CHAMPIONSHIP -> FieldStyle.CONFERENCE_CHAMPIONSHIP
                GameType.BOWL -> FieldStyle.BOWL
                else -> FieldStyle.HOME_FIELD
            }
        val centerLogo =
            when (style) {
                FieldStyle.HOME_FIELD -> homeTeam.scorebugLogo
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
            turf = homeFieldTurf(style, homeTeam),
            homeConferenceLogoUrl = if (style == FieldStyle.BOWL) conferenceLogo(homeTeam) else null,
            awayConferenceLogoUrl = if (style == FieldStyle.BOWL) conferenceLogo(awayTeam) else null,
            homeUniform = homeUniform,
            awayUniform = awayUniform,
            midfieldCaption = midfieldCaption(style, game),
            midfieldLocation = if (style == FieldStyle.NATIONAL_CHAMPIONSHIP) CHAMPIONSHIP_LOCATION else null,
            wallCaption = wallCaption(style, game, homeTeam),
            wallLogoUrl = null,
        )
    }

    private fun wallCaption(
        style: FieldStyle,
        game: Game,
        homeTeam: Team,
    ): String? {
        val named = game.postseasonGameName?.trim()?.takeIf { it.isNotBlank() }
        return when (style) {
            FieldStyle.NATIONAL_CHAMPIONSHIP -> NATIONAL_CHAMPIONSHIP_CAPTION.joinToString(" ")
            FieldStyle.PLAYOFF -> named?.let { "$CFP_PREFIX $it" }
            FieldStyle.BOWL -> named
            FieldStyle.CONFERENCE_CHAMPIONSHIP -> named ?: conferenceName(homeTeam)?.let { "$it $CONFERENCE_TITLE_SUFFIX" }
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
        homeTeam: Team,
    ): Color {
        if (style != FieldStyle.HOME_FIELD) return FieldBackgroundPainter.TURF_COLOR
        return TeamFieldOverrides.forTeam(homeTeam.name)?.turf ?: FieldBackgroundPainter.TURF_COLOR
    }

    private fun drivesRightToLeft(play: Play): Boolean = play.quarter > FIRST_HALF_QUARTERS

    private fun playoffLogo(game: Game): String? =
        gameRepository.getLatestDarkPlayoffLogo() ?: game.postseasonGameLogo ?: gameRepository.getLatestPlayoffLogo()

    private fun conferenceLogo(team: Team): String? = team.conference?.let { conferenceRepository.findById(it).orElse(null)?.logoUrl }

    private fun conferenceName(team: Team): String? = team.conference?.let { conferenceRepository.findById(it).orElse(null)?.label }

    private fun conferenceLogoDark(team: Team): String? =
        team.conference?.let { conferenceRepository.findById(it).orElse(null)?.logoUrlDark }

    companion object {
        private const val FIRST_HALF_QUARTERS = 2
        private const val CHAMPIONSHIP_LOCATION = "Seattle"
        private const val CONFERENCE_TITLE_SUFFIX = "Championship"
        private const val CFP_PREFIX = "CFP"
        private val NATIONAL_CHAMPIONSHIP_CAPTION = listOf("National", "Championship")
    }
}
