package com.fcfb.arceus.exception.game

class InvalidHalfTimePossessionChangeException : Exception("Invalid half time possession change") {
    override fun toString(): String = "InvalidHalfTimePossessionChangeException: ${super.message}"
}
