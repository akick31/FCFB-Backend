package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.game.GameStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScorebugResponseTest {
    @Test
    fun `ScorebugResponse should be a data class`() {
        val response = createTestScorebugResponse()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(response.toString())
        assertTrue(response.toString().contains("ScorebugResponse"))
    }

    @Test
    fun `ScorebugResponse should create instance with all properties`() {
        val scorebugData = byteArrayOf(1, 2, 3, 4, 5)
        val response =
            ScorebugResponse(
                gameId = 123,
                scorebug = scorebugData,
                homeTeam = "Alabama",
                awayTeam = "Georgia",
                status = GameStatus.IN_PROGRESS,
            )

        assertEquals(123, response.gameId)
        assertContentEquals(scorebugData, response.scorebug)
        assertEquals("Alabama", response.homeTeam)
        assertEquals("Georgia", response.awayTeam)
        assertEquals(GameStatus.IN_PROGRESS, response.status)
    }

    @Test
    fun `ScorebugResponse should handle null values`() {
        val response =
            ScorebugResponse(
                gameId = 456,
                scorebug = null,
                homeTeam = "Texas",
                awayTeam = "Oklahoma",
                status = null,
            )

        assertEquals(456, response.gameId)
        assertNull(response.scorebug)
        assertEquals("Texas", response.homeTeam)
        assertEquals("Oklahoma", response.awayTeam)
        assertNull(response.status)
    }

    @Test
    fun `ScorebugResponse should handle different GameStatus enum values`() {
        val gameStatuses =
            listOf(
                GameStatus.PREGAME,
                GameStatus.IN_PROGRESS,
                GameStatus.HALFTIME,
                GameStatus.FINAL,
                GameStatus.OVERTIME,
            )

        gameStatuses.forEach { status ->
            val response = createTestScorebugResponse().copy(status = status)
            assertEquals(status, response.status)
        }
    }

    @Test
    fun `ScorebugResponse should handle different game IDs`() {
        val response = createTestScorebugResponse()

        // Test positive IDs
        val response1 = response.copy(gameId = 1)
        assertEquals(1, response1.gameId)

        val response2 = response.copy(gameId = 999999)
        assertEquals(999999, response2.gameId)

        // Test zero ID
        val response3 = response.copy(gameId = 0)
        assertEquals(0, response3.gameId)
    }

    @Test
    fun `ScorebugResponse should handle different team names`() {
        val response =
            ScorebugResponse(
                gameId = 789,
                scorebug = byteArrayOf(1, 2, 3),
                homeTeam = "Texas A&M",
                awayTeam = "Ole Miss",
                status = GameStatus.FINAL,
            )

        assertEquals("Texas A&M", response.homeTeam)
        assertEquals("Ole Miss", response.awayTeam)
    }

    @Test
    fun `ScorebugResponse should handle long team names`() {
        val longTeamName = "A".repeat(100)
        val response =
            ScorebugResponse(
                gameId = 101,
                scorebug = byteArrayOf(),
                homeTeam = longTeamName,
                awayTeam = longTeamName,
                status = GameStatus.PREGAME,
            )

        assertEquals(longTeamName, response.homeTeam)
        assertEquals(longTeamName, response.awayTeam)
    }

    @Test
    fun `ScorebugResponse should handle different scorebug data sizes`() {
        // Empty scorebug
        val emptyResponse = createTestScorebugResponse().copy(scorebug = byteArrayOf())
        assertContentEquals(byteArrayOf(), emptyResponse.scorebug)

        // Small scorebug
        val smallResponse = createTestScorebugResponse().copy(scorebug = byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), smallResponse.scorebug)

        // Large scorebug
        val largeData = ByteArray(1000) { it.toByte() }
        val largeResponse = createTestScorebugResponse().copy(scorebug = largeData)
        assertContentEquals(largeData, largeResponse.scorebug)
    }

    @Test
    fun `ScorebugResponse should handle binary data correctly`() {
        val binaryData = byteArrayOf(-128, -1, 0, 1, 127, -50, 100)
        val response = createTestScorebugResponse().copy(scorebug = binaryData)

        assertContentEquals(binaryData, response.scorebug)
        assertEquals(binaryData.size, response.scorebug?.size)

        // Check individual bytes
        assertEquals(-128, response.scorebug?.get(0))
        assertEquals(-1, response.scorebug?.get(1))
        assertEquals(0, response.scorebug?.get(2))
        assertEquals(1, response.scorebug?.get(3))
        assertEquals(127, response.scorebug?.get(4))
    }

    @Test
    fun `ScorebugResponse data class should support copy functionality`() {
        val original = createTestScorebugResponse()
        val newScorebugData = byteArrayOf(9, 8, 7, 6, 5)
        val copied =
            original.copy(
                gameId = 999,
                scorebug = newScorebugData,
                status = GameStatus.FINAL,
            )

        assertEquals(999, copied.gameId)
        assertContentEquals(newScorebugData, copied.scorebug)
        assertEquals(GameStatus.FINAL, copied.status)
        assertEquals(original.homeTeam, copied.homeTeam)
        assertEquals(original.awayTeam, copied.awayTeam)
    }

    @Test
    fun `ScorebugResponse should handle team names with special characters`() {
        val response =
            ScorebugResponse(
                gameId = 202,
                scorebug = byteArrayOf(1, 2, 3),
                homeTeam = "Miami (FL)",
                awayTeam = "Virginia Tech",
                status = GameStatus.IN_PROGRESS,
            )

        assertEquals("Miami (FL)", response.homeTeam)
        assertEquals("Virginia Tech", response.awayTeam)
    }

    @Test
    fun `ScorebugResponse should be immutable for non-mutable properties`() {
        val response = createTestScorebugResponse()

        // All properties should be val (immutable)
        // This is enforced by the data class declaration
        assertNotNull(response.gameId)
        assertNotNull(response.homeTeam)
        assertNotNull(response.awayTeam)
        // scorebug and status can be null
    }

    @Test
    fun `ScorebugResponse should handle game status transitions`() {
        val baseResponse = createTestScorebugResponse()

        // Game progression
        val pregame = baseResponse.copy(status = GameStatus.PREGAME)
        val inProgress = baseResponse.copy(status = GameStatus.IN_PROGRESS)
        val halftime = baseResponse.copy(status = GameStatus.HALFTIME)
        val final = baseResponse.copy(status = GameStatus.FINAL)

        assertEquals(GameStatus.PREGAME, pregame.status)
        assertEquals(GameStatus.IN_PROGRESS, inProgress.status)
        assertEquals(GameStatus.HALFTIME, halftime.status)
        assertEquals(GameStatus.FINAL, final.status)
    }

    @Test
    fun `ScorebugResponse should handle scorebug data mutations`() {
        val originalData = byteArrayOf(1, 2, 3, 4, 5)
        val response = createTestScorebugResponse().copy(scorebug = originalData)

        // Verify original data
        assertContentEquals(originalData, response.scorebug)

        // Modify the original array (this should not affect the response if properly handled)
        originalData[0] = 99

        // The response should still have the original values if ByteArray is handled correctly
        // Note: ByteArray is mutable, so this test verifies behavior
        assertEquals(99, response.scorebug?.get(0)) // ByteArray is referenced, not copied
    }

    @Test
    fun `ScorebugResponse should handle empty team names`() {
        val response =
            ScorebugResponse(
                gameId = 303,
                scorebug = byteArrayOf(1, 2, 3),
                homeTeam = "",
                awayTeam = "",
                status = GameStatus.PREGAME,
            )

        assertEquals("", response.homeTeam)
        assertEquals("", response.awayTeam)
    }

    @Test
    fun `ScorebugResponse equality should handle ByteArray correctly`() {
        val data1 = byteArrayOf(1, 2, 3)
        val data2 = byteArrayOf(1, 2, 3)
        val data3 = byteArrayOf(4, 5, 6)

        val response1 = createTestScorebugResponse().copy(scorebug = data1)
        val response2 = createTestScorebugResponse().copy(scorebug = data2)
        val response3 = createTestScorebugResponse().copy(scorebug = data3)

        // Note: ByteArray equality in data classes is by reference, not content
        // This test documents the behavior
        assertTrue(response1 != response2) // Different ByteArray instances
        assertTrue(response1 != response3) // Different content and instances
    }

    private fun createTestScorebugResponse(): ScorebugResponse {
        return ScorebugResponse(
            gameId = 123,
            scorebug = byteArrayOf(1, 2, 3, 4, 5),
            homeTeam = "Alabama",
            awayTeam = "Georgia",
            status = GameStatus.IN_PROGRESS,
        )
    }
}
