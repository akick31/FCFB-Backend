package com.fcfb.arceus.util

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EnumConverterTest {
    private lateinit var offensivePlaybookConverter: OffensivePlaybookConverter
    private lateinit var defensivePlaybookConverter: DefensivePlaybookConverter
    private lateinit var subdivisionConverter: SubdivisionConverter
    private lateinit var resultConverter: ResultConverter
    private lateinit var coinTossCallConverter: CoinTossCallConverter
    private lateinit var gameModeConverter: GameModeConverter

    @BeforeEach
    fun setup() {
        offensivePlaybookConverter = OffensivePlaybookConverter()
        defensivePlaybookConverter = DefensivePlaybookConverter()
        subdivisionConverter = SubdivisionConverter()
        resultConverter = ResultConverter()
        coinTossCallConverter = CoinTossCallConverter()
        gameModeConverter = GameModeConverter()
    }

    @Test
    fun `OffensivePlaybookConverter should be properly annotated`() {
        val converterAnnotation = OffensivePlaybookConverter::class.annotations.find { it is javax.persistence.Converter }
        assertNotNull(converterAnnotation, "OffensivePlaybookConverter should be annotated with @Converter")
    }

    @Test
    fun `OffensivePlaybookConverter should convert enum to database column`() {
        val result = offensivePlaybookConverter.convertToDatabaseColumn(OffensivePlaybook.AIR_RAID)
        assertEquals(OffensivePlaybook.AIR_RAID.description, result)
    }

    @Test
    fun `OffensivePlaybookConverter should handle null enum to database column`() {
        val result = offensivePlaybookConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun `OffensivePlaybookConverter should convert database column to enum`() {
        val description = OffensivePlaybook.SPREAD.description
        val result = offensivePlaybookConverter.convertToEntityAttribute(description)
        assertEquals(OffensivePlaybook.SPREAD, result)
    }

    @Test
    fun `OffensivePlaybookConverter should handle null database column`() {
        val result = offensivePlaybookConverter.convertToEntityAttribute(null)
        assertNull(result)
    }

    @Test
    fun `OffensivePlaybookConverter should handle all offensive playbook types`() {
        val playbooks =
            listOf(
                OffensivePlaybook.AIR_RAID,
                OffensivePlaybook.SPREAD,
                OffensivePlaybook.PRO,
                OffensivePlaybook.FLEXBONE,
            )

        playbooks.forEach { playbook ->
            val dbValue = offensivePlaybookConverter.convertToDatabaseColumn(playbook)
            val convertedBack = offensivePlaybookConverter.convertToEntityAttribute(dbValue)
            assertEquals(playbook, convertedBack, "Failed to convert $playbook")
        }
    }

    @Test
    fun `DefensivePlaybookConverter should be properly annotated`() {
        val converterAnnotation = DefensivePlaybookConverter::class.annotations.find { it is javax.persistence.Converter }
        assertNotNull(converterAnnotation, "DefensivePlaybookConverter should be annotated with @Converter")
    }

    @Test
    fun `DefensivePlaybookConverter should convert enum to database column`() {
        val result = defensivePlaybookConverter.convertToDatabaseColumn(DefensivePlaybook.FOUR_THREE)
        assertEquals(DefensivePlaybook.FOUR_THREE.description, result)
    }

    @Test
    fun `DefensivePlaybookConverter should handle null enum to database column`() {
        val result = defensivePlaybookConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun `DefensivePlaybookConverter should convert database column to enum`() {
        val description = DefensivePlaybook.THREE_FOUR.description
        val result = defensivePlaybookConverter.convertToEntityAttribute(description)
        assertEquals(DefensivePlaybook.THREE_FOUR, result)
    }

    @Test
    fun `DefensivePlaybookConverter should handle null database column`() {
        val result = defensivePlaybookConverter.convertToEntityAttribute(null)
        assertNull(result)
    }

    @Test
    fun `DefensivePlaybookConverter should handle invalid database value`() {
        val result = defensivePlaybookConverter.convertToEntityAttribute("INVALID_PLAYBOOK")
        assertNull(result, "Should return null for invalid playbook")
    }

    @Test
    fun `DefensivePlaybookConverter should handle all defensive playbook types`() {
        val playbooks =
            listOf(
                DefensivePlaybook.FOUR_THREE,
                DefensivePlaybook.THREE_FOUR,
                DefensivePlaybook.THREE_THREE_FIVE,
                DefensivePlaybook.FIVE_TWO,
            )

        playbooks.forEach { playbook ->
            val dbValue = defensivePlaybookConverter.convertToDatabaseColumn(playbook)
            val convertedBack = defensivePlaybookConverter.convertToEntityAttribute(dbValue)
            assertEquals(playbook, convertedBack, "Failed to convert $playbook")
        }
    }

    @Test
    fun `SubdivisionConverter should be properly annotated`() {
        val converterAnnotation = SubdivisionConverter::class.annotations.find { it is javax.persistence.Converter }
        assertNotNull(converterAnnotation, "SubdivisionConverter should be annotated with @Converter")
    }

    @Test
    fun `SubdivisionConverter should convert enum to database column`() {
        val result = subdivisionConverter.convertToDatabaseColumn(Subdivision.FBS)
        assertEquals(Subdivision.FBS.description, result)
    }

    @Test
    fun `SubdivisionConverter should handle null enum to database column`() {
        val result = subdivisionConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun `SubdivisionConverter should convert database column to enum`() {
        val description = Subdivision.FCS.description
        val result = subdivisionConverter.convertToEntityAttribute(description)
        assertEquals(Subdivision.FCS, result)
    }

    @Test
    fun `SubdivisionConverter should handle null database column`() {
        val result = subdivisionConverter.convertToEntityAttribute(null)
        assertNull(result)
    }

    @Test
    fun `ResultConverter should be properly annotated`() {
        val converterAnnotation = ResultConverter::class.annotations.find { it is javax.persistence.Converter }
        assertNotNull(converterAnnotation, "ResultConverter should be annotated with @Converter")
    }

    @Test
    fun `ResultConverter should convert enum to database column`() {
        val result = resultConverter.convertToDatabaseColumn(Scenario.TOUCHDOWN)
        assertEquals(Scenario.TOUCHDOWN.description, result)
    }

    @Test
    fun `ResultConverter should handle null enum to database column`() {
        val result = resultConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun `ResultConverter should convert database column to enum`() {
        val description = Scenario.GOOD.description
        val result = resultConverter.convertToEntityAttribute(description)
        assertEquals(Scenario.GOOD, result)
    }

    @Test
    fun `ResultConverter should handle null database column`() {
        val result = resultConverter.convertToEntityAttribute(null)
        assertNull(result)
    }

    @Test
    fun `CoinTossCallConverter should be properly annotated`() {
        val converterAnnotation = CoinTossCallConverter::class.annotations.find { it is javax.persistence.Converter }
        assertNotNull(converterAnnotation, "CoinTossCallConverter should be annotated with @Converter")
    }

    @Test
    fun `CoinTossCallConverter should convert enum to database column`() {
        val result = coinTossCallConverter.convertToDatabaseColumn(CoinTossCall.HEADS)
        assertEquals(CoinTossCall.HEADS.description, result)
    }

    @Test
    fun `CoinTossCallConverter should handle null enum to database column`() {
        val result = coinTossCallConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun `CoinTossCallConverter should convert database column to enum`() {
        val description = CoinTossCall.TAILS.description
        val result = coinTossCallConverter.convertToEntityAttribute(description)
        assertEquals(CoinTossCall.TAILS, result)
    }

    @Test
    fun `CoinTossCallConverter should handle null database column`() {
        val result = coinTossCallConverter.convertToEntityAttribute(null)
        assertNull(result)
    }

    @Test
    fun `GameModeConverter should be properly annotated`() {
        val converterAnnotation = GameModeConverter::class.annotations.find { it is javax.persistence.Converter }
        assertNotNull(converterAnnotation, "GameModeConverter should be annotated with @Converter")
    }

    @Test
    fun `GameModeConverter should convert enum to database column`() {
        val result = gameModeConverter.convertToDatabaseColumn(GameMode.NORMAL)
        assertEquals(GameMode.NORMAL.description, result)
    }

    @Test
    fun `GameModeConverter should handle null enum to database column`() {
        val result = gameModeConverter.convertToDatabaseColumn(null)
        assertNull(result)
    }

    @Test
    fun `GameModeConverter should convert database column to enum`() {
        val description = GameMode.NORMAL.description
        val result = gameModeConverter.convertToEntityAttribute(description)
        assertEquals(GameMode.NORMAL, result)
    }

    @Test
    fun `GameModeConverter should handle null database column`() {
        val result = gameModeConverter.convertToEntityAttribute(null)
        assertNull(result)
    }

    @Test
    fun `all converters should be instantiable`() {
        assertNotNull(OffensivePlaybookConverter())
        assertNotNull(DefensivePlaybookConverter())
        assertNotNull(SubdivisionConverter())
        assertNotNull(ResultConverter())
        assertNotNull(CoinTossCallConverter())
        assertNotNull(GameModeConverter())
    }
}
