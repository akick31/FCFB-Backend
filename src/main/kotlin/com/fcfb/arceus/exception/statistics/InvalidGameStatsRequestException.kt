package com.fcfb.arceus.exception.statistics

class InvalidGameStatsRequestException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidGameStatsRequestException: ${super.message}"
}
