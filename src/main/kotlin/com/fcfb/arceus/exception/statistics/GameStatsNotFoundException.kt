package com.fcfb.arceus.exception.statistics

class GameStatsNotFoundException(message: String) : Exception(message) {
    override fun toString(): String = "GameStatsNotFoundException: ${super.message}"
}
