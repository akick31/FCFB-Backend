package com.fcfb.arceus.exception.play

class PlayNotFoundException(message: String) : Exception(message) {
    override fun toString(): String = "PlayNotFoundException: ${super.message}"
}
