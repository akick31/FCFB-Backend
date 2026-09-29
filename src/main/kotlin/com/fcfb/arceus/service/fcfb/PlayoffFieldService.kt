package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.enums.game.PlayoffRound
import com.fcfb.arceus.model.PlayoffField
import com.fcfb.arceus.repositories.PlayoffFieldRepository
import com.fcfb.arceus.util.InvalidUniformException
import org.springframework.stereotype.Service

@Service
class PlayoffFieldService(
    private val playoffFieldRepository: PlayoffFieldRepository,
    private val postseasonFieldUpdater: PostseasonFieldUpdater,
) {
    fun getAll(): List<PlayoffField> = playoffFieldRepository.findAll().toList()

    fun getField(round: String): PlayoffField = playoffFieldRepository.findById(round).orElseGet { newField(round) }

    fun updateField(
        round: String,
        request: PostseasonFieldRequest,
    ): PlayoffField = playoffFieldRepository.save(postseasonFieldUpdater.applyTo(getField(round), request))

    private fun newField(round: String): PlayoffField {
        val known = PlayoffRound.fromLabel(round) ?: throw InvalidUniformException("No playoff round named $round")
        return PlayoffField().apply { this.round = known.label }
    }
}
