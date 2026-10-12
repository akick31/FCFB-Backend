package com.fcfb.arceus.exception.discord

class UnableToCreateGameThreadException : Exception("Unable to create game thread in Discord") {
    override fun toString(): String = "UnableToCreateGameThreadException: ${super.message}"
}
