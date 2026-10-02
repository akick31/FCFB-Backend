package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameField
import com.fcfb.arceus.repositories.GameFieldRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.TeamFieldRepository
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Service

@Service
class GameFieldService(
    private val gameFieldRepository: GameFieldRepository,
    private val gameRepository: GameRepository,
    private val teamFieldRepository: TeamFieldRepository,
    private val fieldAppearanceApplier: FieldAppearanceApplier,
) {
    fun getField(gameId: Int): GameField = gameFieldRepository.findById(gameId).orElseGet { seed(gameId) }

    fun updateField(
        gameId: Int,
        request: TeamFieldRequest,
    ): GameField {
        requireAdmin()
        val game = gameRepository.getGameById(gameId) ?: throw InvalidUniformException("No game with id $gameId")
        if (game.gameStatus == GameStatus.FINAL) throw InvalidUniformException("A final game's appearance cannot be changed")
        val field = getField(gameId)
        fieldAppearanceApplier.apply(field, request)
        field.gameId = gameId
        return gameFieldRepository.save(field)
    }

    /** Freezes a game's field so later team edits never rewrite history. Best-effort: never blocks ending a game. */
    fun snapshotIfAbsent(game: Game) {
        val id = game.gameId ?: return
        if (gameFieldRepository.existsById(id)) return
        val teamField = teamFieldRepository.findById(game.homeTeam.orEmpty()).orElse(null) ?: return
        val snapshot = GameField().apply { gameId = id }
        teamField.copyAppearanceInto(snapshot)
        gameFieldRepository.save(snapshot)
    }

    private fun seed(gameId: Int): GameField {
        val game = gameRepository.getGameById(gameId)
        val field = GameField().apply { this.gameId = gameId }
        game?.homeTeam?.let { teamFieldRepository.findById(it).orElse(null)?.copyAppearanceInto(field) }
        return field
    }

    private fun requireAdmin() {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()
    }
}
