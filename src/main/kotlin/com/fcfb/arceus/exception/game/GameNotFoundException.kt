package com.fcfb.arceus.exception.game

class GameNotFoundException(message: String) : RuntimeException(message) {
    override fun toString(): String = "GameNotFoundException: ${super.message}"
}
