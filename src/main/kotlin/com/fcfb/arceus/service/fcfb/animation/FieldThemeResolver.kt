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
                GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP -> FieldStyle.PLAYOFF
                GameType.CONFERENCE_CHAMPIONSHIP -> FieldStyle.CONFERENCE_CHAMPIONSHIP
                GameType.BOWL -> FieldStyle.BOWL
                else -> FieldStyle.HOME_FIELD
            }
        val centerLogo =
            when (style) {
                FieldStyle.HOME_FIELD -> homeTeam.scorebugLogo
                FieldStyle.CONFERENCE_CHAMPIONSHIP -> conferenceLogo(homeTeam) ?: game.postseasonGameLogo
                FieldStyle.PLAYOFF -> playoffLogo(game)
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
        )
    }

    /** A team's own turf color only applies at home; postseason games are played on a neutral field. */
    private fun homeFieldTurf(
        style: FieldStyle,
        homeTeam: Team,
    ): Color {
        if (style != FieldStyle.HOME_FIELD) return FieldBackgroundPainter.TURF_COLOR
        return TeamFieldOverrides.forTeam(homeTeam.name)?.turf ?: FieldBackgroundPainter.TURF_COLOR
    }

    /** Teams switch ends at the half, and overtime quarters keep the home team defending the right end zone. */
    private fun drivesRightToLeft(play: Play): Boolean = play.quarter > FIRST_HALF_QUARTERS

    /** Playoff fields use the dark-background version of the playoff logo, whichever round this game is. */
    private fun playoffLogo(game: Game): String? =
        gameRepository.getLatestDarkPlayoffLogo() ?: game.postseasonGameLogo ?: gameRepository.getLatestPlayoffLogo()

    private fun conferenceLogo(team: Team): String? = team.conference?.let { conferenceRepository.findById(it).orElse(null)?.logoUrl }

    companion object {
        private const val FIRST_HALF_QUARTERS = 2
    }
}
