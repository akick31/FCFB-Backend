package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.system.MessageType
import com.fcfb.arceus.model.RequestMessageLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestMessageLogRepositoryTest {
    private lateinit var requestMessageLogRepository: RequestMessageLogRepository

    @BeforeEach
    fun setUp() {
        requestMessageLogRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and find by id`() {
        val requestMessageLog = createTestRequestMessageLog(id = 1)

        every { requestMessageLogRepository.save(any()) } returns requestMessageLog
        every { requestMessageLogRepository.findById(1) } returns java.util.Optional.of(requestMessageLog)

        val saved = requestMessageLogRepository.save(requestMessageLog)
        val found = requestMessageLogRepository.findById(saved.id!!).get()

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertEquals(MessageType.GAME_THREAD, found.messageType)
        assertEquals(123, found.gameId)
        assertEquals(456, found.playId)
        assertEquals(789L, found.messageId)
        assertEquals("test-channel", found.messageLocation)
        assertEquals("2024-01-01T12:00:00Z", found.messageTs)
    }

    @Test
    fun `test find by message type`() {
        val gameThreadLog =
            createTestRequestMessageLog(
                id = 1,
                messageType = MessageType.GAME_THREAD,
                messageLocation = "game-channel",
            )
        val privateMessageLog =
            createTestRequestMessageLog(
                id = 2,
                messageType = MessageType.PRIVATE_MESSAGE,
                messageLocation = "private-channel",
            )

        every {
            requestMessageLogRepository.findByMessageType(MessageType.GAME_THREAD)
        } returns listOf(gameThreadLog)
        every {
            requestMessageLogRepository.findByMessageType(MessageType.PRIVATE_MESSAGE)
        } returns listOf(privateMessageLog)

        val gameThreadLogs = requestMessageLogRepository.findByMessageType(MessageType.GAME_THREAD)
        val privateMessageLogs = requestMessageLogRepository.findByMessageType(MessageType.PRIVATE_MESSAGE)

        assertEquals(1, gameThreadLogs.size)
        assertEquals(MessageType.GAME_THREAD, gameThreadLogs[0].messageType)

        assertEquals(1, privateMessageLogs.size)
        assertEquals(MessageType.PRIVATE_MESSAGE, privateMessageLogs[0].messageType)
    }

    @Test
    fun `test find by game id`() {
        val requestMessageLog = createTestRequestMessageLog()

        every { requestMessageLogRepository.findByGameId(123) } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByGameId(123)

        assertEquals(1, found.size)
        assertEquals(123, found[0].gameId)
    }

    @Test
    fun `test find by play id`() {
        val requestMessageLog = createTestRequestMessageLog()

        every { requestMessageLogRepository.findByPlayId(456) } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByPlayId(456)

        assertEquals(1, found.size)
        assertEquals(456, found[0].playId)
    }

    @Test
    fun `test find by message id`() {
        val requestMessageLog = createTestRequestMessageLog()

        every { requestMessageLogRepository.findByMessageId(789L) } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByMessageId(789L)

        assertEquals(1, found.size)
        assertEquals(789L, found[0].messageId)
    }

    @Test
    fun `test find by message location`() {
        val requestMessageLog = createTestRequestMessageLog()

        every { requestMessageLogRepository.findByMessageLocation("test-channel") } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByMessageLocation("test-channel")

        assertEquals(1, found.size)
        assertEquals("test-channel", found[0].messageLocation)
    }

    @Test
    fun `test find by game id and message type`() {
        val requestMessageLog = createTestRequestMessageLog()

        every {
            requestMessageLogRepository.findByGameIdAndMessageType(123, MessageType.GAME_THREAD)
        } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByGameIdAndMessageType(123, MessageType.GAME_THREAD)

        assertEquals(1, found.size)
        assertEquals(123, found[0].gameId)
        assertEquals(MessageType.GAME_THREAD, found[0].messageType)
    }

    @Test
    fun `test find by game id and play id`() {
        val requestMessageLog = createTestRequestMessageLog()

        every { requestMessageLogRepository.findByGameIdAndPlayId(123, 456) } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByGameIdAndPlayId(123, 456)

        assertEquals(1, found.size)
        assertEquals(123, found[0].gameId)
        assertEquals(456, found[0].playId)
    }

    @Test
    fun `test find by game id and message type and play id`() {
        val requestMessageLog = createTestRequestMessageLog()

        every {
            requestMessageLogRepository.findByGameIdAndMessageTypeAndPlayId(123, MessageType.GAME_THREAD, 456)
        } returns listOf(requestMessageLog)

        val found = requestMessageLogRepository.findByGameIdAndMessageTypeAndPlayId(123, MessageType.GAME_THREAD, 456)

        assertEquals(1, found.size)
        assertEquals(123, found[0].gameId)
        assertEquals(MessageType.GAME_THREAD, found[0].messageType)
        assertEquals(456, found[0].playId)
    }

    @Test
    fun `test update message log`() {
        val requestMessageLog = createTestRequestMessageLog()

        every { requestMessageLogRepository.save(any()) } returns requestMessageLog

        val updated = requestMessageLogRepository.save(requestMessageLog)

        assertNotNull(updated)
        assertEquals(MessageType.GAME_THREAD, updated.messageType)
    }

    @Test
    fun `test delete by id`() {
        every { requestMessageLogRepository.deleteById(1) } returns Unit

        requestMessageLogRepository.deleteById(1)

        verify { requestMessageLogRepository.deleteById(1) }
    }

    private fun createTestRequestMessageLog(
        id: Int = 1,
        messageType: MessageType = MessageType.GAME_THREAD,
        gameId: Int = 123,
        playId: Int = 456,
        messageId: Long = 789L,
        messageLocation: String = "test-channel",
        messageTs: String = "2024-01-01T12:00:00Z",
    ): RequestMessageLog {
        return RequestMessageLog(
            messageType = messageType,
            gameId = gameId,
            playId = playId,
            messageId = messageId,
            messageLocation = messageLocation,
            messageTs = messageTs,
        ).apply { this.id = id }
    }
}
