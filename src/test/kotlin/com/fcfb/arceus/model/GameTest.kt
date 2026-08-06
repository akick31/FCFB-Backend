package com.fcfb.arceus.model

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {
    @Test
    fun `test Game entity annotations`() {
        val game = Game()

        val entityAnnotation = Game::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = Game::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("game", tableAnnotation.name)
        val typeDefAnnotation = Game::class.java.getAnnotation(org.hibernate.annotations.TypeDef::class.java)
        assertNotNull(typeDefAnnotation)
        assertEquals("json", typeDefAnnotation.name)
    }

    @Test
    fun `test Game default constructor`() {
        val game = Game()

        assertEquals(0, game.gameId)
        assertEquals(listOf<String>(), game.homeCoaches)
        assertEquals(listOf<String>(), game.awayCoaches)
        assertEquals(listOf<String>(), game.homeCoachDiscordIds)
        assertEquals(listOf<String>(), game.awayCoachDiscordIds)
        assertEquals(0, game.homeScore)
        assertEquals(0, game.awayScore)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(1, game.quarter)
        assertEquals("7:00", game.clock)
        assertEquals(0, game.ballLocation)
        assertEquals(1, game.down)
        assertEquals(10, game.yardsToGo)
        assertNull(game.tvChannel)
        assertEquals(0, game.homeTeamRank)
        assertNull(game.homeWins)
        assertNull(game.homeLosses)
        assertNull(game.awayWins)
        assertNull(game.awayLosses)
        assertEquals(0, game.awayTeamRank)
        assertNull(game.subdivision)
        assertNull(game.timestamp)
        assertNull(game.winProbability)
        assertNull(game.season)
        assertNull(game.week)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(0, game.numPlays)
        assertEquals(3, game.homeTimeouts)
        assertEquals(3, game.awayTimeouts)
        assertNull(game.coinTossWinner)
        assertNull(game.coinTossChoice)
        assertNull(game.overtimeCoinTossWinner)
        assertNull(game.overtimeCoinTossChoice)
        assertNull(game.homePlatformId)
        assertNull(game.awayPlatformId)
        assertNull(game.lastMessageTimestamp)
        assertNull(game.gameTimer)
        assertNull(game.gameWarning)
        assertNull(game.currentPlayType)
        assertNull(game.currentPlayId)
        assertFalse(game.clockStopped)
        assertEquals(listOf<String>(), game.requestMessageId)
        assertNull(game.gameStatus)
        assertNull(game.gameType)
        assertNull(game.gameMode)
        assertEquals(0, game.overtimeHalf)
        assertFalse(game.closeGame)
        assertFalse(game.closeGamePinged)
        assertFalse(game.upsetAlert)
        assertFalse(game.upsetAlertPinged)

        assertThrows<UninitializedPropertyAccessException> {
            game.homeTeam
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.awayTeam
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.homeOffensivePlaybook
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.awayOffensivePlaybook
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.homeDefensivePlaybook
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.awayDefensivePlaybook
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.homePlatform
        }
        assertThrows<UninitializedPropertyAccessException> {
            game.awayPlatform
        }
    }

    @Test
    fun `test Game parameterized constructor`() {
        val homeCoaches = listOf("coach1", "coach2")
        val awayCoaches = listOf("coach3", "coach4")
        val homeCoachDiscordIds = listOf("123456789", "987654321")
        val awayCoachDiscordIds = listOf("111222333", "444555666")
        val requestMessageId = listOf("msg1", "msg2")

        val game =
            Game(
                homeTeam = "Alabama",
                awayTeam = "Auburn",
                homeCoaches = homeCoaches,
                awayCoaches = awayCoaches,
                homeCoachDiscordIds = homeCoachDiscordIds,
                awayCoachDiscordIds = awayCoachDiscordIds,
                homeOffensivePlaybook = OffensivePlaybook.AIR_RAID,
                awayOffensivePlaybook = OffensivePlaybook.SPREAD,
                homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE,
                awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR,
                homeScore = 28,
                awayScore = 14,
                possession = TeamSide.HOME,
                quarter = 3,
                clock = "12:30",
                ballLocation = 45,
                down = 2,
                yardsToGo = 8,
                tvChannel = TVChannel.CBS,
                homeTeamRank = 5,
                homeWins = 8,
                homeLosses = 2,
                awayTeamRank = 12,
                awayWins = 6,
                awayLosses = 4,
                subdivision = Subdivision.FBS,
                timestamp = "2024-10-15T18:30:00Z",
                winProbability = 0.75,
                season = 2024,
                week = 8,
                waitingOn = TeamSide.AWAY,
                numPlays = 45,
                homeTimeouts = 2,
                awayTimeouts = 1,
                coinTossWinner = TeamSide.HOME,
                coinTossChoice = CoinTossChoice.RECEIVE,
                overtimeCoinTossWinner = TeamSide.AWAY,
                overtimeCoinTossChoice = OvertimeCoinTossChoice.OFFENSE,
                homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD,
                homePlatformId = "home_platform_id",
                awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD,
                awayPlatformId = "away_platform_id",
                lastMessageTimestamp = "2024-10-15T18:35:00Z",
                gameTimer = "00:05:00",
                gameWarning = GameWarning.NONE,
                currentPlayType = PlayType.NORMAL,
                currentPlayId = 123,
                clockStopped = true,
                requestMessageId = requestMessageId,
                gameStatus = GameStatus.IN_PROGRESS,
                gameType = GameType.CONFERENCE_GAME,
                gameMode = GameMode.NORMAL,
                overtimeHalf = 0,
                closeGame = false,
                closeGamePinged = false,
                upsetAlert = false,
                upsetAlertPinged = false,
            )

        assertEquals("Alabama", game.homeTeam)
        assertEquals("Auburn", game.awayTeam)
        assertEquals(homeCoaches, game.homeCoaches)
        assertEquals(awayCoaches, game.awayCoaches)
        assertEquals(homeCoachDiscordIds, game.homeCoachDiscordIds)
        assertEquals(awayCoachDiscordIds, game.awayCoachDiscordIds)
        assertEquals(OffensivePlaybook.AIR_RAID, game.homeOffensivePlaybook)
        assertEquals(OffensivePlaybook.SPREAD, game.awayOffensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, game.homeDefensivePlaybook)
        assertEquals(DefensivePlaybook.THREE_FOUR, game.awayDefensivePlaybook)
        assertEquals(28, game.homeScore)
        assertEquals(14, game.awayScore)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(3, game.quarter)
        assertEquals("12:30", game.clock)
        assertEquals(45, game.ballLocation)
        assertEquals(2, game.down)
        assertEquals(8, game.yardsToGo)
        assertEquals(TVChannel.CBS, game.tvChannel)
        assertEquals(5, game.homeTeamRank)
        assertEquals(8, game.homeWins)
        assertEquals(2, game.homeLosses)
        assertEquals(12, game.awayTeamRank)
        assertEquals(6, game.awayWins)
        assertEquals(4, game.awayLosses)
        assertEquals(Subdivision.FBS, game.subdivision)
        assertEquals("2024-10-15T18:30:00Z", game.timestamp)
        assertEquals(0.75, game.winProbability)
        assertEquals(2024, game.season)
        assertEquals(8, game.week)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(45, game.numPlays)
        assertEquals(2, game.homeTimeouts)
        assertEquals(1, game.awayTimeouts)
        assertEquals(TeamSide.HOME, game.coinTossWinner)
        assertEquals(CoinTossChoice.RECEIVE, game.coinTossChoice)
        assertEquals(TeamSide.AWAY, game.overtimeCoinTossWinner)
        assertEquals(OvertimeCoinTossChoice.OFFENSE, game.overtimeCoinTossChoice)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, game.homePlatform)
        assertEquals("home_platform_id", game.homePlatformId)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, game.awayPlatform)
        assertEquals("away_platform_id", game.awayPlatformId)
        assertEquals("2024-10-15T18:35:00Z", game.lastMessageTimestamp)
        assertEquals("00:05:00", game.gameTimer)
        assertEquals(GameWarning.NONE, game.gameWarning)
        assertEquals(PlayType.NORMAL, game.currentPlayType)
        assertEquals(123, game.currentPlayId)
        assertTrue(game.clockStopped)
        assertEquals(requestMessageId, game.requestMessageId)
        assertEquals(GameStatus.IN_PROGRESS, game.gameStatus)
        assertEquals(GameType.CONFERENCE_GAME, game.gameType)
        assertEquals(GameMode.NORMAL, game.gameMode)
        assertEquals(0, game.overtimeHalf)
        assertFalse(game.closeGame)
        assertFalse(game.closeGamePinged)
        assertFalse(game.upsetAlert)
        assertFalse(game.upsetAlertPinged)
    }

    @Test
    fun `test Game property mutability`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        game.gameId = 123
        game.homeCoaches = listOf("coach1", "coach2")
        game.awayCoaches = listOf("coach3", "coach4")
        game.homeCoachDiscordIds = listOf("123456789", "987654321")
        game.awayCoachDiscordIds = listOf("111222333", "444555666")
        game.homeScore = 28
        game.awayScore = 14
        game.possession = TeamSide.AWAY
        game.quarter = 4
        game.clock = "2:30"
        game.ballLocation = 35
        game.down = 3
        game.yardsToGo = 5
        game.tvChannel = TVChannel.ESPN
        game.homeTeamRank = 5
        game.homeWins = 8
        game.homeLosses = 2
        game.awayTeamRank = 12
        game.awayWins = 6
        game.awayLosses = 4
        game.subdivision = Subdivision.FBS
        game.timestamp = "2024-10-15T18:30:00Z"
        game.winProbability = 0.75
        game.season = 2024
        game.week = 8
        game.waitingOn = TeamSide.HOME
        game.numPlays = 45
        game.homeTimeouts = 2
        game.awayTimeouts = 1
        game.coinTossWinner = TeamSide.HOME
        game.coinTossChoice = CoinTossChoice.RECEIVE
        game.overtimeCoinTossWinner = TeamSide.AWAY
        game.overtimeCoinTossChoice = OvertimeCoinTossChoice.OFFENSE
        game.homePlatformId = "home_platform_id"
        game.awayPlatformId = "away_platform_id"
        game.lastMessageTimestamp = "2024-10-15T18:35:00Z"
        game.gameTimer = "00:05:00"
        game.gameWarning = GameWarning.NONE
        game.currentPlayType = PlayType.NORMAL
        game.currentPlayId = 123
        game.clockStopped = true
        game.requestMessageId = listOf("msg1", "msg2")
        game.gameStatus = GameStatus.IN_PROGRESS
        game.gameType = GameType.CONFERENCE_GAME
        game.gameMode = GameMode.NORMAL
        game.overtimeHalf = 0
        game.closeGame = false
        game.closeGamePinged = false
        game.upsetAlert = false
        game.upsetAlertPinged = false

        assertEquals(123, game.gameId)
        assertEquals("Alabama", game.homeTeam)
        assertEquals("Auburn", game.awayTeam)
        assertEquals(listOf("coach1", "coach2"), game.homeCoaches)
        assertEquals(listOf("coach3", "coach4"), game.awayCoaches)
        assertEquals(listOf("123456789", "987654321"), game.homeCoachDiscordIds)
        assertEquals(listOf("111222333", "444555666"), game.awayCoachDiscordIds)
        assertEquals(OffensivePlaybook.AIR_RAID, game.homeOffensivePlaybook)
        assertEquals(OffensivePlaybook.SPREAD, game.awayOffensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, game.homeDefensivePlaybook)
        assertEquals(DefensivePlaybook.THREE_FOUR, game.awayDefensivePlaybook)
        assertEquals(28, game.homeScore)
        assertEquals(14, game.awayScore)
        assertEquals(TeamSide.AWAY, game.possession)
        assertEquals(4, game.quarter)
        assertEquals("2:30", game.clock)
        assertEquals(35, game.ballLocation)
        assertEquals(3, game.down)
        assertEquals(5, game.yardsToGo)
        assertEquals(TVChannel.ESPN, game.tvChannel)
        assertEquals(5, game.homeTeamRank)
        assertEquals(8, game.homeWins)
        assertEquals(2, game.homeLosses)
        assertEquals(12, game.awayTeamRank)
        assertEquals(6, game.awayWins)
        assertEquals(4, game.awayLosses)
        assertEquals(Subdivision.FBS, game.subdivision)
        assertEquals("2024-10-15T18:30:00Z", game.timestamp)
        assertEquals(0.75, game.winProbability)
        assertEquals(2024, game.season)
        assertEquals(8, game.week)
        assertEquals(TeamSide.HOME, game.waitingOn)
        assertEquals(45, game.numPlays)
        assertEquals(2, game.homeTimeouts)
        assertEquals(1, game.awayTimeouts)
        assertEquals(TeamSide.HOME, game.coinTossWinner)
        assertEquals(CoinTossChoice.RECEIVE, game.coinTossChoice)
        assertEquals(TeamSide.AWAY, game.overtimeCoinTossWinner)
        assertEquals(OvertimeCoinTossChoice.OFFENSE, game.overtimeCoinTossChoice)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, game.homePlatform)
        assertEquals("home_platform_id", game.homePlatformId)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, game.awayPlatform)
        assertEquals("away_platform_id", game.awayPlatformId)
        assertEquals("2024-10-15T18:35:00Z", game.lastMessageTimestamp)
        assertEquals("00:05:00", game.gameTimer)
        assertEquals(GameWarning.NONE, game.gameWarning)
        assertEquals(PlayType.NORMAL, game.currentPlayType)
        assertEquals(123, game.currentPlayId)
        assertTrue(game.clockStopped)
        assertEquals(listOf("msg1", "msg2"), game.requestMessageId)
        assertEquals(GameStatus.IN_PROGRESS, game.gameStatus)
        assertEquals(GameType.CONFERENCE_GAME, game.gameType)
        assertEquals(GameMode.NORMAL, game.gameMode)
        assertEquals(0, game.overtimeHalf)
        assertFalse(game.closeGame)
        assertFalse(game.closeGamePinged)
        assertFalse(game.upsetAlert)
        assertFalse(game.upsetAlertPinged)
    }

    @Test
    fun `test Game with null optional fields`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        game.tvChannel = null
        game.homeWins = null
        game.homeLosses = null
        game.awayWins = null
        game.awayLosses = null
        game.subdivision = null
        game.timestamp = null
        game.winProbability = null
        game.season = null
        game.week = null
        game.coinTossWinner = null
        game.coinTossChoice = null
        game.overtimeCoinTossWinner = null
        game.overtimeCoinTossChoice = null
        game.homePlatformId = null
        game.awayPlatformId = null
        game.lastMessageTimestamp = null
        game.gameTimer = null
        game.gameWarning = null
        game.currentPlayType = null
        game.currentPlayId = null
        game.requestMessageId = null
        game.gameStatus = null
        game.gameType = null
        game.gameMode = null
        game.overtimeHalf = null

        assertNull(game.tvChannel)
        assertNull(game.homeWins)
        assertNull(game.homeLosses)
        assertNull(game.awayWins)
        assertNull(game.awayLosses)
        assertNull(game.subdivision)
        assertNull(game.timestamp)
        assertNull(game.winProbability)
        assertNull(game.season)
        assertNull(game.week)
        assertNull(game.coinTossWinner)
        assertNull(game.coinTossChoice)
        assertNull(game.overtimeCoinTossWinner)
        assertNull(game.overtimeCoinTossChoice)
        assertNull(game.homePlatformId)
        assertNull(game.awayPlatformId)
        assertNull(game.lastMessageTimestamp)
        assertNull(game.gameTimer)
        assertNull(game.gameWarning)
        assertNull(game.currentPlayType)
        assertNull(game.currentPlayId)
        assertNull(game.requestMessageId)
        assertNull(game.gameStatus)
        assertNull(game.gameType)
        assertNull(game.gameMode)
        assertNull(game.overtimeHalf)
    }

    @Test
    fun `test Game with all enum values`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        TeamSide.entries.forEach { teamSide ->
            game.possession = teamSide
            game.waitingOn = teamSide
            game.coinTossWinner = teamSide
            game.overtimeCoinTossWinner = teamSide
            assertEquals(teamSide, game.possession)
            assertEquals(teamSide, game.waitingOn)
            assertEquals(teamSide, game.coinTossWinner)
            assertEquals(teamSide, game.overtimeCoinTossWinner)
        }

        TVChannel.entries.forEach { tvChannel ->
            game.tvChannel = tvChannel
            assertEquals(tvChannel, game.tvChannel)
        }

        Subdivision.entries.forEach { subdivision ->
            game.subdivision = subdivision
            assertEquals(subdivision, game.subdivision)
        }

        OffensivePlaybook.entries.forEach { offensivePlaybook ->
            game.homeOffensivePlaybook = offensivePlaybook
            game.awayOffensivePlaybook = offensivePlaybook
            assertEquals(offensivePlaybook, game.homeOffensivePlaybook)
            assertEquals(offensivePlaybook, game.awayOffensivePlaybook)
        }

        DefensivePlaybook.entries.forEach { defensivePlaybook ->
            game.homeDefensivePlaybook = defensivePlaybook
            game.awayDefensivePlaybook = defensivePlaybook
            assertEquals(defensivePlaybook, game.homeDefensivePlaybook)
            assertEquals(defensivePlaybook, game.awayDefensivePlaybook)
        }

        com.fcfb.arceus.enums.system.Platform.entries.forEach { platform ->
            game.homePlatform = platform
            game.awayPlatform = platform
            assertEquals(platform, game.homePlatform)
            assertEquals(platform, game.awayPlatform)
        }

        CoinTossChoice.entries.forEach { coinTossChoice ->
            game.coinTossChoice = coinTossChoice
            assertEquals(coinTossChoice, game.coinTossChoice)
        }

        OvertimeCoinTossChoice.entries.forEach { overtimeCoinTossChoice ->
            game.overtimeCoinTossChoice = overtimeCoinTossChoice
            assertEquals(overtimeCoinTossChoice, game.overtimeCoinTossChoice)
        }

        PlayType.entries.forEach { playType ->
            game.currentPlayType = playType
            assertEquals(playType, game.currentPlayType)
        }

        GameStatus.entries.forEach { gameStatus ->
            game.gameStatus = gameStatus
            assertEquals(gameStatus, game.gameStatus)
        }

        GameType.entries.forEach { gameType ->
            game.gameType = gameType
            assertEquals(gameType, game.gameType)
        }

        GameMode.entries.forEach { gameMode ->
            game.gameMode = gameMode
            assertEquals(gameMode, game.gameMode)
        }

        GameWarning.entries.forEach { warning ->
            game.gameWarning = warning
            assertEquals(warning, game.gameWarning)
        }
    }

    @Test
    fun `test Game boolean properties`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        game.clockStopped = true
        assertTrue(game.clockStopped)

        game.clockStopped = false
        assertFalse(game.clockStopped)

        game.closeGame = true
        assertTrue(game.closeGame)

        game.closeGame = false
        assertFalse(game.closeGame)

        game.closeGamePinged = true
        assertTrue(game.closeGamePinged)

        game.closeGamePinged = false
        assertFalse(game.closeGamePinged)

        game.upsetAlert = true
        assertTrue(game.upsetAlert)

        game.upsetAlert = false
        assertFalse(game.upsetAlert)

        game.upsetAlertPinged = true
        assertTrue(game.upsetAlertPinged)

        game.upsetAlertPinged = false
        assertFalse(game.upsetAlertPinged)
    }

    @Test
    fun `test Game numeric properties`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        game.gameId = 123
        assertEquals(123, game.gameId)

        game.homeScore = 28
        game.awayScore = 14
        assertEquals(28, game.homeScore)
        assertEquals(14, game.awayScore)

        game.quarter = 4
        assertEquals(4, game.quarter)

        game.ballLocation = 45
        assertEquals(45, game.ballLocation)

        game.down = 3
        assertEquals(3, game.down)

        game.yardsToGo = 8
        assertEquals(8, game.yardsToGo)

        game.homeTeamRank = 5
        game.awayTeamRank = 12
        assertEquals(5, game.homeTeamRank)
        assertEquals(12, game.awayTeamRank)

        game.homeWins = 8
        game.homeLosses = 2
        game.awayWins = 6
        game.awayLosses = 4
        assertEquals(8, game.homeWins)
        assertEquals(2, game.homeLosses)
        assertEquals(6, game.awayWins)
        assertEquals(4, game.awayLosses)

        game.season = 2024
        game.week = 8
        assertEquals(2024, game.season)
        assertEquals(8, game.week)

        game.numPlays = 45
        assertEquals(45, game.numPlays)

        game.homeTimeouts = 2
        game.awayTimeouts = 1
        assertEquals(2, game.homeTimeouts)
        assertEquals(1, game.awayTimeouts)

        game.currentPlayId = 123
        assertEquals(123, game.currentPlayId)

        game.overtimeHalf = 1
        assertEquals(1, game.overtimeHalf)

        game.winProbability = 0.75
        assertEquals(0.75, game.winProbability)
    }

    @Test
    fun `test Game string properties`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        game.clock = "2:30"
        assertEquals("2:30", game.clock)

        game.timestamp = "2024-10-15T18:30:00Z"
        assertEquals("2024-10-15T18:30:00Z", game.timestamp)

        game.homePlatformId = "home_platform_id"
        game.awayPlatformId = "away_platform_id"
        assertEquals("home_platform_id", game.homePlatformId)
        assertEquals("away_platform_id", game.awayPlatformId)

        game.lastMessageTimestamp = "2024-10-15T18:35:00Z"
        game.gameTimer = "00:05:00"
        assertEquals("2024-10-15T18:35:00Z", game.lastMessageTimestamp)
        assertEquals("00:05:00", game.gameTimer)
    }

    @Test
    fun `test Game list properties`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        val homeCoaches = listOf("coach1", "coach2")
        val awayCoaches = listOf("coach3", "coach4")
        game.homeCoaches = homeCoaches
        game.awayCoaches = awayCoaches
        assertEquals(homeCoaches, game.homeCoaches)
        assertEquals(awayCoaches, game.awayCoaches)

        val homeCoachDiscordIds = listOf("123456789", "987654321")
        val awayCoachDiscordIds = listOf("111222333", "444555666")
        game.homeCoachDiscordIds = homeCoachDiscordIds
        game.awayCoachDiscordIds = awayCoachDiscordIds
        assertEquals(homeCoachDiscordIds, game.homeCoachDiscordIds)
        assertEquals(awayCoachDiscordIds, game.awayCoachDiscordIds)

        val requestMessageId = listOf("msg1", "msg2", "msg3")
        game.requestMessageId = requestMessageId
        assertEquals(requestMessageId, game.requestMessageId)
    }

    @Test
    fun `test Game complete scenario`() {
        val game = Game()

        game.homeTeam = "Alabama"
        game.awayTeam = "Auburn"
        game.homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
        game.awayOffensivePlaybook = OffensivePlaybook.SPREAD
        game.homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
        game.awayDefensivePlaybook = DefensivePlaybook.THREE_FOUR
        game.homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD
        game.awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD

        game.gameId = 456
        game.homeCoaches = listOf("head_coach", "offensive_coordinator", "defensive_coordinator")
        game.awayCoaches = listOf("away_head_coach", "away_offensive_coordinator")
        game.homeCoachDiscordIds = listOf("123456789", "987654321", "555666777")
        game.awayCoachDiscordIds = listOf("111222333", "444555666")
        game.homeScore = 35
        game.awayScore = 21
        game.possession = TeamSide.HOME
        game.quarter = 4
        game.clock = "1:45"
        game.ballLocation = 25
        game.down = 2
        game.yardsToGo = 7
        game.tvChannel = TVChannel.CBS
        game.homeTeamRank = 3
        game.homeWins = 9
        game.homeLosses = 1
        game.awayTeamRank = 15
        game.awayWins = 7
        game.awayLosses = 3
        game.subdivision = Subdivision.FBS
        game.timestamp = "2024-11-23T19:00:00Z"
        game.winProbability = 0.85
        game.season = 2024
        game.week = 12
        game.waitingOn = TeamSide.AWAY
        game.numPlays = 67
        game.homeTimeouts = 1
        game.awayTimeouts = 2
        game.coinTossWinner = TeamSide.HOME
        game.coinTossChoice = CoinTossChoice.RECEIVE
        game.overtimeCoinTossWinner = TeamSide.AWAY
        game.overtimeCoinTossChoice = OvertimeCoinTossChoice.OFFENSE
        game.homePlatformId = "alabama_discord"
        game.awayPlatformId = "auburn_discord"
        game.lastMessageTimestamp = "2024-11-23T19:45:00Z"
        game.gameTimer = "00:15:30"
        game.gameWarning = GameWarning.NONE
        game.currentPlayType = PlayType.NORMAL
        game.currentPlayId = 234
        game.clockStopped = true
        game.requestMessageId = listOf("msg_123", "msg_456", "msg_789")
        game.gameStatus = GameStatus.IN_PROGRESS
        game.gameType = GameType.CONFERENCE_GAME
        game.gameMode = GameMode.NORMAL
        game.overtimeHalf = 0
        game.closeGame = false
        game.closeGamePinged = false
        game.upsetAlert = false
        game.upsetAlertPinged = false

        assertEquals(456, game.gameId)
        assertEquals("Alabama", game.homeTeam)
        assertEquals("Auburn", game.awayTeam)
        assertEquals(listOf("head_coach", "offensive_coordinator", "defensive_coordinator"), game.homeCoaches)
        assertEquals(listOf("away_head_coach", "away_offensive_coordinator"), game.awayCoaches)
        assertEquals(listOf("123456789", "987654321", "555666777"), game.homeCoachDiscordIds)
        assertEquals(listOf("111222333", "444555666"), game.awayCoachDiscordIds)
        assertEquals(OffensivePlaybook.AIR_RAID, game.homeOffensivePlaybook)
        assertEquals(OffensivePlaybook.SPREAD, game.awayOffensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, game.homeDefensivePlaybook)
        assertEquals(DefensivePlaybook.THREE_FOUR, game.awayDefensivePlaybook)
        assertEquals(35, game.homeScore)
        assertEquals(21, game.awayScore)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(4, game.quarter)
        assertEquals("1:45", game.clock)
        assertEquals(25, game.ballLocation)
        assertEquals(2, game.down)
        assertEquals(7, game.yardsToGo)
        assertEquals(TVChannel.CBS, game.tvChannel)
        assertEquals(3, game.homeTeamRank)
        assertEquals(9, game.homeWins)
        assertEquals(1, game.homeLosses)
        assertEquals(15, game.awayTeamRank)
        assertEquals(7, game.awayWins)
        assertEquals(3, game.awayLosses)
        assertEquals(Subdivision.FBS, game.subdivision)
        assertEquals("2024-11-23T19:00:00Z", game.timestamp)
        assertEquals(0.85, game.winProbability)
        assertEquals(2024, game.season)
        assertEquals(12, game.week)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(67, game.numPlays)
        assertEquals(1, game.homeTimeouts)
        assertEquals(2, game.awayTimeouts)
        assertEquals(TeamSide.HOME, game.coinTossWinner)
        assertEquals(CoinTossChoice.RECEIVE, game.coinTossChoice)
        assertEquals(TeamSide.AWAY, game.overtimeCoinTossWinner)
        assertEquals(OvertimeCoinTossChoice.OFFENSE, game.overtimeCoinTossChoice)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, game.homePlatform)
        assertEquals("alabama_discord", game.homePlatformId)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, game.awayPlatform)
        assertEquals("auburn_discord", game.awayPlatformId)
        assertEquals("2024-11-23T19:45:00Z", game.lastMessageTimestamp)
        assertEquals("00:15:30", game.gameTimer)
        assertEquals(GameWarning.NONE, game.gameWarning)
        assertEquals(PlayType.NORMAL, game.currentPlayType)
        assertEquals(234, game.currentPlayId)
        assertTrue(game.clockStopped)
        assertEquals(listOf("msg_123", "msg_456", "msg_789"), game.requestMessageId)
        assertEquals(GameStatus.IN_PROGRESS, game.gameStatus)
        assertEquals(GameType.CONFERENCE_GAME, game.gameType)
        assertEquals(GameMode.NORMAL, game.gameMode)
        assertEquals(0, game.overtimeHalf)
        assertFalse(game.closeGame)
        assertFalse(game.closeGamePinged)
        assertFalse(game.upsetAlert)
        assertFalse(game.upsetAlertPinged)
    }
}
