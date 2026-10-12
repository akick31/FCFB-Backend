package com.fcfb.arceus.exception.schedule

class InvalidConferenceException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidConferenceException: ${super.message}"
}
