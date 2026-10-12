package com.fcfb.arceus.exception.team

class TeamNotFoundException(message: String) : Exception(message) {
    override fun toString(): String = "TeamNotFoundException: ${super.message}"
}
