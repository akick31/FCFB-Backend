package com.fcfb.arceus.service.fcfb.media

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.model.Conference
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.rendering.scorebug.ChewBadgeOverlay
import com.fcfb.arceus.rendering.scorebug.EspnScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.PostseasonScorebugRenderer
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import javax.imageio.ImageIO

class ScorebugServicePostseasonTest {
    @TempDir
    lateinit var directory: Path
    private val repository = mockk<ConferenceRepository>()
    private val renderer = PostseasonScorebugRenderer()
    private val teamService = mockk<TeamService>()
    private val gameService = mockk<GameService>()
    private val espnRenderer = mockk<EspnScorebugRenderer>()

    private fun service(): ScorebugService {
        every { teamService.getTeamByName("Wyoming") } returns home
        every { teamService.getTeamByName("San Jose State") } returns away
        return ScorebugService(
            teamService = teamService,
            gameService = gameService,
            espnRenderer = espnRenderer,
            foxRenderer = mockk(),
            postseasonRenderer = renderer,
            cbsRenderer = mockk(),
            nbcRenderer = mockk(),
            tntRenderer = mockk(),
            cwRenderer = mockk(),
            pacTwelveRenderer = mockk(),
            chewBadgeOverlay = ChewBadgeOverlay(),
            imagePath = directory.toString(),
            conferenceRepository = repository,
        )
    }

    private val home =
        Team().apply {
            name = "Wyoming"
            primaryColor = "#FFC425"
            conference = "HOME"
        }
    private val away =
        Team().apply {
            name = "San Jose State"
            primaryColor = "#0055A2"
            conference = "AWAY"
        }
    private val game =
        Game().apply {
            gameId = 7
            homeTeam = "Wyoming"
            awayTeam = "San Jose State"
            gameType = GameType.CONFERENCE_CHAMPIONSHIP
            gameStatus = GameStatus.FINAL
        }

    private fun logoUrl(): String {
        val path = directory.resolve("logo.png")
        ImageIO.write(BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB), "png", path.toFile())
        return path.toUri().toString()
    }

    private fun render() {
        val image = service().generateScorebug(game)
        assertEquals(180, image.width)
        assertEquals(230, image.height)
    }

    @Test
    fun `valid game logo skips conference lookup`() {
        game.postseasonGameLogo = logoUrl()
        render()
        verify(exactly = 0) { repository.findById(any()) }
    }

    @Test
    fun `missing game logo uses home conference`() {
        every { repository.findById("HOME") } returns Optional.of(Conference().apply { logoUrl = logoUrl() })
        render()
        verify(exactly = 1) { repository.findById("HOME") }
        verify(exactly = 0) { repository.findById("AWAY") }
    }

    @Test
    fun `unreadable game logo falls back to conference`() {
        game.postseasonGameLogo = directory.resolve("missing.png").toUri().toString()
        every { repository.findById("HOME") } returns Optional.empty()
        render()
        verify(exactly = 1) { repository.findById("HOME") }
    }

    @Test
    fun `missing home conference uses away conference`() {
        home.conference = null
        every { repository.findById("AWAY") } returns Optional.empty()
        render()
        verify(exactly = 1) { repository.findById("AWAY") }
    }

    @Test
    fun `missing home conference row does not query away conference`() {
        every { repository.findById("HOME") } returns Optional.empty()
        render()
        verify(exactly = 1) { repository.findById("HOME") }
        verify(exactly = 0) { repository.findById("AWAY") }
    }

    @Test
    fun `malformed game logo URL propagates without conference lookup`() {
        game.postseasonGameLogo = "https://invalid uri"
        assertThrows(java.net.URISyntaxException::class.java) { service().generateScorebug(game) }
        verify(exactly = 0) { repository.findById(any()) }
    }

    @Test
    fun `unreadable conference logo retains image output`() {
        every { repository.findById("HOME") } returns
            Optional.of(
                Conference().apply {
                    logoUrl = directory.resolve("missing.png").toUri().toString()
                },
            )
        render()
    }

    @Test
    fun `other postseason games do not query conferences`() {
        for (type in listOf(GameType.BOWL, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP)) {
            game.gameType = type
            render()
        }
        verify(exactly = 0) { repository.findById(any()) }
    }

    @Test
    fun `game logo title font failure skips conference and centers fallback title`() {
        game.postseasonGameLogo = logoUrl()
        val font = javaClass.classLoader.getResourceAsStream("Helvetica-Bold.ttf").use { Font.createFont(Font.TRUETYPE_FONT, it) }
        mockkStatic(Font::class)
        try {
            var loads = 0
            every { Font.createFont(Font.TRUETYPE_FONT, any<InputStream>()) } answers {
                if (loads++ == 0) throw IOException("title font read failed")
                font
            }
            render()
            verify(exactly = 0) { repository.findById(any()) }
        } finally {
            unmockkStatic(Font::class)
        }
    }

    @Test
    fun `conference title font failure propagates after conference lookup`() {
        every { repository.findById("HOME") } returns Optional.of(Conference().apply { logoUrl = logoUrl() })
        mockkStatic(Font::class)
        try {
            every { Font.createFont(Font.TRUETYPE_FONT, any<InputStream>()) } throws IOException("title font read failed")
            assertThrows(IOException::class.java) { service().generateScorebug(game) }
            verify(exactly = 1) { repository.findById("HOME") }
        } finally {
            unmockkStatic(Font::class)
        }
    }

    @Test
    fun `unsupported game image retains failure without conference fallback`() {
        val path = directory.resolve("invalid.png")
        Files.writeString(path, "not an image")
        game.postseasonGameLogo = path.toUri().toString()
        assertThrows(NullPointerException::class.java) { service().generateScorebug(game) }
        verify(exactly = 0) { repository.findById(any()) }
    }

    @Test
    fun `unsupported conference image retains title and scorebug output`() {
        val path = directory.resolve("invalid.png")
        Files.writeString(path, "not an image")
        every { repository.findById("HOME") } returns Optional.of(Conference().apply { logoUrl = path.toUri().toString() })
        render()
        verify(exactly = 1) { repository.findById("HOME") }
    }

    @Test
    fun `scorebug response preserves PNG headers bytes and storage`() {
        game.postseasonGameLogo = logoUrl()
        every { gameService.getGameById(7) } returns game
        val response = service().getScorebugByGameId(7)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.IMAGE_PNG, response.headers.contentType)
        assertEquals(response.body!!.size.toLong(), response.headers.contentLength)
        assertArrayEquals(Files.readAllBytes(directory.resolve("scorebugs/7_scorebug.png")), response.body)
        val image = ImageIO.read(response.body!!.inputStream())
        assertEquals(180, image.width)
        assertEquals(230, image.height)
        verify(exactly = 0) { repository.findById(any()) }
    }

    @Test
    fun `regular game channel selection stays with existing ESPN renderer`() {
        game.gameType = GameType.CONFERENCE_GAME
        val image = BufferedImage(180, 230, BufferedImage.TYPE_INT_ARGB)
        every { espnRenderer.render(game, home, away) } returns image
        val service = service()
        for (channel in listOf(
            TVChannel.ESPN,
            TVChannel.FOX,
            TVChannel.CBS,
            TVChannel.TNT,
            TVChannel.NBC,
            TVChannel.THE_CW,
            TVChannel.PAC_12_NETWORK,
        )) {
            game.tvChannel = channel
            assertEquals(image, service.generateScorebug(game))
        }
        verify(exactly = 7) { espnRenderer.render(game, home, away) }
        verify(exactly = 0) { repository.findById(any()) }
    }
}
