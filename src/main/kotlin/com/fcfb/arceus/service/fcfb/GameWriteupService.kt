package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.repositories.GameWriteupRepository
import org.springframework.stereotype.Service

@Service
class GameWriteupService(
    private val gameWriteupRepository: GameWriteupRepository,
) {
    fun getGameMessageByScenario(
        scenario: String,
        playCall: String?,
    ): String {
        val writeups = gameWriteupRepository.findByScenario(scenario, playCall)

        return if (writeups.isNotEmpty()) {
            writeups.random().message ?: "No message found"
        } else {
            "No message found"
        }
    }
}
