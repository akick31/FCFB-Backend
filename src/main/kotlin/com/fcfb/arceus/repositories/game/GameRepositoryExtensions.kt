package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.model.game.Game

fun GameRepository.requireOngoingGames(): List<Game> =
    getAllOngoingGames().ifEmpty {
        throw GameNotFoundException("No ongoing games found")
    }
