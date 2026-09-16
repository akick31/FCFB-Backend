package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO

/** Renders postseason fields for real season-11 matchups and logos, for visually checking the bowl, playoff, and championship themes. */
class PostseasonFieldPreviewTool {
    private val classifier = PlayAnimationClassifier()
    private val overlayClassifier = PlayOutcomeOverlayClassifier()
    private val overlayPainter = OverlayPainter()
    private val encoder = AnimatedGifEncoder()
    private val overheadRenderer = OverheadPlayFrameRenderer(classifier)
    private val fieldGoalRenderer = FieldGoalAttemptFrameRenderer()

    @Test
    fun generatePostseasonPreviews() {
        val outputDir = File("animation-previews-postseason")
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        matchups().forEach { (name, theme) ->
            ImageIO.write(FieldBackgroundPainter.paint(theme), "png", File(outputDir, "$name-field.png"))
            writeGif(File(outputDir, "$name-field-goal.gif"), fieldGoal(theme), theme, 75, 75)
            writeGif(File(outputDir, "$name-touchdown.gif"), touchdownRun(theme), theme, 85, 105)
        }
    }

    private fun writeGif(
        file: File,
        play: Play,
        theme: FieldTheme,
        startAbs: Int,
        endAbs: Int,
    ) {
        val renderer = if (classifier.classify(play) == AnimatedPlayType.FIELD_GOAL) fieldGoalRenderer else overheadRenderer
        val frames = renderer.renderFrames(play, startAbs, endAbs, theme, OffensivePlaybook.SPREAD, DefensivePlaybook.FOUR_THREE)
        val decorated = overlayPainter.applyOverlay(frames, overlayClassifier.classifyOverlay(play), play, theme.homeTeam, theme.awayTeam)
        val bytes = encoder.encode(decorated, AnimationPalette.forTheme(theme))
        file.writeBytes(bytes)
        assertTrue(bytes.isNotEmpty(), "Expected non-empty GIF bytes for ${file.name}")
    }

    private fun matchups(): List<Pair<String, FieldTheme>> {
        val utahState = team("Utah State", "#00263A", "#8a8d8f", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/328.png")
        val mississippiState = team("Mississippi State", "#660000", "#FFFFFF", "https://a.espncdn.com/i/teamlogos/ncaa/500/344.png")
        val northTexas = team("North Texas", "#00853E", "#000000", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/249.png")
        val southCarolina = team("South Carolina", "#73000A", "#000000", "https://a.espncdn.com/i/teamlogos/ncaa/500/2579.png")
        val baylor = team("Baylor", "#154734", "#FFB81C", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/239.png")
        val kansasState = team("Kansas State", "#512888", "#d1d1d1", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/2306.png")
        val michiganState = team("Michigan State", "#18453B", "#FFFFFF", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/127.png")
        val northwestern = team("Northwestern", "#4E2A84", "#FFFFFF", "https://a.espncdn.com/i/teamlogos/ncaa/500/77.png")
        val duke = team("Duke", "#003087", "#FFFFFF", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/150.png")
        val uab = team("UAB", "#006341", "#CC8A00", "https://a.espncdn.com/i/teamlogos/ncaa/500/5.png")
        val texasAm = team("Texas A&M", "#500000", "#ffffff", "https://a.espncdn.com/i/teamlogos/ncaa/500-dark/245.png")
        val wyoming = team("Wyoming", "#ffc425", "#492f24", "https://a.espncdn.com/i/teamlogos/ncaa/500/2751.png")
        return listOf(
            "holiday-bowl" to FieldTheme(FieldStyle.BOWL, utahState, mississippiState, HOLIDAY_BOWL_LOGO),
            "liberty-bowl" to FieldTheme(FieldStyle.BOWL, northTexas, southCarolina, LIBERTY_BOWL_LOGO),
            "big-12-championship" to FieldTheme(FieldStyle.CONFERENCE_CHAMPIONSHIP, baylor, kansasState, BIG_12_LOGO),
            "big-ten-championship" to FieldTheme(FieldStyle.CONFERENCE_CHAMPIONSHIP, michiganState, northwestern, BIG_TEN_LOGO),
            "playoff-quarterfinal" to FieldTheme(FieldStyle.PLAYOFF, texasAm, duke, PLAYOFF_LOGO),
            "playoff-semifinal" to FieldTheme(FieldStyle.PLAYOFF, duke, uab, PLAYOFF_LOGO),
            "national-championship" to FieldTheme(FieldStyle.PLAYOFF, duke, wyoming, PLAYOFF_LOGO),
        )
    }

    private fun team(
        name: String,
        primary: String,
        secondary: String,
        logo: String,
    ): Team =
        Team().apply {
            this.name = name
            primaryColor = primary
            secondaryColor = secondary
            scorebugLogo = logo
        }

    private fun fieldGoal(theme: FieldTheme): Play = play(theme, 25, PlayCall.FIELD_GOAL, ActualResult.GOOD)

    private fun touchdownRun(theme: FieldTheme): Play = play(theme, 85, PlayCall.RUN, ActualResult.TOUCHDOWN)

    private fun play(
        theme: FieldTheme,
        ballLocation: Int,
        playCall: PlayCall,
        actualResult: ActualResult,
    ): Play =
        Play().apply {
            playId = 2
            gameId = 1
            possession = TeamSide.HOME
            this.ballLocation = if (playCall == PlayCall.FIELD_GOAL) 100 - ballLocation else ballLocation
            down = 1
            yardsToGo = 10
            this.playCall = playCall
            this.actualResult = actualResult
            homeTeam = theme.homeTeam.name.orEmpty()
            awayTeam = theme.awayTeam.name.orEmpty()
        }

    private companion object {
        const val HOLIDAY_BOWL_LOGO = "https://1000logos.net/wp-content/uploads/2020/04/Holiday-Bowl-Logo.png"
        const val LIBERTY_BOWL_LOGO = "https://1000logos.net/wp-content/uploads/2021/06/Liberty-Bowl-logo.png"
        const val BIG_12_LOGO = "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/4.png"
        const val BIG_TEN_LOGO = "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/5.png"
        const val PLAYOFF_LOGO =
            "https://am-prod-client-files.ppub-tmaws.io/cfbplayoff/s3fs-public/" +
                "CFP%20Symbol%20Gold%20PMS%20Dark%20BG.PNG"
    }
}
