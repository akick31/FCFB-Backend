package com.fcfb.arceus.model

import com.fcfb.arceus.enums.play.Scenario
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RangesTest {
    @Test
    fun `test Ranges entity annotations`() {
        val ranges = Ranges()

        val entityAnnotation = Ranges::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = Ranges::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("ranges", tableAnnotation.name)
    }

    @Test
    fun `test Ranges default constructor`() {
        val ranges = Ranges()

        assertEquals(0, ranges.id)
        assertNull(ranges.playType)
        assertNull(ranges.offensivePlaybook)
        assertNull(ranges.defensivePlaybook)
        assertNull(ranges.ballLocationLower)
        assertNull(ranges.ballLocationUpper)
        assertNull(ranges.distance)
        assertNull(ranges.result)
        assertEquals(0, ranges.playTime)
        assertNull(ranges.lowerRange)
        assertNull(ranges.upperRange)
    }

    @Test
    fun `test Ranges parameterized constructor`() {
        val ranges =
            Ranges(
                playType = "NORMAL",
                offensivePlaybook = "AIR_RAID",
                defensivePlaybook = "FOUR_THREE",
                ballLocationLower = 20,
                ballLocationUpper = 80,
                distance = 10,
                result = Scenario.GAIN_OF_12_YARDS,
                playTime = 25,
                lowerRange = 5,
                upperRange = 15,
            )

        assertEquals("NORMAL", ranges.playType)
        assertEquals("AIR_RAID", ranges.offensivePlaybook)
        assertEquals("FOUR_THREE", ranges.defensivePlaybook)
        assertEquals(20, ranges.ballLocationLower)
        assertEquals(80, ranges.ballLocationUpper)
        assertEquals(10, ranges.distance)
        assertEquals(Scenario.GAIN_OF_12_YARDS, ranges.result)
        assertEquals(25, ranges.playTime)
        assertEquals(5, ranges.lowerRange)
        assertEquals(15, ranges.upperRange)
    }

    @Test
    fun `test Ranges property mutability`() {
        val ranges = Ranges()

        ranges.id = 1
        ranges.playType = "KICKOFF"
        ranges.offensivePlaybook = "SPREAD"
        ranges.defensivePlaybook = "THREE_FOUR"
        ranges.ballLocationLower = 30
        ranges.ballLocationUpper = 70
        ranges.distance = 15
        ranges.result = Scenario.TWENTY_YARD_RETURN
        ranges.playTime = 30
        ranges.lowerRange = 8
        ranges.upperRange = 20

        assertEquals(1, ranges.id)
        assertEquals("KICKOFF", ranges.playType)
        assertEquals("SPREAD", ranges.offensivePlaybook)
        assertEquals("THREE_FOUR", ranges.defensivePlaybook)
        assertEquals(30, ranges.ballLocationLower)
        assertEquals(70, ranges.ballLocationUpper)
        assertEquals(15, ranges.distance)
        assertEquals(Scenario.TWENTY_YARD_RETURN, ranges.result)
        assertEquals(30, ranges.playTime)
        assertEquals(8, ranges.lowerRange)
        assertEquals(20, ranges.upperRange)
    }

    @Test
    fun `test Ranges with null optional fields`() {
        val ranges = Ranges()
        ranges.playType = null
        ranges.offensivePlaybook = null
        ranges.defensivePlaybook = null
        ranges.ballLocationLower = null
        ranges.ballLocationUpper = null
        ranges.distance = null
        ranges.result = null
        ranges.lowerRange = null
        ranges.upperRange = null

        assertNull(ranges.playType)
        assertNull(ranges.offensivePlaybook)
        assertNull(ranges.defensivePlaybook)
        assertNull(ranges.ballLocationLower)
        assertNull(ranges.ballLocationUpper)
        assertNull(ranges.distance)
        assertNull(ranges.result)
        assertNull(ranges.lowerRange)
        assertNull(ranges.upperRange)
    }

    @Test
    fun `test Ranges with all Scenario values`() {
        val ranges = Ranges()

        Scenario.entries.forEach { scenario ->
            ranges.result = scenario
            assertEquals(scenario, ranges.result)
        }
    }

    @Test
    fun `test Ranges play type management`() {
        val ranges = Ranges()

        ranges.playType = "NORMAL"
        assertEquals("NORMAL", ranges.playType)

        ranges.playType = "KICKOFF"
        assertEquals("KICKOFF", ranges.playType)

        ranges.playType = "PAT"
        assertEquals("PAT", ranges.playType)

        ranges.playType = null
        assertNull(ranges.playType)
    }

    @Test
    fun `test Ranges playbook management`() {
        val ranges = Ranges()

        ranges.offensivePlaybook = "AIR_RAID"
        assertEquals("AIR_RAID", ranges.offensivePlaybook)

        ranges.offensivePlaybook = "SPREAD"
        assertEquals("SPREAD", ranges.offensivePlaybook)

        ranges.offensivePlaybook = "PRO"
        assertEquals("PRO", ranges.offensivePlaybook)

        ranges.offensivePlaybook = null
        assertNull(ranges.offensivePlaybook)

        ranges.defensivePlaybook = "FOUR_THREE"
        assertEquals("FOUR_THREE", ranges.defensivePlaybook)

        ranges.defensivePlaybook = "THREE_FOUR"
        assertEquals("THREE_FOUR", ranges.defensivePlaybook)

        ranges.defensivePlaybook = "FIVE_TWO"
        assertEquals("FIVE_TWO", ranges.defensivePlaybook)

        ranges.defensivePlaybook = null
        assertNull(ranges.defensivePlaybook)
    }

    @Test
    fun `test Ranges ball location tracking`() {
        val ranges = Ranges()

        ranges.ballLocationLower = 20
        ranges.ballLocationUpper = 80
        assertEquals(20, ranges.ballLocationLower)
        assertEquals(80, ranges.ballLocationUpper)

        ranges.ballLocationLower = 30
        ranges.ballLocationUpper = 70
        assertEquals(30, ranges.ballLocationLower)
        assertEquals(70, ranges.ballLocationUpper)

        ranges.ballLocationLower = null
        ranges.ballLocationUpper = null
        assertNull(ranges.ballLocationLower)
        assertNull(ranges.ballLocationUpper)
    }

    @Test
    fun `test Ranges distance tracking`() {
        val ranges = Ranges()

        ranges.distance = 10
        assertEquals(10, ranges.distance)

        ranges.distance = 25
        assertEquals(25, ranges.distance)

        ranges.distance = 1
        assertEquals(1, ranges.distance)

        ranges.distance = null
        assertNull(ranges.distance)
    }

    @Test
    fun `test Ranges play time tracking`() {
        val ranges = Ranges()

        ranges.playTime = 0
        assertEquals(0, ranges.playTime)

        ranges.playTime = 25
        assertEquals(25, ranges.playTime)

        ranges.playTime = 45
        assertEquals(45, ranges.playTime)

        ranges.playTime = 60
        assertEquals(60, ranges.playTime)
    }

    @Test
    fun `test Ranges range tracking`() {
        val ranges = Ranges()

        ranges.lowerRange = 5
        ranges.upperRange = 15
        assertEquals(5, ranges.lowerRange)
        assertEquals(15, ranges.upperRange)

        ranges.lowerRange = 10
        ranges.upperRange = 25
        assertEquals(10, ranges.lowerRange)
        assertEquals(25, ranges.upperRange)

        ranges.lowerRange = null
        ranges.upperRange = null
        assertNull(ranges.lowerRange)
        assertNull(ranges.upperRange)
    }

    @Test
    fun `test Ranges id management`() {
        val ranges = Ranges()

        ranges.id = 1
        assertEquals(1, ranges.id)

        ranges.id = 100
        assertEquals(100, ranges.id)

        ranges.id = 0
        assertEquals(0, ranges.id)
    }

    @Test
    fun `test Ranges complete scenario`() {
        val ranges = Ranges()

        ranges.id = 123
        ranges.playType = "NORMAL"
        ranges.offensivePlaybook = "AIR_RAID"
        ranges.defensivePlaybook = "FOUR_THREE"
        ranges.ballLocationLower = 20
        ranges.ballLocationUpper = 80
        ranges.distance = 10
        ranges.result = Scenario.GAIN_OF_12_YARDS
        ranges.playTime = 25
        ranges.lowerRange = 5
        ranges.upperRange = 15

        assertEquals(123, ranges.id)
        assertEquals("NORMAL", ranges.playType)
        assertEquals("AIR_RAID", ranges.offensivePlaybook)
        assertEquals("FOUR_THREE", ranges.defensivePlaybook)
        assertEquals(20, ranges.ballLocationLower)
        assertEquals(80, ranges.ballLocationUpper)
        assertEquals(10, ranges.distance)
        assertEquals(Scenario.GAIN_OF_12_YARDS, ranges.result)
        assertEquals(25, ranges.playTime)
        assertEquals(5, ranges.lowerRange)
        assertEquals(15, ranges.upperRange)
    }

    @Test
    fun `test Ranges different play scenarios`() {
        val ranges = Ranges()

        ranges.playType = "KICKOFF"
        ranges.offensivePlaybook = "PRO"
        ranges.defensivePlaybook = "FOUR_THREE"
        ranges.ballLocationLower = 0
        ranges.ballLocationUpper = 100
        ranges.distance = 0
        ranges.result = Scenario.TWENTY_YARD_RETURN
        ranges.playTime = 30
        ranges.lowerRange = 0
        ranges.upperRange = 0

        assertEquals("KICKOFF", ranges.playType)
        assertEquals("PRO", ranges.offensivePlaybook)
        assertEquals("FOUR_THREE", ranges.defensivePlaybook)
        assertEquals(0, ranges.ballLocationLower)
        assertEquals(100, ranges.ballLocationUpper)
        assertEquals(0, ranges.distance)
        assertEquals(Scenario.TWENTY_YARD_RETURN, ranges.result)
        assertEquals(30, ranges.playTime)
        assertEquals(0, ranges.lowerRange)
        assertEquals(0, ranges.upperRange)

        ranges.playType = "PAT"
        ranges.offensivePlaybook = "SPREAD"
        ranges.defensivePlaybook = "THREE_FOUR"
        ranges.ballLocationLower = 3
        ranges.ballLocationUpper = 3
        ranges.distance = 3
        ranges.result = Scenario.GOOD
        ranges.playTime = 15
        ranges.lowerRange = 1
        ranges.upperRange = 1

        assertEquals("PAT", ranges.playType)
        assertEquals("SPREAD", ranges.offensivePlaybook)
        assertEquals("THREE_FOUR", ranges.defensivePlaybook)
        assertEquals(3, ranges.ballLocationLower)
        assertEquals(3, ranges.ballLocationUpper)
        assertEquals(3, ranges.distance)
        assertEquals(Scenario.GOOD, ranges.result)
        assertEquals(15, ranges.playTime)
        assertEquals(1, ranges.lowerRange)
        assertEquals(1, ranges.upperRange)
    }

    @Test
    fun `test Ranges hashCode method`() {
        val ranges1 =
            Ranges(
                playType = "NORMAL",
                offensivePlaybook = "AIR_RAID",
                defensivePlaybook = "FOUR_THREE",
                ballLocationLower = 20,
                ballLocationUpper = 80,
                distance = 10,
                result = Scenario.GAIN_OF_8_YARDS,
                playTime = 25,
                lowerRange = 5,
                upperRange = 15,
            )

        val ranges2 =
            Ranges(
                playType = "NORMAL",
                offensivePlaybook = "AIR_RAID",
                defensivePlaybook = "FOUR_THREE",
                ballLocationLower = 20,
                ballLocationUpper = 80,
                distance = 10,
                result = Scenario.GAIN_OF_8_YARDS,
                playTime = 25,
                lowerRange = 5,
                upperRange = 15,
            )

        assertEquals(ranges1.hashCode(), ranges2.hashCode())

        ranges2.playType = "KICKOFF"
        assert(ranges1.hashCode() != ranges2.hashCode())
    }
}
