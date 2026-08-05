package com.fcfb.arceus.model

import com.fcfb.arceus.enums.system.MessageType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestMessageLogTest {
    @Test
    fun `RequestMessageLog should be properly annotated`() {
        val entityAnnotation = RequestMessageLog::class.annotations.find { it is javax.persistence.Entity }
        val tableAnnotation = RequestMessageLog::class.annotations.find { it is javax.persistence.Table }
        val typeDefAnnotation = RequestMessageLog::class.annotations.find { it is org.hibernate.annotations.TypeDef }

        assertNotNull(entityAnnotation, "RequestMessageLog should be annotated with @Entity")
        assertNotNull(tableAnnotation, "RequestMessageLog should be annotated with @Table")
        assertNotNull(typeDefAnnotation, "RequestMessageLog should be annotated with @TypeDef")
    }

    @Test
    fun `RequestMessageLog default constructor should initialize with null values`() {
        val requestMessageLog = RequestMessageLog()

        assertNull(requestMessageLog.id)
        assertNull(requestMessageLog.messageType)
        assertNull(requestMessageLog.gameId)
        assertNull(requestMessageLog.playId)
        assertNull(requestMessageLog.messageId)
        assertNull(requestMessageLog.messageLocation)
        assertNull(requestMessageLog.messageTs)
    }

    @Test
    fun `RequestMessageLog parameterized constructor should set values correctly`() {
        val messageType = MessageType.GAME_THREAD
        val gameId = 123
        val playId = 456
        val messageId = 789L
        val messageLocation = "test_location"
        val messageTs = "2023-01-01T12:00:00Z"

        val requestMessageLog =
            RequestMessageLog(
                messageType = messageType,
                gameId = gameId,
                playId = playId,
                messageId = messageId,
                messageLocation = messageLocation,
                messageTs = messageTs,
            )

        assertNull(requestMessageLog.id)
        assertEquals(messageType, requestMessageLog.messageType)
        assertEquals(gameId, requestMessageLog.gameId)
        assertEquals(playId, requestMessageLog.playId)
        assertEquals(messageId, requestMessageLog.messageId)
        assertEquals(messageLocation, requestMessageLog.messageLocation)
        assertEquals(messageTs, requestMessageLog.messageTs)
    }

    @Test
    fun `RequestMessageLog parameterized constructor should handle null playId and messageTs`() {
        val messageType = MessageType.PRIVATE_MESSAGE
        val gameId = 123
        val messageId = 789L
        val messageLocation = "test_location"

        val requestMessageLog =
            RequestMessageLog(
                messageType = messageType,
                gameId = gameId,
                playId = null,
                messageId = messageId,
                messageLocation = messageLocation,
                messageTs = null,
            )

        assertEquals(messageType, requestMessageLog.messageType)
        assertEquals(gameId, requestMessageLog.gameId)
        assertNull(requestMessageLog.playId)
        assertEquals(messageId, requestMessageLog.messageId)
        assertEquals(messageLocation, requestMessageLog.messageLocation)
        assertNull(requestMessageLog.messageTs)
    }

    @Test
    fun `RequestMessageLog properties should be mutable`() {
        val requestMessageLog = RequestMessageLog()

        requestMessageLog.id = 100
        requestMessageLog.messageType = MessageType.GAME_THREAD
        requestMessageLog.gameId = 200
        requestMessageLog.playId = 300
        requestMessageLog.messageId = 400L
        requestMessageLog.messageLocation = "updated_location"
        requestMessageLog.messageTs = "2023-12-31T23:59:59Z"

        assertEquals(100, requestMessageLog.id)
        assertEquals(MessageType.GAME_THREAD, requestMessageLog.messageType)
        assertEquals(200, requestMessageLog.gameId)
        assertEquals(300, requestMessageLog.playId)
        assertEquals(400L, requestMessageLog.messageId)
        assertEquals("updated_location", requestMessageLog.messageLocation)
        assertEquals("2023-12-31T23:59:59Z", requestMessageLog.messageTs)
    }

    @Test
    fun `RequestMessageLog should handle both MessageType enum values`() {
        val gameThreadLog =
            RequestMessageLog(
                messageType = MessageType.GAME_THREAD,
                gameId = 1,
                playId = null,
                messageId = 1L,
                messageLocation = "location1",
                messageTs = null,
            )

        val privateMessageLog =
            RequestMessageLog(
                messageType = MessageType.PRIVATE_MESSAGE,
                gameId = 2,
                playId = null,
                messageId = 2L,
                messageLocation = "location2",
                messageTs = null,
            )

        assertEquals(MessageType.GAME_THREAD, gameThreadLog.messageType)
        assertEquals(MessageType.PRIVATE_MESSAGE, privateMessageLog.messageType)
    }

    @Test
    fun `MessageType enum should have correct descriptions`() {
        assertEquals("Game Thread", MessageType.GAME_THREAD.description)
        assertEquals("Private Message", MessageType.PRIVATE_MESSAGE.description)
    }

    @Test
    fun `RequestMessageLog should be instantiable with both constructors`() {
        val requestMessageLog1 = RequestMessageLog()
        val requestMessageLog2 =
            RequestMessageLog(
                MessageType.GAME_THREAD,
                1,
                null,
                1L,
                "location",
                null,
            )

        assertNotNull(requestMessageLog1)
        assertNotNull(requestMessageLog2)
    }

    @Test
    fun `RequestMessageLog should handle large values`() {
        val requestMessageLog =
            RequestMessageLog(
                messageType = MessageType.PRIVATE_MESSAGE,
                gameId = Int.MAX_VALUE,
                playId = Int.MAX_VALUE,
                messageId = Long.MAX_VALUE,
                messageLocation = "A".repeat(1000),
                messageTs = "2099-12-31T23:59:59.999999999Z",
            )

        assertEquals(MessageType.PRIVATE_MESSAGE, requestMessageLog.messageType)
        assertEquals(Int.MAX_VALUE, requestMessageLog.gameId)
        assertEquals(Int.MAX_VALUE, requestMessageLog.playId)
        assertEquals(Long.MAX_VALUE, requestMessageLog.messageId)
        assertEquals("A".repeat(1000), requestMessageLog.messageLocation)
        assertEquals("2099-12-31T23:59:59.999999999Z", requestMessageLog.messageTs)
    }

    @Test
    fun `RequestMessageLog should handle special characters in string fields`() {
        val messageLocation = "location/with/special!@#$%^&*()characters"
        val messageTs = "2023-01-01T12:00:00+05:30"

        val requestMessageLog =
            RequestMessageLog(
                messageType = MessageType.GAME_THREAD,
                gameId = 1,
                playId = 1,
                messageId = 1L,
                messageLocation = messageLocation,
                messageTs = messageTs,
            )

        assertEquals(messageLocation, requestMessageLog.messageLocation)
        assertEquals(messageTs, requestMessageLog.messageTs)
    }

    @Test
    fun `RequestMessageLog should allow partial updates`() {
        val requestMessageLog = RequestMessageLog()

        requestMessageLog.messageType = MessageType.GAME_THREAD
        assertEquals(MessageType.GAME_THREAD, requestMessageLog.messageType)
        assertNull(requestMessageLog.gameId)

        requestMessageLog.gameId = 123
        assertEquals(MessageType.GAME_THREAD, requestMessageLog.messageType)
        assertEquals(123, requestMessageLog.gameId)
        assertNull(requestMessageLog.messageId)

        requestMessageLog.messageId = 456L
        assertEquals(MessageType.GAME_THREAD, requestMessageLog.messageType)
        assertEquals(123, requestMessageLog.gameId)
        assertEquals(456L, requestMessageLog.messageId)
    }
}
