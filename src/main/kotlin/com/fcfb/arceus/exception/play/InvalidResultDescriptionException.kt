package com.fcfb.arceus.exception.play

class InvalidResultDescriptionException : Exception("Invalid result description, could not parse result yardage") {
    override fun toString(): String = "InvalidInvalidResultDescriptionException: ${super.message}"
}
