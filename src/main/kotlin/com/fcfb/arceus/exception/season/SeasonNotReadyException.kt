package com.fcfb.arceus.exception.season

class SeasonNotReadyException(message: String) : Exception(message) {
    override fun toString(): String = "SeasonNotReadyException: ${super.message}"
}
