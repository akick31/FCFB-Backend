package com.fcfb.arceus.exception.game

class InvalidScenarioException : Exception("Invalid outcome scenario") {
    override fun toString(): String = "InvalidScenarioException: ${super.message}"
}
