package com.fcfb.arceus.exception.game

class InvalidGameReopenException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidGameReopenException: ${super.message}"
}
