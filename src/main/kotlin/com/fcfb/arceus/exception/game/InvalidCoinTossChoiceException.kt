package com.fcfb.arceus.exception.game

class InvalidCoinTossChoiceException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidCoinTossChoiceException: ${super.message}"
}
