package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.script.KickoffPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.KneelPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PassPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PuntPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.RunPlayScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ChoreographyResultSpotTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("plays")
    fun `ball finishes on the result spot`(
        name: String,
        script: PlayScript,
        context: PlayContext,
    ) {
        assertEquals(context.endSpot, script.choreograph(context).ball.at(1f).position.along, TOLERANCE, name)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("plays")
    fun `every play puts eleven players on each side`(
        name: String,
        script: PlayScript,
        context: PlayContext,
    ) {
        val choreography = script.choreograph(context)
        assertEquals(11, choreography.offense.size, name)
        assertEquals(11, choreography.defense.size, name)
    }

    companion object {
        private const val TOLERANCE = 0.01f

        @JvmStatic
        fun plays(): List<Arguments> =
            OffensivePlaybook.entries.flatMap { offense ->
                DefensivePlaybook.entries.flatMap { defense ->
                    listOf(
                        case("run gain", RunPlayScript(), PlayCall.RUN, ActualResult.GAIN, 30, 37f, offense, defense),
                        case("run loss", RunPlayScript(), PlayCall.RUN, ActualResult.LOSS, 30, 27f, offense, defense),
                        case("run fumble", RunPlayScript(), PlayCall.RUN, ActualResult.TURNOVER, 40, 40f, offense, defense),
                        case(
                            "run fumble returned",
                            RunPlayScript(),
                            PlayCall.RUN,
                            ActualResult.TURNOVER,
                            40,
                            30f,
                            offense,
                            defense,
                            result = Scenario.TURNOVER_PLUS_10_YARDS,
                        ),
                        case(
                            "run fumble after gain",
                            RunPlayScript(),
                            PlayCall.RUN,
                            ActualResult.TURNOVER,
                            40,
                            50f,
                            offense,
                            defense,
                            result = Scenario.TURNOVER_MINUS_10_YARDS,
                        ),
                        case(
                            "run fumble scoop and score",
                            RunPlayScript(),
                            PlayCall.RUN,
                            ActualResult.TURNOVER_TOUCHDOWN,
                            40,
                            -5f,
                            offense,
                            defense,
                            result = Scenario.TURNOVER_TOUCHDOWN,
                        ),
                        case("away run", RunPlayScript(), PlayCall.RUN, ActualResult.GAIN, 40, 55f, offense, defense, TeamSide.AWAY),
                        case("completion", PassPlayScript(), PlayCall.PASS, ActualResult.GAIN, 30, 49f, offense, defense),
                        case("sack", PassPlayScript(), PlayCall.PASS, ActualResult.LOSS, 30, 24f, offense, defense),
                        case("interception", PassPlayScript(), PlayCall.PASS, ActualResult.TURNOVER, 30, 45f, offense, defense),
                        case("kneel", KneelPlayScript(), PlayCall.KNEEL, ActualResult.KNEEL, 30, 29f, offense, defense),
                        case("run safety", RunPlayScript(), PlayCall.RUN, ActualResult.SAFETY, 4, -5f, offense, defense),
                        case("sack safety", PassPlayScript(), PlayCall.PASS, ActualResult.SAFETY, 6, -5f, offense, defense),
                        case(
                            "away sack safety",
                            PassPlayScript(),
                            PlayCall.PASS,
                            ActualResult.SAFETY,
                            5,
                            105f,
                            offense,
                            defense,
                            TeamSide.AWAY,
                        ),
                    )
                }
            } +
                listOf(
                    case("punt return", PuntPlayScript(), PlayCall.PUNT, ActualResult.PUNT, 30, 68f),
                    case("kickoff return", KickoffPlayScript(), PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, 35, 72f),
                )

        private fun case(
            name: String,
            script: PlayScript,
            playCall: PlayCall,
            actualResult: ActualResult,
            ballLocation: Int,
            endSpot: Float,
            offense: OffensivePlaybook = OffensivePlaybook.PRO,
            defense: DefensivePlaybook = DefensivePlaybook.FOUR_THREE,
            possession: TeamSide = TeamSide.HOME,
            result: Scenario? = null,
        ): Arguments {
            val play =
                Play().apply {
                    this.result = result
                    this.possession = possession
                    this.ballLocation = ballLocation
                    this.playCall = playCall
                    this.actualResult = actualResult
                }
            val lineOfScrimmage = if (possession == TeamSide.HOME) ballLocation.toFloat() else 100f - ballLocation
            val forward = if (possession == TeamSide.HOME) 1f else -1f
            val context = PlayContext(play, lineOfScrimmage, endSpot, forward, 1f, offense, defense)
            return Arguments.of("$name ($offense vs $defense)", script, context)
        }
    }
}
