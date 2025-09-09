package com.fcfb.arceus.model

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GameStatsTest {
    @Test
    fun `test GameStats entity annotations`() {
        val gameStats = GameStats()

        // Test that the class has the correct JPA annotations
        val entityAnnotation = GameStats::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = GameStats::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("game_stats", tableAnnotation.name)
    }

    @Test
    fun `test GameStats default constructor`() {
        val gameStats = GameStats()

        assertEquals(0, gameStats.id)
        assertEquals(0, gameStats.gameId)
        assertNull(gameStats.team)
        assertNull(gameStats.tvChannel)
        assertEquals(listOf<String>(), gameStats.coaches)
        assertNull(gameStats.offensivePlaybook)
        assertNull(gameStats.defensivePlaybook)
        assertNull(gameStats.season)
        assertNull(gameStats.week)
        assertNull(gameStats.subdivision)
        assertEquals(0, gameStats.score)
        assertEquals(0, gameStats.passAttempts)
        assertEquals(0, gameStats.passCompletions)
        assertEquals(0.0, gameStats.passCompletionPercentage)
        assertEquals(0, gameStats.passYards)
        assertEquals(0, gameStats.longestPass)
        assertEquals(0, gameStats.sacksAllowed)
        assertEquals(0, gameStats.sacksForced)
        assertEquals(0, gameStats.rushAttempts)
        assertEquals(0, gameStats.rushSuccesses)
        assertEquals(0.0, gameStats.rushSuccessPercentage)
        assertEquals(0, gameStats.passSuccesses)
        assertEquals(0.0, gameStats.passSuccessPercentage)
        assertEquals(0, gameStats.rushYards)
        assertEquals(0, gameStats.longestRun)
        assertEquals(0, gameStats.totalYards)
        assertEquals(0, gameStats.interceptionsLost)
        assertEquals(0, gameStats.interceptionsForced)
        assertEquals(0, gameStats.fumblesLost)
        assertEquals(0, gameStats.fumblesForced)
        assertEquals(0, gameStats.turnoversLost)
        assertEquals(0, gameStats.turnoversForced)
        assertEquals(0, gameStats.turnoverTouchdownsLost)
        assertEquals(0, gameStats.turnoverTouchdownsForced)
        assertEquals(0, gameStats.fieldGoalMade)
        assertEquals(0, gameStats.fieldGoalAttempts)
        assertEquals(0.0, gameStats.fieldGoalPercentage)
        assertEquals(0, gameStats.longestFieldGoal)
        assertEquals(0, gameStats.blockedOpponentFieldGoals)
        assertEquals(0, gameStats.fieldGoalTouchdown)
        assertEquals(0, gameStats.puntsAttempted)
        assertEquals(0, gameStats.longestPunt)
        assertEquals(0.0, gameStats.averagePuntLength)
        assertEquals(0, gameStats.blockedOpponentPunt)
        assertEquals(0, gameStats.puntReturnTd)
        assertEquals(0.0, gameStats.puntReturnTdPercentage)
        assertEquals(0, gameStats.numberOfKickoffs)
        assertEquals(0, gameStats.onsideAttempts)
        assertEquals(0, gameStats.onsideSuccess)
        assertEquals(0.0, gameStats.onsideSuccessPercentage)
        assertEquals(0, gameStats.normalKickoffAttempts)
        assertEquals(0, gameStats.touchbacks)
        assertEquals(0.0, gameStats.touchbackPercentage)
        assertEquals(0, gameStats.kickReturnTd)
        assertEquals(0.0, gameStats.kickReturnTdPercentage)
        assertEquals(0, gameStats.numberOfDrives)
        assertEquals(0, gameStats.timeOfPossession)
        assertEquals(0, gameStats.q1Score)
        assertEquals(0, gameStats.q2Score)
        assertEquals(0, gameStats.q3Score)
        assertEquals(0, gameStats.q4Score)
        assertEquals(0, gameStats.otScore)
        assertEquals(0, gameStats.touchdowns)
        assertEquals(0.0, gameStats.averageOffensiveDiff)
        assertEquals(0.0, gameStats.averageDefensiveDiff)
        assertEquals(0.0, gameStats.averageOffensiveSpecialTeamsDiff)
        assertEquals(0.0, gameStats.averageDefensiveSpecialTeamsDiff)
        assertEquals(0.0, gameStats.averageYardsPerPlay)
        assertEquals(0, gameStats.firstDowns)
        assertEquals(0, gameStats.thirdDownConversionSuccess)
        assertEquals(0, gameStats.thirdDownConversionAttempts)
        assertEquals(0.0, gameStats.thirdDownConversionPercentage)
        assertEquals(0, gameStats.fourthDownConversionSuccess)
        assertEquals(0, gameStats.fourthDownConversionAttempts)
        assertEquals(0.0, gameStats.fourthDownConversionPercentage)
        assertEquals(0, gameStats.largestLead)
        assertEquals(0, gameStats.largestDeficit)
        assertEquals(0, gameStats.passTouchdowns)
        assertEquals(0, gameStats.rushTouchdowns)
        assertEquals(GameType.SCRIMMAGE, gameStats.gameType)
        assertNull(gameStats.gameStatus)
        assertEquals(0, gameStats.redZoneAttempts)
        assertEquals(0, gameStats.redZoneSuccesses)
        assertEquals(0.0, gameStats.redZoneSuccessPercentage)
        assertEquals(0.0, gameStats.redZonePercentage)
        assertEquals(0.9, gameStats.averageDiff)
        assertEquals(0, gameStats.turnoverDifferential)
        assertEquals(0, gameStats.pickSixesThrown)
        assertEquals(0, gameStats.pickSixesForced)
        assertEquals(0, gameStats.fumbleReturnTdsCommitted)
        assertEquals(0, gameStats.fumbleReturnTdsForced)
        assertEquals(0, gameStats.safetiesForced)
        assertEquals(0, gameStats.safetiesCommitted)
        assertEquals(0.0, gameStats.averageResponseSpeed)
        assertNull(gameStats.lastModifiedTs)
        assertEquals(1500.0, gameStats.teamElo)
    }

    @Test
    fun `test GameStats property mutability`() {
        val gameStats = GameStats()

        // Test basic properties
        gameStats.id = 1
        gameStats.gameId = 123
        gameStats.team = "Alabama"
        gameStats.tvChannel = TVChannel.ESPN
        gameStats.coaches = listOf("coach1", "coach2")
        gameStats.offensivePlaybook = OffensivePlaybook.AIR_RAID
        gameStats.defensivePlaybook = DefensivePlaybook.FOUR_THREE
        gameStats.season = 2024
        gameStats.week = 5
        gameStats.subdivision = Subdivision.FBS
        gameStats.score = 28
        gameStats.gameType = GameType.CONFERENCE_GAME
        gameStats.gameStatus = GameStatus.FINAL
        gameStats.lastModifiedTs = "2024-01-01T12:00:00Z"
        gameStats.teamElo = 1650.5

        assertEquals(1, gameStats.id)
        assertEquals(123, gameStats.gameId)
        assertEquals("Alabama", gameStats.team)
        assertEquals(TVChannel.ESPN, gameStats.tvChannel)
        assertEquals(listOf("coach1", "coach2"), gameStats.coaches)
        assertEquals(OffensivePlaybook.AIR_RAID, gameStats.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, gameStats.defensivePlaybook)
        assertEquals(2024, gameStats.season)
        assertEquals(5, gameStats.week)
        assertEquals(Subdivision.FBS, gameStats.subdivision)
        assertEquals(28, gameStats.score)
        assertEquals(GameType.CONFERENCE_GAME, gameStats.gameType)
        assertEquals(GameStatus.FINAL, gameStats.gameStatus)
        assertEquals("2024-01-01T12:00:00Z", gameStats.lastModifiedTs)
        assertEquals(1650.5, gameStats.teamElo)
    }

    @Test
    fun `test GameStats passing statistics`() {
        val gameStats = GameStats()

        gameStats.passAttempts = 35
        gameStats.passCompletions = 25
        gameStats.passCompletionPercentage = 71.4
        gameStats.passYards = 350
        gameStats.longestPass = 45
        gameStats.sacksAllowed = 2
        gameStats.passSuccesses = 20
        gameStats.passSuccessPercentage = 57.1
        gameStats.passTouchdowns = 3

        assertEquals(35, gameStats.passAttempts)
        assertEquals(25, gameStats.passCompletions)
        assertEquals(71.4, gameStats.passCompletionPercentage)
        assertEquals(350, gameStats.passYards)
        assertEquals(45, gameStats.longestPass)
        assertEquals(2, gameStats.sacksAllowed)
        assertEquals(20, gameStats.passSuccesses)
        assertEquals(57.1, gameStats.passSuccessPercentage)
        assertEquals(3, gameStats.passTouchdowns)
    }

    @Test
    fun `test GameStats rushing statistics`() {
        val gameStats = GameStats()

        gameStats.rushAttempts = 40
        gameStats.rushSuccesses = 25
        gameStats.rushSuccessPercentage = 62.5
        gameStats.rushYards = 180
        gameStats.longestRun = 35
        gameStats.rushTouchdowns = 2

        assertEquals(40, gameStats.rushAttempts)
        assertEquals(25, gameStats.rushSuccesses)
        assertEquals(62.5, gameStats.rushSuccessPercentage)
        assertEquals(180, gameStats.rushYards)
        assertEquals(35, gameStats.longestRun)
        assertEquals(2, gameStats.rushTouchdowns)
    }

    @Test
    fun `test GameStats turnover statistics`() {
        val gameStats = GameStats()

        gameStats.interceptionsLost = 1
        gameStats.interceptionsForced = 2
        gameStats.fumblesLost = 1
        gameStats.fumblesForced = 1
        gameStats.turnoversLost = 2
        gameStats.turnoversForced = 3
        gameStats.turnoverTouchdownsLost = 0
        gameStats.turnoverTouchdownsForced = 1
        gameStats.turnoverDifferential = 1
        gameStats.pickSixesThrown = 0
        gameStats.pickSixesForced = 1
        gameStats.fumbleReturnTdsCommitted = 0
        gameStats.fumbleReturnTdsForced = 0

        assertEquals(1, gameStats.interceptionsLost)
        assertEquals(2, gameStats.interceptionsForced)
        assertEquals(1, gameStats.fumblesLost)
        assertEquals(1, gameStats.fumblesForced)
        assertEquals(2, gameStats.turnoversLost)
        assertEquals(3, gameStats.turnoversForced)
        assertEquals(0, gameStats.turnoverTouchdownsLost)
        assertEquals(1, gameStats.turnoverTouchdownsForced)
        assertEquals(1, gameStats.turnoverDifferential)
        assertEquals(0, gameStats.pickSixesThrown)
        assertEquals(1, gameStats.pickSixesForced)
        assertEquals(0, gameStats.fumbleReturnTdsCommitted)
        assertEquals(0, gameStats.fumbleReturnTdsForced)
    }

    @Test
    fun `test GameStats kicking statistics`() {
        val gameStats = GameStats()

        gameStats.fieldGoalMade = 2
        gameStats.fieldGoalAttempts = 3
        gameStats.fieldGoalPercentage = 66.7
        gameStats.longestFieldGoal = 45
        gameStats.blockedOpponentFieldGoals = 0
        gameStats.fieldGoalTouchdown = 0

        assertEquals(2, gameStats.fieldGoalMade)
        assertEquals(3, gameStats.fieldGoalAttempts)
        assertEquals(66.7, gameStats.fieldGoalPercentage)
        assertEquals(45, gameStats.longestFieldGoal)
        assertEquals(0, gameStats.blockedOpponentFieldGoals)
        assertEquals(0, gameStats.fieldGoalTouchdown)
    }

    @Test
    fun `test GameStats punting statistics`() {
        val gameStats = GameStats()

        gameStats.puntsAttempted = 5
        gameStats.longestPunt = 55
        gameStats.averagePuntLength = 42.5
        gameStats.blockedOpponentPunt = 0
        gameStats.puntReturnTd = 0
        gameStats.puntReturnTdPercentage = 0.0

        assertEquals(5, gameStats.puntsAttempted)
        assertEquals(55, gameStats.longestPunt)
        assertEquals(42.5, gameStats.averagePuntLength)
        assertEquals(0, gameStats.blockedOpponentPunt)
        assertEquals(0, gameStats.puntReturnTd)
        assertEquals(0.0, gameStats.puntReturnTdPercentage)
    }

    @Test
    fun `test GameStats kickoff statistics`() {
        val gameStats = GameStats()

        gameStats.numberOfKickoffs = 6
        gameStats.onsideAttempts = 0
        gameStats.onsideSuccess = 0
        gameStats.onsideSuccessPercentage = 0.0
        gameStats.normalKickoffAttempts = 6
        gameStats.touchbacks = 3
        gameStats.touchbackPercentage = 50.0
        gameStats.kickReturnTd = 0
        gameStats.kickReturnTdPercentage = 0.0

        assertEquals(6, gameStats.numberOfKickoffs)
        assertEquals(0, gameStats.onsideAttempts)
        assertEquals(0, gameStats.onsideSuccess)
        assertEquals(0.0, gameStats.onsideSuccessPercentage)
        assertEquals(6, gameStats.normalKickoffAttempts)
        assertEquals(3, gameStats.touchbacks)
        assertEquals(50.0, gameStats.touchbackPercentage)
        assertEquals(0, gameStats.kickReturnTd)
        assertEquals(0.0, gameStats.kickReturnTdPercentage)
    }

    @Test
    fun `test GameStats drive and possession statistics`() {
        val gameStats = GameStats()

        gameStats.numberOfDrives = 12
        gameStats.timeOfPossession = 1800
        gameStats.firstDowns = 22
        gameStats.averageYardsPerPlay = 5.8

        assertEquals(12, gameStats.numberOfDrives)
        assertEquals(1800, gameStats.timeOfPossession)
        assertEquals(22, gameStats.firstDowns)
        assertEquals(5.8, gameStats.averageYardsPerPlay)
    }

    @Test
    fun `test GameStats conversion statistics`() {
        val gameStats = GameStats()

        gameStats.thirdDownConversionSuccess = 8
        gameStats.thirdDownConversionAttempts = 15
        gameStats.thirdDownConversionPercentage = 53.3
        gameStats.fourthDownConversionSuccess = 1
        gameStats.fourthDownConversionAttempts = 2
        gameStats.fourthDownConversionPercentage = 50.0

        assertEquals(8, gameStats.thirdDownConversionSuccess)
        assertEquals(15, gameStats.thirdDownConversionAttempts)
        assertEquals(53.3, gameStats.thirdDownConversionPercentage)
        assertEquals(1, gameStats.fourthDownConversionSuccess)
        assertEquals(2, gameStats.fourthDownConversionAttempts)
        assertEquals(50.0, gameStats.fourthDownConversionPercentage)
    }

    @Test
    fun `test GameStats quarter scoring`() {
        val gameStats = GameStats()

        gameStats.q1Score = 7
        gameStats.q2Score = 14
        gameStats.q3Score = 0
        gameStats.q4Score = 7
        gameStats.otScore = 0
        gameStats.touchdowns = 4

        assertEquals(7, gameStats.q1Score)
        assertEquals(14, gameStats.q2Score)
        assertEquals(0, gameStats.q3Score)
        assertEquals(7, gameStats.q4Score)
        assertEquals(0, gameStats.otScore)
        assertEquals(4, gameStats.touchdowns)
    }

    @Test
    fun `test GameStats red zone statistics`() {
        val gameStats = GameStats()

        gameStats.redZoneAttempts = 5
        gameStats.redZoneSuccesses = 4
        gameStats.redZoneSuccessPercentage = 80.0
        gameStats.redZonePercentage = 41.7

        assertEquals(5, gameStats.redZoneAttempts)
        assertEquals(4, gameStats.redZoneSuccesses)
        assertEquals(80.0, gameStats.redZoneSuccessPercentage)
        assertEquals(41.7, gameStats.redZonePercentage)
    }

    @Test
    fun `test GameStats defensive statistics`() {
        val gameStats = GameStats()

        gameStats.sacksForced = 3
        gameStats.safetiesForced = 0
        gameStats.safetiesCommitted = 0

        assertEquals(3, gameStats.sacksForced)
        assertEquals(0, gameStats.safetiesForced)
        assertEquals(0, gameStats.safetiesCommitted)
    }

    @Test
    fun `test GameStats differential statistics`() {
        val gameStats = GameStats()

        gameStats.averageOffensiveDiff = 2.5
        gameStats.averageDefensiveDiff = -1.2
        gameStats.averageOffensiveSpecialTeamsDiff = 0.8
        gameStats.averageDefensiveSpecialTeamsDiff = -0.3
        gameStats.averageDiff = 1.8
        gameStats.largestLead = 21
        gameStats.largestDeficit = 0

        assertEquals(2.5, gameStats.averageOffensiveDiff)
        assertEquals(-1.2, gameStats.averageDefensiveDiff)
        assertEquals(0.8, gameStats.averageOffensiveSpecialTeamsDiff)
        assertEquals(-0.3, gameStats.averageDefensiveSpecialTeamsDiff)
        assertEquals(1.8, gameStats.averageDiff)
        assertEquals(21, gameStats.largestLead)
        assertEquals(0, gameStats.largestDeficit)
    }

    @Test
    fun `test GameStats response speed`() {
        val gameStats = GameStats()

        gameStats.averageResponseSpeed = 45.5

        assertEquals(45.5, gameStats.averageResponseSpeed)
    }

    @Test
    fun `test GameStats team ELO`() {
        val gameStats = GameStats()

        // Test default value
        assertEquals(1500.0, gameStats.teamElo)

        // Test setting custom ELO values
        gameStats.teamElo = 1750.25
        assertEquals(1750.25, gameStats.teamElo)

        gameStats.teamElo = 1200.0
        assertEquals(1200.0, gameStats.teamElo)

        gameStats.teamElo = 2000.0
        assertEquals(2000.0, gameStats.teamElo)
    }

    @Test
    fun `test GameStats with all enum values`() {
        val gameStats = GameStats()

        // Test all TVChannel values
        TVChannel.entries.forEach { tvChannel ->
            gameStats.tvChannel = tvChannel
            assertEquals(tvChannel, gameStats.tvChannel)
        }

        // Test all OffensivePlaybook values
        OffensivePlaybook.entries.forEach { offensivePlaybook ->
            gameStats.offensivePlaybook = offensivePlaybook
            assertEquals(offensivePlaybook, gameStats.offensivePlaybook)
        }

        // Test all DefensivePlaybook values
        DefensivePlaybook.entries.forEach { defensivePlaybook ->
            gameStats.defensivePlaybook = defensivePlaybook
            assertEquals(defensivePlaybook, gameStats.defensivePlaybook)
        }

        // Test all Subdivision values
        Subdivision.entries.forEach { subdivision ->
            gameStats.subdivision = subdivision
            assertEquals(subdivision, gameStats.subdivision)
        }

        // Test all GameType values
        GameType.entries.forEach { gameType ->
            gameStats.gameType = gameType
            assertEquals(gameType, gameStats.gameType)
        }
    }

    @Test
    fun `test GameStats with null optional fields`() {
        val gameStats = GameStats()
        gameStats.team = null
        gameStats.tvChannel = null
        gameStats.coaches = null
        gameStats.offensivePlaybook = null
        gameStats.defensivePlaybook = null
        gameStats.season = null
        gameStats.week = null
        gameStats.subdivision = null
        gameStats.gameStatus = null
        gameStats.lastModifiedTs = null

        assertNull(gameStats.team)
        assertNull(gameStats.tvChannel)
        assertNull(gameStats.coaches)
        assertNull(gameStats.offensivePlaybook)
        assertNull(gameStats.defensivePlaybook)
        assertNull(gameStats.season)
        assertNull(gameStats.week)
        assertNull(gameStats.subdivision)
        assertNull(gameStats.gameStatus)
        assertNull(gameStats.lastModifiedTs)
    }

    @Test
    fun `test GameStats total yards calculation`() {
        val gameStats = GameStats()

        gameStats.passYards = 250
        gameStats.rushYards = 150
        gameStats.totalYards = 400

        assertEquals(250, gameStats.passYards)
        assertEquals(150, gameStats.rushYards)
        assertEquals(400, gameStats.totalYards)
    }

    @Test
    fun `test GameStats complete game scenario`() {
        val gameStats = GameStats()

        // Set up a complete game statistics scenario
        gameStats.id = 123
        gameStats.gameId = 456
        gameStats.team = "Alabama"
        gameStats.tvChannel = TVChannel.CBS
        gameStats.coaches = listOf("head_coach", "offensive_coordinator", "defensive_coordinator")
        gameStats.offensivePlaybook = OffensivePlaybook.AIR_RAID
        gameStats.defensivePlaybook = DefensivePlaybook.FOUR_THREE
        gameStats.season = 2024
        gameStats.week = 8
        gameStats.subdivision = Subdivision.FBS
        gameStats.score = 35
        gameStats.gameType = GameType.CONFERENCE_GAME
        gameStats.gameStatus = GameStatus.FINAL
        gameStats.lastModifiedTs = "2024-10-15T18:30:00Z"

        // Verify all properties
        assertEquals(123, gameStats.id)
        assertEquals(456, gameStats.gameId)
        assertEquals("Alabama", gameStats.team)
        assertEquals(TVChannel.CBS, gameStats.tvChannel)
        assertEquals(listOf("head_coach", "offensive_coordinator", "defensive_coordinator"), gameStats.coaches)
        assertEquals(OffensivePlaybook.AIR_RAID, gameStats.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, gameStats.defensivePlaybook)
        assertEquals(2024, gameStats.season)
        assertEquals(8, gameStats.week)
        assertEquals(Subdivision.FBS, gameStats.subdivision)
        assertEquals(35, gameStats.score)
        assertEquals(GameType.CONFERENCE_GAME, gameStats.gameType)
        assertEquals(GameStatus.FINAL, gameStats.gameStatus)
        assertEquals("2024-10-15T18:30:00Z", gameStats.lastModifiedTs)
    }
}
