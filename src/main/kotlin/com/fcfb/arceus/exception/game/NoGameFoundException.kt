package com.fcfb.arceus.exception.game

class NoGameFoundException : Exception("No games found to start week") {
    override fun toString(): String = "NoGameFoundException: ${super.message}"
}
