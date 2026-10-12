package com.fcfb.arceus.exception.team

class NoCoachDiscordIdsFoundException : Exception("No coach Discord IDs found") {
    override fun toString(): String = "NoCoachDiscordIdFoundException: ${super.message}"
}
